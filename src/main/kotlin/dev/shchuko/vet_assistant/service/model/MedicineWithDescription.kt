package dev.shchuko.vet_assistant.service.model

data class MedicineWithDescription(
    val name: String,
    val description: String,
    val analogues: List<String>,
    val activeIngredients: List<String>,
)
