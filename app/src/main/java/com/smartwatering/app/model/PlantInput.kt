package com.smartwatering.app.model

import java.util.Locale

object PlantInput {
    private val examples = mapOf(
        Species.SUCCULENT to listOf("aloe", "cactus"),
        Species.HERB to listOf("basil", "rosemary", "mint", "thyme"),
        Species.FLOWERING to listOf("marigold", "petunia", "roses", "rose"),
        Species.FERN to listOf("Boston fern", "fern"),
        Species.LEAFY_VEGETABLE to listOf("sukuma wiki", "spinach", "kale")
    )

    fun validName(input: String): String? = input.trim().takeIf(String::isNotEmpty)

    fun examplesFor(species: Species) = examples.getValue(species)
        .filterNot { it == "rose" || it == "fern" }.joinToString(", ")

    fun suggest(name: String): Species? {
        val words = name.lowercase(Locale.ROOT).split(Regex("[^\\p{L}]+")).filter(String::isNotEmpty)
        val matches = examples.filterValues { names ->
            names.any { example ->
                val parts = example.lowercase(Locale.ROOT).split(' ')
                words.windowed(parts.size).any { it == parts }
            }
        }
        return matches.keys.singleOrNull()
    }
}
