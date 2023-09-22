package dev.shchuko.vet_assistant.medicine.api

import java.util.*

data class MedicineDto(
    val id: UUID,
    val name: String,
    val description: String,
    val substances: List<MedicineSubstanceDto>,
    val analogueNames: List<String>
)

sealed interface MedicineSearchResult {
    data object NoneMatch : MedicineSearchResult
    data class SingleMatchByName(val value: MedicineDto, val misspell: Boolean) : MedicineSearchResult
    data class MultipleMatch(val medicineNames: List<String>, val substanceNames: List<String>) : MedicineSearchResult
}

sealed interface MedicineDeleteResult {
    data class NotFound(val medicineId: UUID) : MedicineDeleteResult
    data class Deleted(val deletedValue: MedicineDto) : MedicineDeleteResult
    data class SafeDeleteFailed(val linkedAnalogueNames: List<String>) : MedicineDeleteResult
}

data class MedicineSubstanceDto(val id: UUID, val name: String)


sealed interface MedicineSubstanceDeleteResult {
    data class NotFound(val medicineSubstanceId: UUID) : MedicineSubstanceDeleteResult
    data class Deleted(val deletedValue: MedicineSubstanceDto) : MedicineSubstanceDeleteResult
    data class SafeDeleteFailed(val linkedMedicineNames: List<String>) : MedicineSubstanceDeleteResult
}