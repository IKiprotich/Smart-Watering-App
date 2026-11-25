package com.smartwatering.app.model

import java.util.Locale

object ReasonLine {
    private val rainAmount = Regex("""(\d+(?:[.,]\d+)?)\s*mm\b""")
    private val legacyJoin = Regex(",\\s+(?=[A-Z0-9])")
    private fun days(n: Int) = "$n day${if (n == 1) "" else "s"}"

    private fun phrase(s: Step): Pair<String, Boolean>? {
        val v = s.value
        return when (s.feature) {
            8 -> if (v < 1f) "it rained yesterday" to false else "there has been no effective rain for ${days(v.toInt())}" to true
            5 -> if (v == 0f) "there was no rain in the last 3 days" to true
                else "${formatRainfall(v.toDouble())} of rain fell in the last 3 days" to false
            4 -> if (v == 0f) "yesterday was dry" to true
                else "${formatRainfall(v.toDouble())} of rain fell yesterday" to false
            6, 7 -> if (s.wentLeft) "the weather was mild" to false else "the weather was hot and drying" to true
            9 -> "your last watering was ${days(v.toInt())} ago" to !s.wentLeft
            2 -> if (s.wentLeft) "the air was dry yesterday" to true else "the air was humid yesterday" to false
            0, 1 -> if (s.wentLeft) "yesterday was cool" to false else "yesterday was warm" to true
            else -> null
        }
    }

    fun of(decision: Decision): String {
        val clauses = decision.path.asReversed().mapNotNull(::phrase)
            .filter { it.second == decision.water }.map { it.first }.distinct().take(2)
        return sentence(clauses.joinToString(" and ").ifEmpty {
            if (decision.water) "Soil is likely dry" else "Soil is likely still moist"
        })
    }

    // Older Room records remain readable without rewriting historical decisions.
    fun display(stored: String): String {
        val joined = stored.split(legacyJoin).joinToString(" and ") {
            legacyClause(it.trim().replaceFirstChar { char -> char.lowercase(Locale.ROOT) })
        }
        return sentence(rainAmount.replace(joined) {
            formatRainfall(it.groupValues[1].replace(',', '.').toDouble())
        })
    }

    private fun legacyClause(text: String): String = when {
        text == "mild weather" -> "the weather was mild"
        text == "hot, drying weather" -> "the weather was hot and drying"
        text == "humid yesterday" -> "the air was humid yesterday"
        text == "dry air yesterday" -> "the air was dry yesterday"
        text == "cool yesterday" -> "yesterday was cool"
        text == "warm yesterday" -> "yesterday was warm"
        text == "dry yesterday" -> "yesterday was dry"
        text.startsWith("rained ") -> "it $text"
        text.endsWith(" of rain in the last 3 days") ->
            text.replace(" of rain in the last 3 days", " of rain fell in the last 3 days")
        else -> text
    }

    private fun sentence(text: String): String {
        val trimmed = text.trim().trimEnd('.')
        if (trimmed.isEmpty()) return ""
        return trimmed.replaceFirstChar { it.titlecase(Locale.ROOT) } + "."
    }
}
