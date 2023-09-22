package dev.shchuko.vet_assistant.medicine.impl.service

import dev.shchuko.vet_assistant.medicine.api.*
import dev.shchuko.vet_assistant.medicine.api.MedicineValidationException.MedicineNameCollisionException
import dev.shchuko.vet_assistant.medicine.impl.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import medicineStub
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import java.sql.Connection
import java.util.*

class MedicineServiceImpl(
    private val database: Database,
) : MedicineService {
    init {
        // TODO remove this stub
        transaction {
            SchemaUtils.create(
                MedicineTable,
                MedicineAnalogueTable,
                MedicineSubstanceTable,
                MedicineToMedicineSubstanceTable
            )
        }
        runBlocking {
            medicineStub.forEach { (t, u) -> createMedicine(t, u) }
        }
    }

    override suspend fun createMedicine(name: String, description: String): UUID {
        val preprocessedName = preprocessMedicineName(name)
        validateMedicineName(preprocessedName)
        validateMedicineDescription(description)

        return writeTransaction {
            if (
                MedicineTable.select { MedicineTable.name eq preprocessedName }.any() ||
                MedicineAnalogueTable.select { MedicineAnalogueTable.name eq preprocessedName }.any()
            ) {
                throw MedicineNameCollisionException(preprocessedName)
            }


            MedicineEntity.new {
                this.name = name
                this.description = description
            }.id.value
        }
    }

    override suspend fun editMedicineName(medicineId: UUID, name: String): String =
        checkNotNull(editMedicineInternal(medicineId, name = name, description = null).oldName)

    override suspend fun editMedicineDescription(medicineId: UUID, description: String): String =
        checkNotNull(editMedicineInternal(medicineId, name = null, description = description).oldDescription)

    override suspend fun findMedicine(name: String): Optional<MedicineDto> = readTransaction {
        val preprocessedName = preprocessMedicineName(name)
        val medicine = MedicineEntity.find {
            MedicineTable.name eq preprocessedName
        }.firstOrNull() ?: return@readTransaction Optional.empty()
        Optional.of(
            MedicineDto(
                id = medicine.id.value,
                name = medicine.name,
                description = medicine.description,
                substances = medicine.medicineSubstances.map {
                    MedicineSubstanceDto(it.id.value, it.name)
                },
                analogueNames = medicine.analogues.map(MedicineAnalogueEntity::name)
            ),

            )
    }

    override suspend fun searchMedicine(medicineOrSubstanceName: String): MedicineSearchResult {
        // TODO implement misspell correction
        val optional = findMedicine(medicineOrSubstanceName)
        if (optional.isEmpty) {
            return MedicineSearchResult.NoneMatch
        }
        return MedicineSearchResult.SingleMatchByName(optional.get(), misspell = false)
    }

    override suspend fun deleteMedicine(medicineId: UUID, safeDelete: Boolean): MedicineDeleteResult =
        writeTransaction {
            val medicine = MedicineEntity.findById(medicineId)
                ?: return@writeTransaction MedicineDeleteResult.NotFound(medicineId)
            val analogueNames = medicine.analogues.map(MedicineAnalogueEntity::name)

            if (safeDelete) {
                if (analogueNames.isNotEmpty()) {
                    return@writeTransaction MedicineDeleteResult.SafeDeleteFailed(analogueNames)
                }
            }

            val medicineDto = MedicineDto(
                id = medicine.id.value,
                name = medicine.name,
                description = medicine.description,
                substances = medicine.medicineSubstances.map {
                    MedicineSubstanceDto(it.id.value, it.name)
                },
                analogueNames = analogueNames
            )
            medicine.delete()
            MedicineDeleteResult.Deleted(medicineDto)
        }

    override suspend fun addMedicineAnalogues(medicineId: UUID, analogueNames: List<String>): List<String> {
        val names = analogueNames.asSequence()
            .map { name ->
                preprocessMedicineName(name)
                validateMedicineName(name)
            }
            .distinct()
            .toMutableList()

        writeTransaction {
            val medicine = MedicineEntity.findById(medicineId) ?: throw MedicineIdNotFoundException(medicineId)

            val primaryNameCollision = MedicineEntity.find { MedicineTable.name inList names }
                .asSequence().map(MedicineEntity::name)
            val analogueNameCollision = MedicineAnalogueEntity.find { MedicineAnalogueTable.name inList names }
                .asSequence().map(MedicineAnalogueEntity::name)
            val toSet = (primaryNameCollision + analogueNameCollision).toSet()
            names -= toSet

            MedicineAnalogueTable.batchInsert(names, shouldReturnGeneratedValues = false) { analogueName ->
                this[MedicineAnalogueTable.medicineId] = medicine.id
                this[MedicineAnalogueTable.name] = analogueName
            }
        }
        return names
    }

    override suspend fun deleteMedicineAnalogues(analogueNames: List<String>): Int {
        val names = analogueNames.asSequence()
            .map { name ->
                preprocessMedicineName(name)
                validateMedicineName(name)
            }
            .distinct()
            .toList()

        return writeTransaction {
            MedicineAnalogueTable.deleteWhere { MedicineAnalogueTable.name inList names }
        }
    }

    override suspend fun makeMedicineAnalogueNamePrimary(medicineId: UUID, analogueName: String) = writeTransaction {
        val medicine = MedicineEntity.findById(medicineId) ?: throw MedicineIdNotFoundException(medicineId)

        val analogueNamePreprocessed = preprocessMedicineName(analogueName)
        val analogueList = MedicineAnalogueEntity.find {
            (MedicineAnalogueTable.medicineId eq medicine.id) and (MedicineAnalogueTable.name eq medicine.name)
        }.toList()
        check(analogueList.size <= 1) { "Unexpected list of analogues for medicineId=$medicineId,analogueName=$analogueName" }

        val analogue = analogueList.singleOrNull() ?: throw MedicineAnalogueNotFoundException(
            medicine.name,
            analogueNamePreprocessed
        )

        val previousPrimaryName = medicine.name
        medicine.name = analogue.name
        analogue.name = medicine.name
        previousPrimaryName
    }

    override suspend fun createMedicineSubstance(name: String): UUID {
        val preprocessedName = preprocessMedicineSubstanceName(name)
        validateMedicineSubstanceName(preprocessedName)
        return writeTransaction {
            if (!MedicineSubstanceEntity.find { MedicineSubstanceTable.name eq preprocessedName }.empty()) {
                throw MedicineSubstanceValidationException.MedicineSubstanceNameCollisionException(preprocessedName)
            }

            MedicineSubstanceEntity.new {
                this.name = name
            }.id.value
        }
    }

    override suspend fun editMedicineSubstanceName(medicineSubstanceId: UUID, name: String): String {
        val preprocessedName = preprocessMedicineSubstanceName(name)
        validateMedicineSubstanceName(preprocessedName)

        return writeTransaction {
            val isCollision = MedicineSubstanceTable.select {
                (MedicineSubstanceTable.name eq preprocessedName) and (MedicineSubstanceTable.id neq medicineSubstanceId)
            }.any()
            if (isCollision) {
                throw MedicineSubstanceValidationException.MedicineSubstanceNameCollisionException(preprocessedName)
            }

            val substance = MedicineSubstanceEntity.findById(medicineSubstanceId)
                ?: throw MedicineSubstanceIdNotFoundException(medicineSubstanceId)
            val oldName = substance.name
            substance.name = preprocessedName
            oldName
        }
    }

    override suspend fun deleteMedicineSubstance(
        medicineSubstanceId: UUID,
        safeDelete: Boolean
    ): MedicineSubstanceDeleteResult = writeTransaction {
        val medicineSubstance = MedicineSubstanceEntity.findById(medicineSubstanceId)
            ?: return@writeTransaction MedicineSubstanceDeleteResult.NotFound(medicineSubstanceId)

        if (safeDelete) {
            val linkedMedicines = medicineSubstance.containingMedicines.map(MedicineEntity::name)
            if (linkedMedicines.isNotEmpty()) {
                return@writeTransaction MedicineSubstanceDeleteResult.SafeDeleteFailed(linkedMedicines)
            }
        }

        val dto = MedicineSubstanceDto(medicineSubstance.id.value, medicineSubstance.name)
        medicineSubstance.delete()
        return@writeTransaction MedicineSubstanceDeleteResult.Deleted(dto)
    }

    override suspend fun linkSubstanceToMedicine(medicineId: UUID, medicineSubstanceId: UUID): Boolean =
        writeTransaction {
            val medicine = MedicineEntity.findById(medicineId)
                ?: throw MedicineIdNotFoundException(medicineId)
            val medicineSubstance = MedicineSubstanceEntity.findById(medicineSubstanceId)
                ?: throw MedicineSubstanceIdNotFoundException(medicineSubstanceId)

            val current = medicine.medicineSubstances
            if (current.contains(medicineSubstance)) {
                false
            } else {
                medicine.medicineSubstances = SizedCollection(current + medicineSubstance)
                true
            }
        }

    override suspend fun unlinkSubstanceFromMedicine(medicineId: UUID, medicineSubstanceId: UUID): Boolean =
        writeTransaction {
            val medicine = MedicineEntity.findById(medicineId)
                ?: throw MedicineIdNotFoundException(medicineId)
            val medicineSubstance = MedicineSubstanceEntity.findById(medicineSubstanceId)
                ?: throw MedicineSubstanceIdNotFoundException(medicineSubstanceId)

            val current = medicine.medicineSubstances
            if (!current.contains(medicineSubstance)) {
                false
            } else {
                medicine.medicineSubstances = SizedCollection(current - medicineSubstance)
                true
            }
        }

    data class EditMedicineInternalResult(val oldName: String?, val oldDescription: String?)

    private suspend fun editMedicineInternal(
        medicineId: UUID,
        name: String?,
        description: String?
    ): EditMedicineInternalResult {
        require(name != null || description != null)
        val preprocessedName = name?.let(::preprocessMedicineName)?.also(::validateMedicineName)
        description?.also(::validateMedicineDescription)

        return writeTransaction {
            if (preprocessedName != null) {
                val isCollision = MedicineTable.select {
                    (MedicineTable.name eq preprocessedName) and (MedicineTable.id neq medicineId)
                }.any()
                if (isCollision) {
                    throw MedicineNameCollisionException(preprocessedName)
                }
            }

            val medicine = MedicineEntity.findById(medicineId) ?: throw MedicineIdNotFoundException(medicineId)
            val oldName = preprocessedName?.let {
                val extractedOldName = medicine.name
                medicine.name = it
                extractedOldName
            }
            val oldDescription = description?.let {
                val extractedOldDescription = medicine.description
                medicine.description = it
                extractedOldDescription
            }
            EditMedicineInternalResult(oldName, oldDescription)
        }
    }

    /**
     * Avoids dirty reads of uncommitted changes only
     */
    private suspend fun <T> readTransaction(statement: Transaction.() -> T) = withContext(Dispatchers.IO) {
        transaction(
            transactionIsolation = Connection.TRANSACTION_READ_COMMITTED,
            repetitionAttempts = database.transactionManager.defaultRepetitionAttempts,
            db = database,
            statement = statement
        )
    }

    /**
     * All [writeTransaction]s are serializable
     */
    private suspend fun <T> writeTransaction(statement: Transaction.() -> T) = withContext(Dispatchers.IO) {
        transaction(
            transactionIsolation = Connection.TRANSACTION_SERIALIZABLE,
            repetitionAttempts = database.transactionManager.defaultRepetitionAttempts,
            db = database,
            statement = statement
        )
    }

    private fun preprocessMedicineName(name: String): String = name.trim()

    private fun validateMedicineName(name: String): String {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            throw MedicineValidationException.MedicineNameIsBlankException()
        }
        if (trimmedName.length > MedicineTable.NAME_MAX_LEN) {
            throw MedicineValidationException.MedicineNameTooLongException(
                trimmedName.length,
                MedicineTable.NAME_MAX_LEN
            )
        }
        return trimmedName
    }

    private fun validateMedicineDescription(description: String): String {
        if (description.length > MedicineTable.DESCRIPTION_MAX_LEN) {
            throw MedicineValidationException.MedicineDescriptionTooLongException(
                providedLen = description.length,
                maxLen = MedicineTable.DESCRIPTION_MAX_LEN
            )
        }
        return description
    }

    private fun preprocessMedicineSubstanceName(name: String): String = name.trim()

    private fun validateMedicineSubstanceName(name: String): String {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            throw MedicineSubstanceValidationException.MedicineSubstanceNameIsBlankException()
        }
        if (trimmedName.length > MedicineSubstanceTable.NAME_MAX_LEN) {
            throw MedicineSubstanceValidationException.MedicineSubstanceNameTooLongException(
                providedLen = trimmedName.length,
                maxLen = MedicineTable.NAME_MAX_LEN
            )
        }
        return trimmedName
    }
}