package com.smartwatering.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.smartwatering.app.ui.util.NairobiZone
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LastWateredPicker(daysAgo: Int?, onSelect: (Int?) -> Unit) {
    val context = LocalContext.current
    Text("When did you last water it?", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((days, label) in listOf(0 to "Today", 1 to "Yesterday", 2 to "2 days ago", null to "Not sure")) {
            FilterChip(
                selected = daysAgo == days,
                onClick = { onSelect(days) },
                label = { Text(label) }
            )
        }
        FilterChip(
            selected = daysAgo != null && daysAgo >= 3,
            onClick = {
                val today = LocalDate.now(NairobiZone)
                val initial = today.minusDays(daysAgo?.toLong() ?: 3)
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val date = LocalDate.of(year, month + 1, day)
                        onSelect(ChronoUnit.DAYS.between(date, today).toInt().coerceAtLeast(0))
                    },
                    initial.year, initial.monthValue - 1, initial.dayOfMonth
                ).apply {
                    datePicker.maxDate = System.currentTimeMillis()
                }.show()
            },
            label = { Text(if (daysAgo != null && daysAgo >= 3) "$daysAgo days ago" else "Choose date") }
        )
    }
}
