package com.smartwatering.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartwatering.app.data.AppDatabase
import com.smartwatering.app.data.DecisionRecord
import com.smartwatering.app.data.PlantRepository
import com.smartwatering.app.model.DecisionTree
import com.smartwatering.app.model.FeatureBuilder
import com.smartwatering.app.model.ReasonLine
import com.smartwatering.app.weather.WeatherClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class DailyDecisionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val repository by lazy {
        PlantRepository(AppDatabase.get(applicationContext))
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val weather = loadWeather() ?: return@withContext Result.retry()
        val today = LocalDate.now(Nairobi)
        if (weather.dates.lastOrNull() != today.toString()) return@withContext Result.retry()

        val tree = loadDecisionTree() ?: return@withContext Result.failure()
        repository.allPlants().forEach { plant ->
            val daysSinceWatered = daysSinceLastWatering(plant.id, today)
            val decision = if (daysSinceWatered == 0) {
                DecisionRecord(plant.id, today.toString(), false, "Watered today")
            } else {
                val features = FeatureBuilder.build(weather, plant.species, daysSinceWatered)
                val prediction = tree.predict(features)
                DecisionRecord(
                    plantId = plant.id,
                    date = today.toString(),
                    water = prediction.water,
                    reason = ReasonLine.of(prediction),
                    features = features.joinToString(",")
                )
            }
            repository.saveDecision(decision)
        }
        Result.success()
    }

    private suspend fun loadWeather() = try {
        WeatherClient.fetch(applicationContext)
    } catch (_: Exception) {
        WeatherClient.cached(applicationContext)
    }

    private fun loadDecisionTree(): DecisionTree? = try {
        val json = applicationContext.assets.open("decision_tree.json")
            .bufferedReader()
            .use { it.readText() }
        DecisionTree(json)
    } catch (_: Exception) {
        null
    }

    private suspend fun daysSinceLastWatering(plantId: Long, today: LocalDate): Int {
        val lastWatered = repository.lastWatered(plantId) ?: return 1
        val wateredDate = Instant.ofEpochMilli(lastWatered).atZone(Nairobi).toLocalDate()
        return ChronoUnit.DAYS.between(wateredDate, today).toInt()
    }

    companion object {
        private val Nairobi: ZoneId = ZoneId.of("Africa/Nairobi")
    }
}
