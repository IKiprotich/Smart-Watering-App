package com.smartwatering.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartwatering.app.model.DailyWeather
import com.smartwatering.app.model.DecisionTree
import com.smartwatering.app.model.FeatureBuilder
import com.smartwatering.app.model.ReasonLine
import com.smartwatering.app.model.Species
import com.smartwatering.app.model.formatRainfall
import com.smartwatering.app.ui.SmartWateringTheme
import com.smartwatering.app.ui.components.SpeciesAvatar
import com.smartwatering.app.ui.components.StatusPill

class WateringReplayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val weather = DailyWeather.fromOpenMeteo(readAsset("fern_replay_weather.json"))
        val model = DecisionTree(readAsset("decision_tree.json"))
        val darkMode = getSharedPreferences("garden_preferences", MODE_PRIVATE)
            .let { if (it.contains("dark_mode")) it.getBoolean("dark_mode", false) else null }
        setContent {
            SmartWateringTheme(darkMode) {
                ReplayScreen(weather, model, ::finish)
            }
        }
    }

    private fun readAsset(name: String) = assets.open(name).bufferedReader().use { it.readText() }
}

@Composable
private fun ReplayScreen(weather: DailyWeather, model: DecisionTree, goBack: () -> Unit) {
    var days by rememberSaveable { mutableIntStateOf(3) }
    val prediction = remember(days) {
        if (days == 0) null else {
            val inputs = FeatureBuilder.build(weather, Species.FERN, days)
            require(FeatureBuilder.acceptsForPrediction(inputs))
            model.predict(inputs)
        }
    }
    val yesterday = weather.dates.lastIndex - 1
    val rain = formatRainfall(weather.rain[yesterday])
    val reason = prediction?.let(ReasonLine::of) ?: "Watered today. No further watering is due."
    Scaffold { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = goBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to live garden")
            }
            Text("Nairobi · 16 November 2026", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            Text("Watering replay", style = MaterialTheme.typography.headlineLarge)
            Text("Past weather with a sample watering history. Your garden records stay as they are.",
                style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SpeciesAvatar(Species.FERN)
                Column {
                    Text("Boston fern", style = MaterialTheme.typography.titleLarge)
                    Text("Rain the previous day: $rain", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("When was it last watered?", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = days == 1, onClick = { days = 1 }, label = { Text("1 day ago") })
                FilterChip(selected = days == 3, onClick = { days = 3 }, label = { Text("3 days ago") })
            }
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(if (prediction?.water == true) "Water" else "Wait", prediction?.water == true)
                    Text(reason, style = MaterialTheme.typography.titleMedium)
                }
            }
            Button(
                onClick = { days = 0 },
                enabled = days != 0,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("Mark as watered in replay") }
        }
    }
}
