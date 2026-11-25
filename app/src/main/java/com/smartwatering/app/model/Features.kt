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
    val featureOrder = listOf(
        "temp_mean_yday", "temp_max_yday", "humidity_mean_yday", "wind_max_yday",
        "rain_yday", "rain_last_3d", "et0_yday", "et0_last_3d", "days_since_rain",
        "days_since_watered", "kc", "threshold_mm"
    )

    fun build(w: DailyWeather, species: Species, daysSinceWatered: Int): FloatArray {
        val y = w.dates.size - 2
        require(y >= 2) { "Need at least 3 past days of weather" }
        val rainLast3 = w.rain[y - 2] + w.rain[y - 1] + w.rain[y]
        val et0Last3 = w.et0[y - 2] + w.et0[y - 1] + w.et0[y]
        return floatArrayOf(
            w.tempMean[y].toFloat(), w.tempMax[y].toFloat(), w.humidityMean[y].toFloat(),
            w.windMax[y].toFloat(), w.rain[y].toFloat(), rainLast3.toFloat(),
            w.et0[y].toFloat(), et0Last3.toFloat(), dryDays(w, y),
            daysSinceWatered.toFloat(), species.kc, species.thresholdMm
        )
    }

    fun acceptsForPrediction(values: FloatArray): Boolean {
        if (values.size != featureOrder.size || values.any { !it.isFinite() }) return false
        val (mean, max, humidity) = values
        return mean in -5f..45f && max in mean..45f && humidity in 0f..100f &&
            (3..8).all { values[it] >= 0f } &&
            values[9] >= 1f && values[10] > 0f && values[11] > 0f
    }

    private fun dryDays(weather: DailyWeather, yesterday: Int): Float {
        // The shipped tree's highest dry-day split is 70.
        for (days in 0 until 71) {
            val rainfall = weather.rain.getOrNull(yesterday - days) ?: return Float.NaN
            if (!rainfall.isFinite()) return Float.NaN
            if (rainfall >= MIN_EFFECTIVE_RAIN) return days.toFloat()
        }
        return 71f
    }
}
