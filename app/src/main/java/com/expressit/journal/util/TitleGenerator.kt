package com.expressit.journal.util

import java.util.Locale

/**
 * Generates short entry headings from transcribed text — instantly, offline.
 *
 * Extractive by design: it lifts the most representative words from the entry
 * rather than inventing new ones, which keeps titles truthful to what was said.
 * [generate] is deterministic per (text, seed) so "regenerate" cycles through
 * distinct strategies.
 */
object TitleGenerator {

    private const val MAX_WORDS = 7

    private val stopWords = setOf(
        // English
        "the", "a", "an", "and", "or", "but", "so", "of", "to", "in", "on", "at",
        "is", "are", "was", "were", "be", "been", "am", "i", "you", "he", "she",
        "it", "we", "they", "my", "your", "his", "her", "its", "our", "their",
        "this", "that", "these", "those", "with", "for", "as", "by", "from",
        "just", "like", "very", "really", "there", "here", "what", "which",
        "not", "no", "do", "does", "did", "have", "has", "had", "will", "would",
        "can", "could", "should", "about", "them", "then", "than", "when", "me",
        // German
        "der", "die", "das", "ein", "eine", "und", "oder", "aber", "ich", "du",
        "er", "sie", "es", "wir", "ihr", "ist", "sind", "war", "waren", "mit",
        "für", "von", "auf", "im", "in", "zu", "den", "dem", "nicht", "auch",
        "dass", "wie", "hat", "haben", "bin", "sehr", "mein", "dein", "sein"
    )

    fun generate(text: String, seed: Int = 0): String {
        val clean = text.trim()
        if (clean.isEmpty()) return ""

        val sentences = clean
            .split(Regex("(?<=[.!?…])\\s+|\\n+"))
            .map { it.trim().trim('"', '\'') }
            .filter { it.isNotEmpty() }
        if (sentences.isEmpty()) return ""

        val strategies = listOf(
            { openingPhrase(sentences) },
            { keySentence(sentences) },
            { keywordTitle(clean) }
        )
        val ordered = strategies.indices.map { strategies[(it + seed) % strategies.size] }
        for (strategy in ordered) {
            val candidate = strategy().trim()
            if (candidate.isNotEmpty()) return polish(candidate)
        }
        return polish(sentences.first())
    }

    /** Strategy 1 — the natural opening: how the entry itself begins. */
    private fun openingPhrase(sentences: List<String>): String =
        clip(sentences.first())

    /** Strategy 2 — the sentence carrying the most signal words. */
    private fun keySentence(sentences: List<String>): String {
        val frequencies = wordFrequencies(sentences.joinToString(" "))
        val best = sentences.maxByOrNull { sentence ->
            val words = tokenize(sentence)
            if (words.isEmpty()) 0.0
            else words.sumOf { frequencies[it] ?: 0 }.toDouble() / words.size
        } ?: sentences.first()
        return clip(best)
    }

    /** Strategy 3 — the entry's strongest keywords, joined. */
    private fun keywordTitle(text: String): String {
        val top = wordFrequencies(text).entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key.length })
            .take(3)
            .map { entry -> entry.key.replaceFirstChar { it.titlecase(Locale.getDefault()) } }
        return top.joinToString(" · ")
    }

    private fun wordFrequencies(text: String): Map<String, Int> =
        tokenize(text).groupingBy { it }.eachCount()

    private fun tokenize(text: String): List<String> =
        Regex("[\\p{L}][\\p{L}'-]{2,}").findAll(text.lowercase(Locale.getDefault()))
            .map { it.value }
            .filter { it !in stopWords }
            .toList()

    private fun clip(sentence: String): String {
        val words = sentence.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val clipped = words.take(MAX_WORDS).joinToString(" ")
        return if (words.size > MAX_WORDS) "$clipped…" else clipped
    }

    private fun polish(raw: String): String {
        var title = raw.trim().trimEnd('.', ',', ';', ':', '!', '?')
        title = title.replaceFirstChar { it.titlecase(Locale.getDefault()) }
        return title
    }
}
