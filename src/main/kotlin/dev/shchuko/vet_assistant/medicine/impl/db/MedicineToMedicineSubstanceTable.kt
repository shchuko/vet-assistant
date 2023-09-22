package dev.shchuko.vet_assistant.medicine.impl.db

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object MedicineToMedicineSubstanceTable : Table() {
    val medicine = reference("medicineId", MedicineTable, onDelete = ReferenceOption.CASCADE)
    val medicineSubstance = reference("medicineSubstanceId", MedicineSubstanceTable, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(medicine, medicineSubstance)
}