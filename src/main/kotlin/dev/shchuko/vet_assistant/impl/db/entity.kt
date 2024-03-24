package dev.shchuko.vet_assistant.impl.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

class MedicineEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<MedicineEntity>(MedicineTable)

    val name by MedicineTable.name
    val description by MedicineTable.description

    val ingredients by ActiveIngredientEntity referrersOn ActiveIngredientTable.medicineId
    val analogues by MedicineAnalogueEntity referrersOn MedicineAnalogueTable.medicineId
}

class ActiveIngredientEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ActiveIngredientEntity>(ActiveIngredientTable)

    val name by ActiveIngredientTable.name
}

class MedicineAnalogueEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<MedicineAnalogueEntity>(MedicineAnalogueTable)

    val name by MedicineAnalogueTable.name
}

class TelegramUserEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<TelegramUserEntity>(TelegramUserTable)

    val telegramUsername by TelegramUserTable.telegramUsername
    val isAdmin by TelegramUserTable.isAdmin
}
