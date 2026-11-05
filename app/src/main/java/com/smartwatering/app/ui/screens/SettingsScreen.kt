package com.smartwatering.app.ui.screens

import android.net.Uri
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartwatering.app.ui.components.SettingsRow
import com.smartwatering.app.ui.components.SettingsSection
import com.smartwatering.app.ui.state.SmartWateringViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(darkMode: Boolean, onDarkModeChange: (Boolean) -> Unit, vm: SmartWateringViewModel = viewModel()) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var exporting by remember { mutableStateOf(false) }
    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) scope.launch {
            exporting = true
            runCatching { vm.exportCsv(uri) }
                .onSuccess { snackbar.showSnackbar("Care history exported") }
                .onFailure { snackbar.showSnackbar(it.message ?: "Export failed") }
            exporting = false
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { insets ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column {
                Text(
                    "Make it yours",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold
                )
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
                    Switch(checked = darkMode, onCheckedChange = onDarkModeChange)
                }
            }
            SettingsSection("Garden setup") {
                SettingsRow(Icons.Filled.LocationOn, "Garden location", "Nairobi, Kenya")
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingsRow(Icons.Filled.Cloud, "Weather data", "Open-Meteo forecast")
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingsRow(Icons.Filled.Notifications, "Daily care plan", "Runs automatically each day")
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
                "Smart Watering · MVP",
                Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
