package dev.shchuko.vet_assistant.impl

import dev.shchuko.vet_assistant.api.VetMedicineService
import dev.shchuko.vet_assistant.impl.db.*
import dev.shchuko.vet_assistant.service.model.MedicineSearchResult
import dev.shchuko.vet_assistant.service.model.MedicineWithDescription
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class VetMedicineServiceImpl : VetMedicineService {
    private var index: Map<String, String> = mapOf()

    override fun init() {
        rebuildIndex()
    }

    override fun search(name: String): MedicineSearchResult = transaction {
        val indexLookupResult = index[name.trim().lowercase()] ?: return@transaction MedicineSearchResult(
            misspellMatches = emptyList(),
            medicine = null,
        )

        val dbLookupResult = MedicineEntity.find { MedicineTable.name eq indexLookupResult }.toList()

        val medicine = dbLookupResult.singleOrNull()?.let { medicineEntity ->
            MedicineWithDescription(
                name = medicineEntity.name,
                description = medicineEntity.description,
                analogues = MedicineAnalogueEntity
                    .find { MedicineAnalogueTable.medicineId eq medicineEntity.id }
                    .map { it.name },
                activeIngredients = ActiveIngredientEntity
                    .find { ActiveIngredientTable.medicineId eq medicineEntity.id }
                    .map { it.name },
            )
        }

        MedicineSearchResult(
            misspellMatches = if (medicine == null) dbLookupResult.map { it.name } else emptyList(),
            medicine = medicine,
        )
    }

    override fun getAll(): List<MedicineWithDescription> = transaction {
        MedicineEntity.all().map { medicineEntity ->
            MedicineWithDescription(
                medicineEntity.name,
                medicineEntity.description,
                medicineEntity.analogues.map { analogueEntity -> analogueEntity.name },
                medicineEntity.ingredients.map { ingredientEntity -> ingredientEntity.name }
            )
        }
    }

    @Synchronized
    override fun setAll(entries: List<MedicineWithDescription>) {
        transaction {
            MedicineTable.deleteAll()
            entries.forEach { entry ->
                val nextMedicineId = UUID.randomUUID()

                MedicineTable.insert {
                    it[id] = nextMedicineId
                    it[name] = entry.name
                    it[description] = entry.description
                }

                entry.activeIngredients.forEach { activeIngredientName ->
                    ActiveIngredientTable.insert {
                        it[id] = UUID.randomUUID()
                        it[medicineId] = nextMedicineId
                        it[name] = activeIngredientName
                    }
                }

                entry.analogues.forEach { analogueName ->
                    MedicineAnalogueTable.insert {
                        it[id] = UUID.randomUUID()
                        it[medicineId] = nextMedicineId
                        it[name] = analogueName
                    }
                }
            }
        }
        rebuildIndex()
    }

    private fun rebuildIndex() {
        index = getAll().flatMap { medicine ->
            listOf(medicine.name.trim().lowercase() to medicine.name) +
                    medicine.activeIngredients.map { ingredient -> ingredient.trim().lowercase() to medicine.name } +
                    medicine.analogues.map { analogue -> analogue.trim().lowercase() to medicine.name }
        }.toMap()
    }
}