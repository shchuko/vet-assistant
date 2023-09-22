package dev.shchuko.vet_assistant.medicine.impl.db

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption

object MedicineAnalogueTable : UUIDTable() {
    val name = varchar("name", MedicineTable.NAME_MAX_LEN).uniqueIndex()
    val medicineId = reference("medicineId", MedicineTable, onDelete = ReferenceOption.CASCADE)

    init {
        uniqueIndex(name, medicineId)
    }
}
