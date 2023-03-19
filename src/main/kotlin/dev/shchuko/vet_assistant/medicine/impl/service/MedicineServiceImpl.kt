package dev.shchuko.vet_assistant.medicine.impl.service

import dev.shchuko.vet_assistant.medicine.api.model.MedicineSearchResult
import dev.shchuko.vet_assistant.medicine.api.model.MedicineWithDescription
import dev.shchuko.vet_assistant.medicine.api.service.MedicineService
import dev.shchuko.vet_assistant.medicine.impl.repository.MedicineServiceRepository
import dev.shchuko.vet_assistant.medicine.impl.service.util.ClosestWordsSearchUtil
import transliterate

class MedicineServiceImpl(
    private val repository: MedicineServiceRepository
) : MedicineService {
    private val searchUtil = ClosestWordsSearchUtil(dictProvider = repository::getAllMedicineNames)

    override fun findMedicine(name: String): MedicineSearchResult {
        val nameTrimmed = name.trim()
        return findMedicineInternal(nameTrimmed)
            ?: nameTrimmed.transliterate()?.let { findMedicineInternal(it) }
            ?: MedicineSearchResult.buildResultOf()
    }

    /**
     * Returns result with at least one match. Of none found, returns null.
     */
    private fun findMedicineInternal(name: String): MedicineSearchResult? {
        val result = searchUtil.findWord(name)
        return when (result.resultType) {
            ClosestWordsSearchUtil.SearchResult.Type.NOTHING_FOUND -> null

            ClosestWordsSearchUtil.SearchResult.Type.EXACT_MATCH -> {
                assert(result.words.size == 1)
                getMedicineDescriptionFor(result.words.first())?.let { medicine ->
                    MedicineSearchResult.buildResultOf(listOf(medicine))
                }
            }

            ClosestWordsSearchUtil.SearchResult.Type.MISSPELL_CORRECTION_SINGLE_MATCH -> {
                assert(result.words.size == 1)
                getMedicineDescriptionFor(result.words.first())?.let { medicine ->
                    MedicineSearchResult.buildResultOf(listOf(medicine), misspell = true)
                }
            }

            ClosestWordsSearchUtil.SearchResult.Type.MISSPELL_CORRECTION_MULTIPLE_MATCH -> {
                assert(result.words.size > 1)
                val medicines = result.words.mapNotNull { getMedicineDescriptionFor(it) }
                assert(medicines.size == result.words.size)

                MedicineSearchResult.buildResultOf(medicines, misspell = true)
            }
        }
    }

    /**
     * Query repository for medicine description with exact [name].
     */
    private fun getMedicineDescriptionFor(name: String): MedicineWithDescription? {
        val description = repository.getMedicineDescriptionByName(name)
        assert(description != null)
        // if null and assertions disabled, fallback and return null
        return description?.let { MedicineWithDescription(name, it) }
    }
}