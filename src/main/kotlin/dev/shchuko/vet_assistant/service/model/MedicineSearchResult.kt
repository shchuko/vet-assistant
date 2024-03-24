package dev.shchuko.vet_assistant.service.model

data class MedicineSearchResult(
    val misspellMatches: List<String>,
    val medicine: MedicineWithDescription?,
)

