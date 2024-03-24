package dev.shchuko.vet_assistant.impl.db

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption

object MedicineTable : UUIDTable() {
    val name = varchar("name", 255)
    val description = varchar("description", 3000)

    init {
        index(isUnique = true, name)
    }
}

object ActiveIngredientTable : UUIDTable() {
    val medicineId = reference("medicine_id", MedicineTable, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 255)

    init {
        index(isUnique = true, name, medicineId)
    }
}

object MedicineAnalogueTable : UUIDTable() {
    val medicineId = reference("medicine_id", MedicineTable, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 255)

    init {
        index(isUnique = true, name, medicineId)
    }
}

object TelegramUserTable : UUIDTable() {
    val telegramUsername = varchar("telegram_username", 255).uniqueIndex()
    val isAdmin = bool("is_admin")
}
