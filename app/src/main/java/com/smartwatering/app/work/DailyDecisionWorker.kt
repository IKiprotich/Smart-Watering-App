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
import kotlinx.coroutines.CancellationException
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
        val requiredDates = (3L downTo 0L).map { today.minusDays(it).toString() }
        if (weather.dates.takeLast(4) != requiredDates) return@withContext Result.retry()

        val tree = loadDecisionTree() ?: return@withContext Result.failure()
        repository.allPlants().forEach { plant ->
            val lastWateredAt = repository.lastWatered(plant.id)
            val daysSinceWatered = daysSinceLastWatering(lastWateredAt, today)
            val decision = if (daysSinceWatered == 0) {
                DecisionRecord(plant.id, today.toString(), false, "Watered today")
            } else {
                val features = FeatureBuilder.build(weather, plant.species, daysSinceWatered)
                if (features.any { !it.isFinite() }) return@withContext Result.retry()
                val prediction = tree.predict(features)
                DecisionRecord(
                    plantId = plant.id,
                    date = today.toString(),
                    water = prediction.water,
                    reason = ReasonLine.of(prediction),
                    features = features.joinToString(",")
                )
            }
            repository.saveDecision(decision, lastWateredAt)
        }
        if (inputData.getBoolean(Scheduler.MORNING_RUN, false)) {
            val due = repository.currentDecisions(today.toString()).count { it.water }
            if (due > 0) CareReminder.post(applicationContext, due)
        }
        Result.success()
    }

    private suspend fun loadWeather() = try {
        WeatherClient.fetch(applicationContext)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        runCatching { WeatherClient.cached(applicationContext) }.getOrNull()
    }

    private fun loadDecisionTree(): DecisionTree? = try {
        val json = applicationContext.assets.open("decision_tree.json")
            .bufferedReader()
            .use { it.readText() }
        DecisionTree(json).also { require(it.features == FeatureBuilder.featureOrder) }
    } catch (_: Exception) {
        null
    }

    private fun daysSinceLastWatering(lastWateredAt: Long?, today: LocalDate): Int {
        val lastWatered = lastWateredAt ?: return 1
        val wateredDate = Instant.ofEpochMilli(lastWatered).atZone(Nairobi).toLocalDate()
        return ChronoUnit.DAYS.between(wateredDate, today).toInt().coerceAtLeast(0)
    }

    companion object {
        private val Nairobi: ZoneId = ZoneId.of("Africa/Nairobi")
    }
}
