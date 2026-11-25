package com.smartwatering.app.ui.screens

import android.net.Uri
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartwatering.app.ui.components.SettingsRow
import com.smartwatering.app.ui.components.SettingsSection
import com.smartwatering.app.ui.state.SmartWateringViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    gardenName: String,
    onGardenNameChange: (String) -> Unit,
    vm: SmartWateringViewModel = viewModel()
) {
    val context = LocalContext.current
    val replay = remember(context) {
        Intent().setClassName(context.packageName, "com.smartwatering.app.WateringReplayActivity")
            .takeIf { it.resolveActivity(context.packageManager) != null }
    }
    val garden by vm.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var exporting by remember { mutableStateOf(false) }
    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) scope.launch {
            exporting = true
            runCatching { vm.exportCsv(uri) }
                .onSuccess { snackbar.showSnackbar("Care history exported") }
                .onFailure { snackbar.showSnackbar("Couldn’t export care history. Try again.") }
            exporting = false
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { insets ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column {
                Text(
                    "Profile",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            GardenProfile(gardenName, garden.plants.size, garden.checkIns.size, onGardenNameChange)
            replay?.let { intent ->
                SettingsSection("Presentation") {
                    SettingsRow(Icons.Filled.LocalFlorist, "Watering replay", "A past Nairobi weather example") {
                        context.startActivity(intent)
                    }
                }
            }
            SettingsSection("Preferences") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text("Dark appearance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                        Text(
                            "Use a darker color palette",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = darkMode,
                        onCheckedChange = onDarkModeChange,
                        modifier = Modifier.semantics { contentDescription = "Dark appearance" }
                    )
                }
            }
            SettingsSection("Garden setup") {
                SettingsRow(Icons.Filled.LocationOn, "Garden location", "Nairobi, Kenya. Advice is tuned for Nairobi weather.")
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingsRow(Icons.Filled.Cloud, "Open-Meteo forecast", "View the weather source") {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/")))
                    }.onFailure { scope.launch { snackbar.showSnackbar("Couldn’t open Open-Meteo.") } }
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                ReminderSettingsRow()
            }
            SettingsSection("About") {
                SettingsRow(Icons.Filled.Cloud, "Weather data by Open-Meteo.com", "Nairobi daily forecast and rainfall")
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingsRow(Icons.Filled.LocalFlorist, "Watering model", "Runs on your phone. Trained with Nairobi weather from 2015–2025 and simulated watering labels.")
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingsRow(Icons.Filled.Lock, "Privacy", "No account required. Weather requests use a fixed Nairobi location. You choose where to export your records.")
            }
            SettingsSection("Your data") {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(
                                "Export care history",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Save decisions and check-ins as CSV",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { createCsv.launch("smart-watering-history.csv") },
                        enabled = !exporting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (exporting) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Export CSV")
                        }
                    }
                }
            }
            Text(
                "Smart Watering",
                Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
