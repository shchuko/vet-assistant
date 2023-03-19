package dev.shchuko.vet_assistant.medicine.impl.service.util

import dev.shchuko.vet_assistant.medicine.impl.service.util.ClosestWordsSearchUtil.SearchResult.Type.*
import org.apache.commons.text.similarity.LevenshteinDistance
import java.util.concurrent.ConcurrentHashMap

/**
 * Primitive case-insensitive Levenshtein Distance based "the closest words" search engine
 */
class ClosestWordsSearchUtil(
    private val cacheLimit: Long = 10000,
    private val sameWordMaxEditDistance: Int = 1,
    private val resultLimit: Int = 5,
    private val dictProvider: () -> List<String>
) {
    companion object {

        private const val LEVENSHTEIN_THRESHOLD_MIN = 1
        private const val LEVENSHTEIN_THRESHOLD_MAX = 15

        /* Fill map of LevenshteinDistance instances with different thresholds */
        private val levenshteinCalcMap =
            (LEVENSHTEIN_THRESHOLD_MIN..LEVENSHTEIN_THRESHOLD_MAX).associateWith { LevenshteinDistance(it) }

        private fun levenshteinApply(first: String, second: String): Int {
            // use average length as a base for threshold selection
            val avgLen = (first.length + second.length) / 3
            // allow to change not more than a quarter of average length. if edit distance is bigger - drop the word.
            val threshold = avgLen / 4
            // fit in threshold range
            val calcThreshold = threshold.coerceIn(LEVENSHTEIN_THRESHOLD_MIN, LEVENSHTEIN_THRESHOLD_MAX)
            return levenshteinCalcMap[calcThreshold]!!.apply(first, second)
        }
    }

    data class SearchResult(val words: List<String>, val resultType: Type) {
        enum class Type {
            NOTHING_FOUND,
            EXACT_MATCH,
            MISSPELL_CORRECTION_SINGLE_MATCH,
            MISSPELL_CORRECTION_MULTIPLE_MATCH;
        }
    }

    @Volatile
    private lateinit var dict: Map<String, String>
    private val cache: MutableMap<String, SearchResult> = ConcurrentHashMap()

    init {
        refreshDict()
    }


    fun findWord(string: String): SearchResult {
        assert(this@ClosestWordsSearchUtil::dict.isInitialized)

        val stringLower = string.lowercase()

        val exactMatch = getExactMatch(stringLower)
        if (exactMatch != null) {
            return SearchResult(listOf(exactMatch), EXACT_MATCH)
        }

        val cached = findSimilaritiesInCache(stringLower)
        if (cached != null) return cached

        val similarities = calculateSimilaritiesList(stringLower)
        addSimilaritiesToCache(stringLower, similarities)
        return similarities
    }

    @Synchronized
    fun refreshDict() {
        dict = dictProvider().asSequence().map { it.trim().lowercase() to it }.toMap()
        cache.clear()
    }

    private fun getExactMatch(string: String) = dict[string]

    private fun calculateSimilaritiesList(stringLower: String): SearchResult {
        /** Find N closest words, where N is [resultLimit] */
        val words = dict.asSequence()
            .map { (nameLower, originalName) -> originalName to levenshteinApply(stringLower, nameLower) }
            .filterNot { (_, distance) -> distance == -1 }
            .sortedBy { (_, distance) -> distance }
            .take(resultLimit)
            .toList()

        fun List<Pair<String, Int>>.dropDistanceMap() = map { it.first }

        if (words.isEmpty()) {
            return SearchResult(emptyList(), NOTHING_FOUND)
        }

        /** If single word found - assume it was requested but misspelled */
        if (words.size == 1) {
            return SearchResult(words.dropDistanceMap(), MISSPELL_CORRECTION_SINGLE_MATCH)
        }

        /** Find words with distance fits in [sameWordMaxEditDistance] - if single found, assume it was requested but misspelled */
        val wordsWithDistanceFitsInMaxMisspell = words.filter { it.second <= sameWordMaxEditDistance }
        if (wordsWithDistanceFitsInMaxMisspell.size == 1) {
            return SearchResult(
                wordsWithDistanceFitsInMaxMisspell.dropDistanceMap(),
                MISSPELL_CORRECTION_SINGLE_MATCH
            )
        }

        /** If more than one word found - return their list */
        return SearchResult(words.dropDistanceMap(), MISSPELL_CORRECTION_MULTIPLE_MATCH)
    }


    private fun findSimilaritiesInCache(string: String) = cache[string]

    private fun addSimilaritiesToCache(string: String, similarities: SearchResult) {
        // Dumb way to prevent cache overgrowth
        if (cache.size >= cacheLimit) {
            cache.clear()
        }
        cache[string] = similarities
    }
}
