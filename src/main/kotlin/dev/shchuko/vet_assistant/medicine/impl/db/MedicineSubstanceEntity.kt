package dev.shchuko.vet_assistant.medicine.impl.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

class MedicineSubstanceEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<MedicineSubstanceEntity>(MedicineSubstanceTable)

    var name by MedicineSubstanceTable.name
    var containingMedicines by MedicineEntity via MedicineToMedicineSubstanceTable
}

