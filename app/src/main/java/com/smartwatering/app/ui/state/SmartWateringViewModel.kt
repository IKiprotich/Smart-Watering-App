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
import kotlinx.coroutines.delay
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
    val isSavingPlant: Boolean = false,
    val isLoggingWater: Boolean = false,
    val error: String? = null
)

class SmartWateringViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PlantRepository(AppDatabase.get(application))
    private val csvExporter = CsvExporter(repository)
    private val today = LocalDate.now(Nairobi).toString()
    private val mutableState = MutableStateFlow(GardenUiState())

    val state: StateFlow<GardenUiState> = mutableState.asStateFlow()

    init {
        observePlants()
        observeTodayDecisions()
        loadInitialGardenData()
    }

    fun refresh() {
        Scheduler.runNow(getApplication())
        mutableState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            delay(REFRESH_INDICATOR_MS)
            val cachedWeather = runCatching { WeatherClient.cached(getApplication()) }.getOrNull()
            mutableState.update { it.copy(isRefreshing = false, weather = cachedWeather) }
        }
    }

    suspend fun addPlant(name: String, species: Species): Result<Unit> {
        mutableState.update { it.copy(isSavingPlant = true, error = null) }
        return runCatching {
            val plantId = repository.addPlant(name, species)
            repository.logWatering(plantId)
            Scheduler.runNow(getApplication())
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
            mutableState.update { it.copy(checkIns = repository.allCheckIns()) }
            Scheduler.runNow(getApplication())
            checkInId
        }.onFailure { error ->
            mutableState.update { it.copy(error = error.message ?: "Could not save watering") }
        }.also {
            mutableState.update { it.copy(isLoggingWater = false) }
        }
    }

    suspend fun undoCheckIn(id: Long) {
        repository.undoCheckIn(id)
        Scheduler.runNow(getApplication())
        mutableState.update { it.copy(checkIns = repository.allCheckIns()) }
    }

    private fun observePlants() {
        viewModelScope.launch {
            repository.plants().collect { plants ->
                mutableState.update { it.copy(plants = plants) }
            }
        }
    }

    private fun observeTodayDecisions() {
        viewModelScope.launch {
            repository.decisions(today).collect { decisions ->
                mutableState.update { it.copy(decisions = decisions) }
            }
        }
    }

    private fun loadInitialGardenData() {
        viewModelScope.launch {
            val checkIns = repository.allCheckIns()
            val weather = runCatching { WeatherClient.cached(getApplication()) }.getOrNull()
            mutableState.update { it.copy(checkIns = checkIns, weather = weather) }
        }
    }

    companion object {
        private val Nairobi = ZoneId.of("Africa/Nairobi")
        private const val REFRESH_INDICATOR_MS = 700L
    }
}
