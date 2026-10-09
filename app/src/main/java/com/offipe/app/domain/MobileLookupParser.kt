package com.offipe.app.domain

import java.util.Locale

sealed interface MobileLookupResult {
    data class Linked(val name: String) : MobileLookupResult
    data object NotLinked : MobileLookupResult
    data object Failed : MobileLookupResult
}

object MobileLookupParser {
    val MOBILE_PROMPT = Regex("""enter\s+mobile\s+no""", RegexOption.IGNORE_CASE)
    val LINKED = Regex(
        """paying\s+(.+?)\s*,\s*enter\s+amount""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    val NOT_LINKED = Regex(
        """(entered\s+upi\s+id\s+is\s+invalid|enter\s+correct\s+upi\s+id)""",
        RegexOption.IGNORE_CASE
    )

    private val whitespace = Regex("\\s+")
    private val controlsAndBraces = Regex("[\\p{Cc}{}]")
    private val mobilePattern = Regex("^[6-9]\\d{9}$")

    fun normalizeMobile(raw: String): String {
        var normalized = raw.filterNot(Char::isWhitespace)
        if (normalized.startsWith("+91")) normalized = normalized.drop(3)
        if (normalized.startsWith('0')) normalized = normalized.drop(1)
        return normalized
    }

    fun isValidMobile(raw: String): Boolean = mobilePattern.matches(normalizeMobile(raw))

    fun classify(text: String): MobileLookupResult {
        if (NOT_LINKED.containsMatchIn(text)) return MobileLookupResult.NotLinked
        val match = LINKED.find(text) ?: return MobileLookupResult.Failed
        val name = sanitizeName(match.groupValues[1])
        return if (name.isEmpty()) MobileLookupResult.Failed else MobileLookupResult.Linked(name)
    }

    fun sanitizeName(raw: String): String = raw
        .replace(whitespace, " ")
        .replace(controlsAndBraces, "")
        .trim()
        .take(40)
        .trimEnd()

    fun titleCaseName(name: String): String = name
        .lowercase(Locale.ROOT)
        .split(' ')
        .joinToString(" ") { word ->
            word.replaceFirstChar { character -> character.titlecase(Locale.ROOT) }
        }
}