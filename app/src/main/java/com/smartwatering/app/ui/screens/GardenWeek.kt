package com.smartwatering.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.smartwatering.app.data.CheckIn
import com.smartwatering.app.model.DailyWeather
import com.smartwatering.app.model.formatRainfall
import com.smartwatering.app.ui.util.localDate
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun WeatherFreshness(updatedAt: Long?, offline: Boolean) {
    val now by produceState(System.currentTimeMillis(), updatedAt) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000)
        }
    }
    val message = if (updatedAt == null) {
        if (offline) "Offline. No weather has been saved yet." else null
    } else {
        val minutes = ((now - updatedAt) / 60_000).coerceAtLeast(0)
        val age = when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes min ago"
            minutes < 1_440 -> "${minutes / 60} h ago"
            else -> "${minutes / 1_440} d ago"
        }
        when {
            offline -> "Offline · using weather updated $age"
            minutes >= 360 -> "Saved weather · updated $age"
            else -> "Weather updated $age"
        }
    }
    message?.let {
        Text(it, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun GardenWeek(weather: DailyWeather?, checkIns: List<CheckIn>, today: LocalDate) {
    val dates = (6L downTo 0L).map(today::minusDays)
    val checkInCounts = remember(checkIns) { checkIns.groupingBy { localDate(it.timestamp) }.eachCount() }
    val rainfall = remember(weather, today) {
        dates.associateWith { date ->
            if (date == today || weather == null) null
            else weather.rain.getOrNull(weather.dates.indexOf(date.toString()))?.takeIf(Double::isFinite)
        }
    }
    var selected by rememberSaveable(today.toString()) { mutableIntStateOf(6) }
    val date = dates[selected]
    val blue = MaterialTheme.colorScheme.primary
    val green = MaterialTheme.colorScheme.secondary
    val maxRain = rainfall.values.filterNotNull().maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Last 7 days", style = MaterialTheme.typography.headlineMedium)
        Text("Compare daily rainfall and care activity.", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val dayWidth = (maxWidth / 7).coerceAtLeast(48.dp)
                    Row(Modifier.horizontalScroll(rememberScrollState()).selectableGroup()) {
                        dates.forEachIndexed { index, day ->
                            val rain = rainfall[day]
                            val count = checkInCounts[day] ?: 0
                            val label = day.format(DateTimeFormatter.ofPattern("EEE"))
                            Box(
                                Modifier.width(dayWidth).heightIn(min = 76.dp)
                                    .background(
                                        if (index == selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainer,
                                        MaterialTheme.shapes.small
                                    )
                                    .selectable(selected == index, role = Role.Tab, onClick = { selected = index })
                                    .semantics {
                                        contentDescription = "$label ${day.dayOfMonth}, ${rain?.let(::formatRainfall) ?: "rain unavailable"}, $count check-ins"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.height(26.dp), contentAlignment = Alignment.BottomCenter) {
                                        if (rain != null && rain > 0) {
                                            Box(Modifier.width(8.dp).height((rain / maxRain * 26).toFloat().dp.coerceAtLeast(3.dp))
                                                .background(blue, MaterialTheme.shapes.extraSmall))
                                        } else Text(if (rain == null) "–" else "·", color = blue)
                                    }
                                    Text(label, style = MaterialTheme.typography.labelMedium)
                                    Text(if (count > 0) "●" else " ", style = MaterialTheme.typography.labelMedium, color = green)
                                }
                            }
                        }
                    }
                }
                Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Bar · Rainfall", style = MaterialTheme.typography.labelMedium, color = blue)
                    Text("Dot · Check-ins", style = MaterialTheme.typography.labelMedium, color = green)
                }
            }
        }
        val rainText = rainfall[date]?.let(::formatRainfall)
            ?: if (date == today) "Rain total pending" else "Rain unavailable"
        val count = checkInCounts[date] ?: 0
        Text(if (date == today) "Today" else date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
            style = MaterialTheme.typography.titleMedium)
        Text("$rainText · $count ${if (count == 1) "check-in" else "check-ins"}",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
