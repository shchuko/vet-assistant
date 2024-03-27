package dev.shchuko.vet_assistant.impl

import dev.shchuko.vet_assistant.api.VetMedicineService
import dev.shchuko.vet_assistant.impl.db.*
import dev.shchuko.vet_assistant.medicine.impl.service.util.MisspellCorrector
import dev.shchuko.vet_assistant.service.model.MedicineSearchResult
import dev.shchuko.vet_assistant.service.model.MedicineWithDescription
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.*

class VetMedicineServiceImpl : VetMedicineService {
    private val logger = LoggerFactory.getLogger(VetMedicineServiceImpl::class.java)

    private data class IndexEntry(
        val originalSearchableParameterName: String,
        val medicineName: String
    )

    private var index: Map<String, IndexEntry> = mapOf()

    private val misspellCorrector = MisspellCorrector(
        cacheLimit = 5000,
        sameWordMaxEditDistance = 2,
        resultLimit = 5,
    )

    override fun init() {
        rebuildIndex()
    }

    override fun search(name: String): MedicineSearchResult {
        val trimmed = name.trim()
        var dbSearchQuery = index[trimmed.lowercase()]

        var misspelled = emptyList<String>()
        if (dbSearchQuery == null) {
            val misspellCorrectionResult = misspellCorrector.findWord(trimmed)
            when (misspellCorrectionResult.resultType) {
                MisspellCorrector.SearchResult.Type.NOTHING_FOUND -> {
                    return MedicineSearchResult(misspellMatches = emptyList(), medicine = null)
                }

                MisspellCorrector.SearchResult.Type.EXACT_MATCH -> {
                    dbSearchQuery = index[trimmed.lowercase()]
                }

                MisspellCorrector.SearchResult.Type.MISSPELL_CORRECTION_SINGLE_MATCH -> {
                    dbSearchQuery = index[misspellCorrectionResult.words.single()]
                    misspelled = listOfNotNull(dbSearchQuery?.originalSearchableParameterName)
                }

                MisspellCorrector.SearchResult.Type.MISSPELL_CORRECTION_MULTIPLE_MATCH -> {
                    misspelled = misspellCorrectionResult.words.mapNotNull {
                        index[it]?.originalSearchableParameterName?.trim()
                    }
                }
            }
        }

        if (dbSearchQuery == null) {
            return MedicineSearchResult(
                misspellMatches = misspelled,
                medicine = null,
            )
        }

        return transaction {
            val dbLookupResult = MedicineEntity.find { MedicineTable.name eq dbSearchQuery.medicineName }.toList()

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

            if (medicine == null) {
                logger.warn("${dbSearchQuery.medicineName} found in index but not found in database")
            }
            MedicineSearchResult(
                misspellMatches = if (medicine == null) emptyList() else misspelled,
                medicine = medicine,
            )
        }
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
            val indexByMedicineName =
                medicine.name.trim().lowercase() to IndexEntry(medicine.name, medicine.name)

            val indexByIngredientName = medicine.activeIngredients.map { ingredient ->
                ingredient.trim().lowercase() to IndexEntry(ingredient, medicine.name)
            }

            val indexByAnalogueName = medicine.analogues.map { analogue ->
                analogue.trim().lowercase() to IndexEntry(analogue, medicine.name)
            }

            listOf(indexByMedicineName) + indexByIngredientName + indexByAnalogueName
        }.toMap()

        misspellCorrector.reloadDict(index.asSequence().map { (searchableParameter, _) ->
            searchableParameter
        })
    }
}