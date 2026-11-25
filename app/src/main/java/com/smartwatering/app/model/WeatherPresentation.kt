package com.smartwatering.app.model

import java.util.Locale

fun formatRainfall(mm: Double): String = String.format(Locale.ROOT, "%.1f mm", mm)

enum class WeatherGlyph {
    SUN, PARTLY_CLOUDY, CLOUD, FOG, DRIZZLE, RAIN, SNOW, THUNDER, UNKNOWN;

    companion object {
        fun forCode(code: Int?): WeatherGlyph = when (code) {
            0, 1 -> SUN
            2 -> PARTLY_CLOUDY
            3 -> CLOUD
            45, 48 -> FOG
            51, 53, 55, 56, 57 -> DRIZZLE
            61, 63, 65, 66, 67, 80, 81, 82 -> RAIN
            71, 73, 75, 77, 85, 86 -> SNOW
            95, 96, 99 -> THUNDER
            else -> UNKNOWN
        }
    }
}
