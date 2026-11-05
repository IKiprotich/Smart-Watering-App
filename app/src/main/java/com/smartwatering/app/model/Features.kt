package com.smartwatering.app.model

import org.json.JSONObject

data class DailyWeather(
    val dates: List<String>, val tempMean: DoubleArray, val tempMax: DoubleArray,
    val humidityMean: DoubleArray, val windMax: DoubleArray,
    val rain: DoubleArray, val et0: DoubleArray,
    val weatherCode: IntArray,
) {
    companion object {
        fun fromOpenMeteo(json: String): DailyWeather {
            val d = JSONObject(json).getJSONObject("daily")
            fun col(name: String) = d.getJSONArray(name).let { a ->
                DoubleArray(a.length()) { i -> if (a.isNull(i)) Double.NaN else a.getDouble(i) }
            }
            val time = d.getJSONArray("time")
            return DailyWeather(
                List(time.length()) { time.getString(it) }, col("temperature_2m_mean"),
                col("temperature_2m_max"), col("relative_humidity_2m_mean"),
                col("wind_speed_10m_max"), col("precipitation_sum"),
                col("et0_fao_evapotranspiration"),
                d.optJSONArray("weather_code")?.let { a -> IntArray(a.length()) { i -> if (a.isNull(i)) -1 else a.optInt(i, -1) } }
                    ?: IntArray(time.length()) { -1 }
            )
        }
    }

    fun condition(index: Int): String = when (weatherCode.getOrNull(index) ?: -1) {
        0 -> "Clear"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        51, 53, 55, 56, 57 -> "Drizzle"
        61, 63, 65, 66, 67 -> "Rain"
        71, 73, 75, 77 -> "Snow"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95, 96, 99 -> "Thunderstorms"
        else -> "Condition unavailable"
    }
}

object FeatureBuilder {
    const val MIN_EFFECTIVE_RAIN = 2.0
    fun build(w: DailyWeather, species: Species, daysSinceWatered: Int): FloatArray {
        val y = w.dates.size - 2
        require(y >= 3) { "Need at least 4 past days of weather" }
        val rainLast3 = w.rain[y - 2] + w.rain[y - 1] + w.rain[y]
        val et0Last3 = w.et0[y - 2] + w.et0[y - 1] + w.et0[y]
        var daysSinceRain = 0
        var i = y
        while (i >= 0 && w.rain[i] < MIN_EFFECTIVE_RAIN) { daysSinceRain++; i-- }
        return floatArrayOf(
            w.tempMean[y].toFloat(), w.tempMax[y].toFloat(), w.humidityMean[y].toFloat(),
            w.windMax[y].toFloat(), w.rain[y].toFloat(), rainLast3.toFloat(),
            w.et0[y].toFloat(), et0Last3.toFloat(), daysSinceRain.toFloat(),
            daysSinceWatered.toFloat(), species.kc, species.thresholdMm
        )
    }
}

object ReasonLine {
    private fun days(n: Int) = "$n day${if (n == 1) "" else "s"}"

    private fun phrase(s: Step): Pair<String, Boolean>? {
        val v = s.value
        return when (s.feature) {
            8 -> if (v < 1f) "Rained yesterday" to false else "No proper rain for ${days(v.toInt())}" to true
            5 -> if (v < 1f) "No rain in the last 3 days" to true else "${"%.0f".format(v)} mm of rain in the last 3 days" to false
            4 -> if (v < 1f) "Dry yesterday" to true else "Rained ${"%.0f".format(v)} mm yesterday" to false
            6, 7 -> if (s.wentLeft) "Mild weather" to false else "Hot, drying weather" to true
            9 -> if (s.wentLeft) "Watered ${days(v.toInt())} ago" to false else "Last watered ${days(v.toInt())} ago" to true
            2 -> if (s.wentLeft) "Dry air yesterday" to true else "Humid yesterday" to false
            0, 1 -> if (s.wentLeft) "Cool yesterday" to false else "Warm yesterday" to true
            else -> null
        }
    }

    fun of(d: Decision): String = d.path.asReversed()
        .mapNotNull(::phrase)
        .filter { it.second == d.water }
        .map { it.first }
        .distinct()
        .take(2)
        .joinToString(", ")
        .ifEmpty { if (d.water) "Soil is likely dry" else "Soil is likely still moist" }
}
