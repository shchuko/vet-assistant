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
    override fun search(name: String): MedicineSearchResult = transaction {
        val result = MedicineEntity.find { MedicineTable.name eq name }.toList()

        val medicine = result.singleOrNull()?.let { medicineEntity ->
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
            misspellMatches = if (medicine == null) result.map { it.name } else emptyList(),
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

    override fun setAll(entries: List<MedicineWithDescription>): Unit = transaction {
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
}