package dev.shchuko.vet_assistant.medicine.api.service

import dev.shchuko.vet_assistant.medicine.api.model.MedicineSearchResult

interface MedicineService {
    /**
     * Find medicine by name. This method tries to perform misspell correction and transliteration while searching.
     */
    fun findMedicine(name: String): MedicineSearchResult
}