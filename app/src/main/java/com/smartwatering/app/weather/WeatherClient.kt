package com.smartwatering.app.weather

import android.content.Context
import android.util.AtomicFile
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
            "&past_days=92&forecast_days=1&timezone=Africa%2FNairobi"

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
            val weather = DailyWeather.fromOpenMeteo(response)
            val cache = AtomicFile(cacheFile(context))
            val stream = cache.startWrite()
            try {
                stream.write(response.toByteArray(Charsets.UTF_8))
                cache.finishWrite(stream)
            } catch (error: Exception) {
                cache.failWrite(stream)
                throw error
            }
            weather
        } finally {
            connection.disconnect()
        }
    }

    fun cached(context: Context): DailyWeather? = if (cacheFile(context).exists()) {
        val response = AtomicFile(cacheFile(context)).readFully()
        DailyWeather.fromOpenMeteo(response.toString(Charsets.UTF_8))
    } else {
        null
    }

    private const val NETWORK_TIMEOUT_MS = 15_000
}
