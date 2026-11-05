package com.smartwatering.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.smartwatering.app.data.CheckInType
import com.smartwatering.app.data.Plant
import com.smartwatering.app.model.DailyWeather
import com.smartwatering.app.ui.components.EmptyPanel
import com.smartwatering.app.ui.components.SpeciesAvatar
import com.smartwatering.app.ui.state.GardenUiState
import com.smartwatering.app.ui.state.SmartWateringViewModel
import com.smartwatering.app.ui.util.NairobiZone
import com.smartwatering.app.ui.util.localDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(nav: NavHostController, vm: SmartWateringViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val today = LocalDate.now(NairobiZone)

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        TodayHeader(today = today, state = state, onRefresh = vm::refresh)
        Spacer(Modifier.height(18.dp))
        DailyPlanCard(state = state)
        Spacer(Modifier.height(12.dp))
        WeatherCards(weather = state.weather, today = today)
        Spacer(Modifier.height(12.dp))
        CareStreakCard(state = state, today = today)
        Spacer(Modifier.height(20.dp))
        Text(
            "Your plants",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))
        TodayPlantList(state, nav)
    }
}

@Composable
private fun TodayHeader(state: GardenUiState, today: LocalDate, onRefresh: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                today.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Your garden",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        IconButton(
            onClick = onRefresh,
            enabled = !state.isRefreshing,
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            if (state.isRefreshing) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh today's watering plan")
            }
        }
    }
}

@Composable
private fun DailyPlanCard(state: GardenUiState) {
    val plantsToWater = state.decisions.count { it.water }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(22.dp)) {
            Text(
                "TODAY’S PLAN",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .78f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "$plantsToWater of ${state.plants.size}",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (plantsToWater == 1) "plant needs a drink" else "plants need a drink",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                planDescription(state, plantsToWater),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .86f)
            )
        }
    }
}

private fun planDescription(state: GardenUiState, plantsToWater: Int): String = when {
    state.plants.isEmpty() -> "Add your first plant to get a daily care plan."
    plantsToWater > 0 -> "Start with the thirsty ones below."
    else -> "Your plants are looking settled for today."
}

@Composable
private fun WeatherCards(weather: DailyWeather?, today: LocalDate) {
    val index = weatherIndex(weather, today)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WeatherSummary(
            modifier = Modifier.weight(1f),
            title = "Nairobi weather",
            value = index?.let { weatherSummary(weather, it) } ?: "Waiting for weather",
            icon = weatherIcon(weather, index)
        )
        WeatherSummary(
            modifier = Modifier.weight(1f),
            title = "Recent rainfall",
            value = index?.let { "${"%.1f".format(weather?.rain?.getOrNull(it) ?: 0.0)} mm" }
                ?: "No cached data",
            icon = Icons.Filled.WaterDrop
        )
    }
}

private fun weatherIndex(weather: DailyWeather?, today: LocalDate): Int? =
    weather?.dates?.indexOf(today.toString())?.takeIf { it >= 0 }
        ?: weather?.dates?.lastIndex

private fun weatherSummary(weather: DailyWeather?, index: Int): String {
    val temperature = weather?.tempMean?.getOrNull(index)?.toInt()
    val condition = weather?.condition(index) ?: "Condition unavailable"
    return temperature?.let { "$it°C · $condition" } ?: condition
}

private fun weatherIcon(
    weather: DailyWeather?,
    index: Int?
): androidx.compose.ui.graphics.vector.ImageVector {
    val code = index?.let { weather?.weatherCode?.getOrNull(it) } ?: -1
    return when (code) {
        0, 1 -> Icons.Filled.WbSunny
        2, 3, 45, 48 -> Icons.Filled.Cloud
        in 51..67, in 80..82 -> Icons.Filled.WaterDrop
        in 71..77, in 85..86 -> Icons.Filled.AcUnit
        in 95..99 -> Icons.Filled.Thunderstorm
        else -> Icons.Filled.Cloud
    }
}

@Composable
private fun WeatherSummary(
    modifier: Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            Modifier.padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CareStreakCard(state: GardenUiState, today: LocalDate) {
    val wateredDates = state.checkIns
        .filter { it.type == CheckInType.WATERED }
        .map { localDate(it.timestamp) }
        .toSet()
    val streak = careStreak(wateredDates, today)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = .16f)
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            if (streak > 0) "$streak day${if (streak == 1) "" else "s"} of garden care"
            else "Your care streak starts today",
            Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TodayPlantList(state: GardenUiState, nav: NavHostController) {
    if (state.plants.isEmpty()) {
        EmptyPanel(
            "Start with one plant",
            "Add a plant and get a simple, weather-aware answer each day.",
            "Add your first plant"
        ) { nav.navigate("add") }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        items(state.plants, key = Plant::id) { plant ->
            val decision = state.decisions.firstOrNull { it.plantId == plant.id }
            Card(
                onClick = { nav.navigate("plant/${plant.id}") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpeciesAvatar(plant.species, 46.dp)
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            plant.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            plant.species.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            decision?.reason ?: "Your care plan is updating",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AssistChip(
                        onClick = { nav.navigate("plant/${plant.id}") },
                        label = {
                            Text(
                                when {
                                    decision == null -> "Checking"
                                    decision.water -> "Water"
                                    else -> "Wait"
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

private fun careStreak(wateredDates: Set<LocalDate>, today: LocalDate): Int {
    var date = if (today in wateredDates) today else today.minusDays(1)
    var count = 0
    while (date in wateredDates) {
        count++
        date = date.minusDays(1)
    }
    return count
}
