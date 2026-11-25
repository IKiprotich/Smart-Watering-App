package com.smartwatering.app.ui.state

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartwatering.app.data.AppDatabase
import com.smartwatering.app.data.CheckIn
import com.smartwatering.app.data.CheckInType
import com.smartwatering.app.data.DecisionRecord
import com.smartwatering.app.data.Plant
import com.smartwatering.app.data.PlantRepository
import com.smartwatering.app.model.DailyWeather
import com.smartwatering.app.model.Species
import com.smartwatering.app.ui.util.CsvExporter
import com.smartwatering.app.work.Scheduler
import com.smartwatering.app.weather.WeatherClient
import androidx.work.WorkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.smartwatering.app.ui.util.localDate
import java.time.LocalDate
import java.time.ZoneId

data class GardenUiState(
    val plants: List<Plant> = emptyList(),
    val modelDecisions: List<DecisionRecord> = emptyList(),
    val yesterdayDecisions: List<DecisionRecord> = emptyList(),
    val checkIns: List<CheckIn> = emptyList(),
    val weather: DailyWeather? = null,
    val weatherUpdatedAt: Long? = null,
    val offline: Boolean = false,
    val plantsLoaded: Boolean = false,
    val plantsError: Boolean = false,
    val isRefreshing: Boolean = false,
    val refreshMessage: String? = null,
    val isSavingPlant: Boolean = false,
    val isLoggingWater: Boolean = false,
    val error: String? = null
) {
    val decisions: List<DecisionRecord>
        get() {
            val today = LocalDate.now(ZoneId.of("Africa/Nairobi"))
            val watered = checkIns.filter {
                it.type == CheckInType.WATERED && localDate(it.timestamp) == today
            }.mapTo(mutableSetOf()) { it.plantId }
            return modelDecisions.filter {
                it.date == today.toString() && it.plantId !in watered && it.reason != "Watered today"
            } + watered.map { DecisionRecord(it, today.toString(), false, "Watered today") }
        }
}

class SmartWateringViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PlantRepository(AppDatabase.get(application))
    private val csvExporter = CsvExporter(repository)
    private var decisionDate: String? = null
    private var decisionObserver: Job? = null
    private var refreshJob: Job? = null
    private var plantObserver: Job? = null
    private val mutableState = MutableStateFlow(GardenUiState())

    val state: StateFlow<GardenUiState> = mutableState.asStateFlow()

    init {
        observePlants()
        viewModelScope.launch {
            repository.observeCheckIns().collect { checkIns ->
                mutableState.update { it.copy(checkIns = checkIns) }
            }
        }
        refresh()
    }

    fun refresh() = refreshPlan()

    private fun refreshPlan(replaceRunning: Boolean = false) {
        observeTodayDecisions()
        if (state.value.isRefreshing && !replaceRunning) return
        refreshJob?.cancel()
        mutableState.update { it.copy(isRefreshing = true, refreshMessage = null) }
        refreshJob = viewModelScope.launch {
            loadCachedWeather()
            val work = Scheduler.runNow(getApplication())
            val result = withTimeoutOrNull(35_000) {
                work.first {
                    it.state.isFinished ||
                        (it.state == WorkInfo.State.ENQUEUED && it.runAttemptCount > 0)
                }
            }
            loadCachedWeather()
            mutableState.update {
                it.copy(
                    isRefreshing = false,
                    refreshMessage = if (result?.state == WorkInfo.State.SUCCEEDED) null
                    else "Couldn’t refresh the care plan. Tap refresh to try again."
                )
            }
        }
    }

    private suspend fun loadCachedWeather() {
        val context = getApplication<Application>()
        val (snapshot, offline) = withContext(Dispatchers.IO) {
            val manager = context.getSystemService(android.net.ConnectivityManager::class.java)
            val snapshot = runCatching { WeatherClient.snapshot(context) }.getOrNull()
            snapshot to (manager.activeNetwork == null)
        }
        mutableState.update {
            it.copy(weather = snapshot?.weather, weatherUpdatedAt = snapshot?.updatedAtMillis, offline = offline)
        }
    }

    suspend fun addPlant(name: String, species: Species, lastWateredAt: Long?): Result<Unit> {
        mutableState.update { it.copy(isSavingPlant = true, error = null) }
        return runCatching {
            repository.addPlant(name, species, lastWateredAt)
            refreshPlan(replaceRunning = true)
        }.onFailure { error ->
            mutableState.update { it.copy(error = error.message ?: "Could not save this plant") }
        }.also {
            mutableState.update { it.copy(isSavingPlant = false) }
        }
    }

    suspend fun updateSpecies(id: Long, species: Species): Result<Unit> = runCatching {
        repository.updateSpecies(id, species, LocalDate.now(Nairobi).toString())
        refreshPlan(replaceRunning = true)
    }

    fun removePlant(id: Long) {
        viewModelScope.launch { repository.removePlant(id) }
    }

    fun plant(id: Long) = repository.plant(id)

    fun history(id: Long) = repository.history(id)

    suspend fun exportCsv(uri: Uri) = csvExporter.export(getApplication(), uri)

    suspend fun logCheckIn(plantId: Long, type: CheckInType): Result<Long> {
        mutableState.update { it.copy(isLoggingWater = true, error = null) }
        return runCatching {
            val checkInId = repository.logCheckIn(plantId, type)
            if (type == CheckInType.WATERED) refreshPlan(replaceRunning = true)
            checkInId
        }.onFailure { error ->
            mutableState.update { it.copy(error = error.message ?: "Could not save watering") }
        }.also {
            mutableState.update { it.copy(isLoggingWater = false) }
        }
    }

    suspend fun undoCheckIn(id: Long) {
        repository.undoCheckIn(id)
        refreshPlan(replaceRunning = true)
    }

    fun retryPlants() = observePlants()

    private fun observePlants() {
        plantObserver?.cancel()
        mutableState.update { it.copy(plantsError = false) }
        plantObserver = viewModelScope.launch {
            repository.plants().catch {
                mutableState.update { it.copy(plantsError = true) }
            }.collect { plants ->
                mutableState.update { it.copy(plants = plants, plantsLoaded = true, plantsError = false) }
            }
        }
    }

    private fun observeTodayDecisions() {
        val today = LocalDate.now(Nairobi)
        if (decisionDate == today.toString()) return
        decisionDate = today.toString()
        decisionObserver?.cancel()
        mutableState.update { it.copy(modelDecisions = emptyList(), yesterdayDecisions = emptyList()) }
        decisionObserver = viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                repository.decisions(today.toString()),
                repository.decisions(today.minusDays(1).toString())
            ) { current, previous -> current to previous }.collect { (current, previous) ->
                mutableState.update { it.copy(modelDecisions = current, yesterdayDecisions = previous) }
            }
        }
    }

    companion object {
        private val Nairobi = ZoneId.of("Africa/Nairobi")
    }
}
