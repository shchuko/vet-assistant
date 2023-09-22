package dev.shchuko.vet_assistant.medicine.impl.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

class MedicineAnalogueEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<MedicineAnalogueEntity>(MedicineAnalogueTable)

    var name by MedicineAnalogueTable.name
    var medicine by MedicineEntity referencedOn MedicineAnalogueTable.medicineId
}