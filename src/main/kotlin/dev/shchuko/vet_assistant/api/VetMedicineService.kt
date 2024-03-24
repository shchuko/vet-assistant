package dev.shchuko.vet_assistant.api

import dev.shchuko.vet_assistant.service.model.MedicineSearchResult
import dev.shchuko.vet_assistant.service.model.MedicineWithDescription

interface VetMedicineService {
    fun search(name: String): MedicineSearchResult

    fun getAll(): List<MedicineWithDescription>

    fun setAll(entries: List<MedicineWithDescription>)
}