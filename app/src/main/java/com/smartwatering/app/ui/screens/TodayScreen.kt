package com.smartwatering.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.smartwatering.app.data.Plant
import com.smartwatering.app.model.DailyWeather
import com.smartwatering.app.model.WeatherGlyph
import com.smartwatering.app.model.ReasonLine
import com.smartwatering.app.model.formatRainfall
import com.smartwatering.app.ui.components.DailyCareCard
import com.smartwatering.app.ui.components.EmptyPanel
import com.smartwatering.app.ui.components.StatusPill
import com.smartwatering.app.ui.components.SpeciesAvatar
import com.smartwatering.app.ui.state.GardenUiState
import com.smartwatering.app.ui.state.SmartWateringViewModel
import com.smartwatering.app.ui.util.NairobiZone
import com.smartwatering.app.ui.util.localDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(nav: NavHostController, vm: SmartWateringViewModel = viewModel(), gardenName: String = "") {
    val state by vm.state.collectAsState()
    val today = LocalDate.now(NairobiZone)
    var showHistory by rememberSaveable { mutableStateOf(false) }
    val hasHistory = state.weather != null || state.checkIns.isNotEmpty()

    if (showHistory && hasHistory) {
        ModalBottomSheet(
            onDismissRequest = { showHistory = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
            ) {
                GardenWeek(state.weather, state.checkIns, today)
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = vm::refresh,
        modifier = Modifier.fillMaxSize()
    ) {
        TodayContent(state, today, nav, vm::refresh, gardenName, vm::retryPlants,
            onHistory = if (hasHistory) ({ showHistory = true }) else null)
    }
}

@Composable
private fun TodayContent(
    state: GardenUiState,
    today: LocalDate,
    nav: NavHostController,
    refresh: () -> Unit,
    gardenName: String,
    retryPlants: () -> Unit,
    onHistory: (() -> Unit)?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TodayHeader(today = today, state = state, onRefresh = refresh, gardenName = gardenName) }
        item {
            if (state.plantsError) {
                EmptyPanel("Can’t open your garden", "Try loading your saved plants again.", "Retry", retryPlants)
            } else {
                DailyCareCard(state) { nav.navigate("add") }
            }
        }
        state.refreshMessage?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error) }
        }
        item {
            WeatherCards(weather = state.weather, today = today)
            WeatherFreshness(state.weatherUpdatedAt, state.offline)
        }
        if (onHistory != null) item {
            HistoryShortcut(state, today, onHistory)
        }
        if (state.plants.isNotEmpty()) item {
            Text(
                "Your plants",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (state.plants.isNotEmpty()) TodayPlantList(state, nav)
    }
}

@Composable
private fun TodayHeader(state: GardenUiState, today: LocalDate, onRefresh: () -> Unit, gardenName: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                today.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                gardenName.ifBlank { "Your garden" },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        IconButton(
            onClick = onRefresh,
            enabled = !state.isRefreshing,
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Refresh today's watering plan")
        }
    }
}

@Composable
private fun WeatherCards(weather: DailyWeather?, today: LocalDate) {
    val index = weatherIndex(weather, today)
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        WeatherSummary(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            title = "Nairobi weather",
            value = index?.let { weatherSummary(weather, it) } ?: "Waiting for weather",
            icon = weatherIcon(weather, index)
        )
        WeatherSummary(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            title = "Yesterday’s rain",
            value = weatherIndex(weather, today.minusDays(1))
                ?.let { weather?.rain?.getOrNull(it) }
                ?.takeIf { it.isFinite() }
                ?.let { formatRainfall(it) } ?: "Unavailable",
            icon = Icons.Filled.WaterDrop
        )
    }
}

private fun weatherIndex(weather: DailyWeather?, today: LocalDate): Int? =
    weather?.dates?.indexOf(today.toString())?.takeIf { it >= 0 }

private fun weatherSummary(weather: DailyWeather?, index: Int): String {
    val temperature = weather?.tempMean?.getOrNull(index)?.takeIf { it.isFinite() }?.toInt()
    val condition = weather?.condition(index) ?: "Condition unavailable"
    return temperature?.let { "$it°C · $condition" } ?: condition
}

private fun weatherIcon(
    weather: DailyWeather?,
    index: Int?
): androidx.compose.ui.graphics.vector.ImageVector {
    return when (WeatherGlyph.forCode(index?.let { weather?.weatherCode?.getOrNull(it) })) {
        WeatherGlyph.SUN -> Icons.Filled.WbSunny
        WeatherGlyph.PARTLY_CLOUDY -> Icons.Filled.WbCloudy
        WeatherGlyph.CLOUD, WeatherGlyph.UNKNOWN -> Icons.Filled.Cloud
        WeatherGlyph.FOG -> Icons.Filled.Waves
        WeatherGlyph.DRIZZLE -> Icons.Filled.Grain
        WeatherGlyph.RAIN -> Icons.Filled.Umbrella
        WeatherGlyph.SNOW -> Icons.Filled.AcUnit
        WeatherGlyph.THUNDER -> Icons.Filled.Thunderstorm
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
private fun HistoryShortcut(state: GardenUiState, today: LocalDate, onClick: () -> Unit) {
    val firstDay = today.minusDays(6)
    val recent = state.checkIns.count {
        val date = localDate(it.timestamp)
        date in firstDay..today
    }
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null,
                    modifier = Modifier.padding(9.dp).size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(Modifier.weight(1f)) {
                Text("View last 7 days", style = MaterialTheme.typography.titleSmall)
                val label = if (recent == 1) "1 check-in" else "$recent check-ins"
                Text("$label · Rain and care activity", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.TodayPlantList(state: GardenUiState, nav: NavHostController) {
    items(
        state.plants.sortedByDescending { plant -> state.decisions.any { it.plantId == plant.id && it.water } },
        key = Plant::id
    ) { plant ->
        val decision = state.decisions.firstOrNull { it.plantId == plant.id }
        val previous = state.yesterdayDecisions.firstOrNull { it.plantId == plant.id }
            ?.takeIf { state.refreshMessage != null }
        OutlinedCard(
            onClick = { nav.navigate("plant/${plant.id}") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        decision?.reason?.let(ReasonLine::display) ?: previous?.let {
                            "Yesterday: ${if (it.water) "Water" else "Wait"}. ${ReasonLine.display(it.reason)}"
                        } ?: "Waiting for today’s care plan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusPill(
                    label = when {
                        decision == null -> if (previous != null) "Yesterday" else "Checking"
                        decision.water -> "Water"
                        else -> "Wait"
                    },
                    needsWater = decision?.water == true
                )
            }
        }
    }
}
