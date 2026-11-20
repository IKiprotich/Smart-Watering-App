package com.smartwatering.app.ui.state

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartwatering.app.data.AppDatabase
import com.smartwatering.app.data.CheckIn
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class GardenUiState(
    val plants: List<Plant> = emptyList(),
    val decisions: List<DecisionRecord> = emptyList(),
    val checkIns: List<CheckIn> = emptyList(),
    val weather: DailyWeather? = null,
    val isRefreshing: Boolean = false,
    val refreshMessage: String? = null,
    val isSavingPlant: Boolean = false,
    val isLoggingWater: Boolean = false,
    val error: String? = null
)

class SmartWateringViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PlantRepository(AppDatabase.get(application))
    private val csvExporter = CsvExporter(repository)
    private var decisionDate: String? = null
    private var decisionObserver: Job? = null
    private var refreshJob: Job? = null
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
            val work = Scheduler.runNow(getApplication())
            val result = withTimeoutOrNull(35_000) {
                work.first {
                    it.state.isFinished ||
                        (it.state == WorkInfo.State.ENQUEUED && it.runAttemptCount > 0)
                }
            }
            val weather = withContext(Dispatchers.IO) {
                runCatching { WeatherClient.cached(getApplication()) }.getOrNull()
            }
            mutableState.update {
                it.copy(
                    isRefreshing = false,
                    weather = weather,
                    refreshMessage = if (result?.state == WorkInfo.State.SUCCEEDED) null
                    else "Couldn’t refresh the care plan. Tap refresh to try again."
                )
            }
        }
    }

    suspend fun addPlant(name: String, species: Species): Result<Unit> {
        mutableState.update { it.copy(isSavingPlant = true, error = null) }
        return runCatching {
            repository.addPlant(name, species)
            refreshPlan(replaceRunning = true)
        }.onFailure { error ->
            mutableState.update { it.copy(error = error.message ?: "Could not save this plant") }
        }.also {
            mutableState.update { it.copy(isSavingPlant = false) }
        }
    }

    fun removePlant(id: Long) {
        viewModelScope.launch { repository.removePlant(id) }
    }

    fun plant(id: Long) = repository.plant(id)

    fun history(id: Long) = repository.history(id)

    suspend fun exportCsv(uri: Uri) = csvExporter.export(getApplication(), uri)

    suspend fun logWatering(plantId: Long): Result<Long> {
        mutableState.update { it.copy(isLoggingWater = true, error = null) }
        return runCatching {
            val checkInId = repository.logWatering(plantId)
            refreshPlan(replaceRunning = true)
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

    private fun observePlants() {
        viewModelScope.launch {
            repository.plants().collect { plants ->
                mutableState.update { it.copy(plants = plants) }
            }
        }
    }

    private fun observeTodayDecisions() {
        val today = LocalDate.now(Nairobi).toString()
        if (decisionDate == today) return
        decisionDate = today
        decisionObserver?.cancel()
        mutableState.update { it.copy(decisions = emptyList()) }
        decisionObserver = viewModelScope.launch {
            repository.decisions(today).collect { decisions ->
                mutableState.update { it.copy(decisions = decisions) }
            }
        }
    }

    companion object {
        private val Nairobi = ZoneId.of("Africa/Nairobi")
    }
}
