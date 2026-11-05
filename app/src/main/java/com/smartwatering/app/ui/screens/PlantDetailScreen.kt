package com.smartwatering.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.smartwatering.app.data.CheckInType
import com.smartwatering.app.ui.components.SpeciesAvatar
import com.smartwatering.app.ui.state.SmartWateringViewModel
import com.smartwatering.app.ui.util.relativeTime
import kotlinx.coroutines.launch

@Composable
fun PlantDetailScreen(id: Long?, nav: NavHostController, vm: SmartWateringViewModel = viewModel()) {
    if (id == null) { MissingPlant { nav.popBackStack() }; return }
    val plant by vm.plant(id).collectAsState(initial = null)
    val history by vm.history(id).collectAsState(initial = emptyList())
    val gardenState by vm.state.collectAsState()
    val decision = gardenState.decisions.firstOrNull { it.plantId == id }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { insets ->
        val currentPlant = plant
        if (currentPlant == null) {
            MissingPlant(Modifier.padding(insets)) { nav.popBackStack() }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(insets)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                IconButton(onClick = nav::popBackStack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to plants"
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SpeciesAvatar(currentPlant.species, 68.dp)
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(currentPlant.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                        Text(currentPlant.species.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(22.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text(
                            "TODAY’S CARE",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            decision?.reason ?: "Your care plan is updating",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    vm.logWatering(id).onSuccess { checkInId ->
                                        val result = snackbar.showSnackbar(
                                            message = "Watering recorded",
                                            actionLabel = "Undo"
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            vm.undoCheckIn(checkInId)
                                        }
                                    }
                                }
                            },
                            enabled = !gardenState.isLoggingWater,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.WaterDrop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("I watered this plant")
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text("Care history", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                if (history.isEmpty()) {
                    Text(
                        "Check-ins will show here as you care for this plant.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(history, key = { it.id }) { checkIn ->
                        ListItem(
                            headlineContent = { Text(checkIn.type.label) },
                            supportingContent = { Text(relativeTime(checkIn.timestamp)) },
                            leadingContent = {
                                Icon(
                                    Icons.Filled.WaterDrop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MissingPlant(modifier: Modifier = Modifier, onBack: () -> Unit) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Plant not found", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}
