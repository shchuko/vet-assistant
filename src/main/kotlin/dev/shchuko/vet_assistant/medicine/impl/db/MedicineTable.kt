package dev.shchuko.vet_assistant.medicine.impl.db

import org.jetbrains.exposed.dao.id.UUIDTable

object MedicineTable : UUIDTable() {
    const val NAME_MAX_LEN = 64
    const val DESCRIPTION_MAX_LEN = 3078

    val name = varchar("name", NAME_MAX_LEN).uniqueIndex()
    val description = text("description", eagerLoading = false)
}