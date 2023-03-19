package dev.shchuko.vet_assistant.medicine.api.model

import dev.shchuko.vet_assistant.medicine.api.model.MedicineSearchResult.ResultType.*

data class MedicineSearchResult(val type: ResultType, val result: List<MedicineWithDescription>) {
    enum class ResultType {
        EXACT_MATCH,
        MISSPELL_SINGLE_MATCH,
        MISSPELL_MULTIPLE_MATCH,
        NONE_MATCH
    }

    companion object {
        private val NONE_MATCH_RESULT = MedicineSearchResult(NONE_MATCH, emptyList())

        fun buildResultOf(
            result: List<MedicineWithDescription> = emptyList(),
            misspell: Boolean = false
        ): MedicineSearchResult = when (result.size) {
            0 -> {
                assert(!misspell)
                NONE_MATCH_RESULT
            }

            1 -> MedicineSearchResult(
                if (misspell) MISSPELL_SINGLE_MATCH else EXACT_MATCH,
                result
            )

            else -> {
                assert(misspell)
                MedicineSearchResult(MISSPELL_MULTIPLE_MATCH, result)
            }
        }
    }
}
