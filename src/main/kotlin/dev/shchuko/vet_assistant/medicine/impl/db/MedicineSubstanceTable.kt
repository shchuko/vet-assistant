package dev.shchuko.vet_assistant.medicine.impl.db

import org.jetbrains.exposed.dao.id.UUIDTable

object MedicineSubstanceTable : UUIDTable() {
    const val NAME_MAX_LEN = 64

    val name = varchar("name", NAME_MAX_LEN).uniqueIndex()
}