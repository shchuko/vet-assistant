package dev.shchuko.vet_assistant.medicine.impl.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

class MedicineEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<MedicineEntity>(MedicineTable)

    var name by MedicineTable.name
    var description by MedicineTable.description

    var medicineSubstances by MedicineSubstanceEntity via MedicineToMedicineSubstanceTable

    val analogues by MedicineAnalogueEntity referrersOn MedicineAnalogueTable.medicineId
}