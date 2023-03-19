package dev.shchuko.vet_assistant.medicine.impl.repository

interface MedicineServiceRepository {
    fun getAllMedicineNames(): List<String>

    fun getMedicineDescriptionByName(name: String): String?
}