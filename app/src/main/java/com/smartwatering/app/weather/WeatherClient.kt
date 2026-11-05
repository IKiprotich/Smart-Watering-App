package com.smartwatering.app.weather

import android.content.Context
import com.smartwatering.app.model.DailyWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object WeatherClient {
    private const val endpoint =
        "https://api.open-meteo.com/v1/forecast?latitude=-1.2921&longitude=36.8219" +
            "&daily=temperature_2m_mean,temperature_2m_max,relative_humidity_2m_mean" +
            ",wind_speed_10m_max,precipitation_sum,et0_fao_evapotranspiration,weather_code" +
            "&past_days=30&forecast_days=1&timezone=Africa%2FNairobi"

    private fun cacheFile(context: Context) = File(context.filesDir, "weather.json")

    suspend fun fetch(context: Context): DailyWeather = withContext(Dispatchers.IO) {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.connectTimeout = NETWORK_TIMEOUT_MS
        connection.readTimeout = NETWORK_TIMEOUT_MS
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) {
                "Weather service returned ${connection.responseCode}"
            }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            cacheFile(context).writeText(response)
            DailyWeather.fromOpenMeteo(response)
        } finally {
            connection.disconnect()
        }
    }

    fun cached(context: Context): DailyWeather? = cacheFile(context)
        .takeIf(File::exists)
        ?.let { DailyWeather.fromOpenMeteo(it.readText()) }

    private const val NETWORK_TIMEOUT_MS = 15_000
}
