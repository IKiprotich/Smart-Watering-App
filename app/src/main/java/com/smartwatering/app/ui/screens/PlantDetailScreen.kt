package com.smartwatering.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.smartwatering.app.data.CheckInType
import com.smartwatering.app.model.ReasonLine
import com.smartwatering.app.ui.components.SpeciesAvatar
import com.smartwatering.app.ui.state.SmartWateringViewModel
import com.smartwatering.app.ui.util.relativeTime
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch

@Composable
fun PlantDetailScreen(id: Long?, nav: NavHostController, vm: SmartWateringViewModel = viewModel()) {
    if (id == null) { MissingPlant { nav.popBackStack() }; return }
    var retry by remember { mutableIntStateOf(0) }
    val plantResult by remember(id, retry) {
        vm.plant(id).map { Result.success(it) }.catch { emit(Result.failure(it)) }
    }.collectAsState(initial = null)
    val plant = plantResult?.getOrNull()
    val history by remember(id) { vm.history(id) }.collectAsState(initial = emptyList())
    val gardenState by vm.state.collectAsState()
    val decision = gardenState.decisions.firstOrNull { it.plantId == id }
    val previous = gardenState.yesterdayDecisions.firstOrNull { it.plantId == id }
        ?.takeIf { gardenState.refreshMessage != null }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var editingType by remember { mutableStateOf(false) }
    var showCheckIns by remember { mutableStateOf(false) }
    fun saveCheckIn(type: CheckInType) {
        scope.launch {
            vm.logCheckIn(id, type).onSuccess { checkInId ->
                val message = if (type == CheckInType.WATERED) "Watering recorded" else "Check-in saved"
                if (snackbar.showSnackbar(message, "Undo", duration = SnackbarDuration.Short) ==
                    SnackbarResult.ActionPerformed
                ) {
                    vm.undoCheckIn(checkInId)
                }
            }.onFailure { snackbar.showSnackbar("Couldn’t save the check-in. Try again.") }
        }
    }

    if (showCheckIns) {
        PlantCheckInSheet(onDismiss = { showCheckIns = false }) { type ->
            showCheckIns = false
            saveCheckIn(type)
        }
    }

    if (editingType && plant != null) {
        var draft by remember { mutableStateOf(plant?.species) }
        AlertDialog(
            onDismissRequest = { editingType = false },
            title = { Text("Change plant type") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    PlantTypeOptions(draft) { draft = it }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val type = draft ?: return@TextButton
                    editingType = false
                    scope.launch {
                        vm.updateSpecies(id, type)
                            .onSuccess { snackbar.showSnackbar("Plant type updated") }
                            .onFailure { snackbar.showSnackbar("Couldn’t update the plant type.") }
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingType = false }) { Text("Cancel") } }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { insets ->
        val currentPlant = plant
        if (currentPlant == null) {
            if (plantResult == null) {
                Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (plantResult?.isFailure == true) {
                Column(Modifier.padding(insets).padding(20.dp)) {
                    com.smartwatering.app.ui.components.EmptyPanel(
                        "Can’t open this plant", "Try loading the plant again.", "Retry"
                    ) { retry++ }
                    TextButton(onClick = { nav.popBackStack() }) { Text("Back") }
                }
            } else {
                MissingPlant(Modifier.padding(insets)) { nav.popBackStack() }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(insets),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Column {
                        IconButton(onClick = nav::popBackStack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to plants"
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SpeciesAvatar(currentPlant.species, 68.dp)
                            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                                Text(currentPlant.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                                Text(currentPlant.species.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { editingType = true }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Change plant type")
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
                                    when {
                                        decision?.reason == "Watered today" -> "Today’s care"
                                        decision?.water == true -> "Water today"
                                        decision != null -> "Wait today"
                                        previous != null -> "Yesterday’s advice"
                                        else -> "Today’s care"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    when {
                                        decision != null -> ReasonLine.display(decision.reason)
                                        previous != null -> "${if (previous.water) "Water" else "Wait"}. ${ReasonLine.display(previous.reason)}"
                                        else -> "Waiting for today’s care plan"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(14.dp))
                                Button(
                                    onClick = { saveCheckIn(CheckInType.WATERED) },
                                    enabled = !gardenState.isLoggingWater,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.WaterDrop, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Log watering")
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { showCheckIns = true },
                            enabled = !gardenState.isLoggingWater,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        ) { Text("Log a check-in") }
                        Spacer(Modifier.height(22.dp))
                        Text("Care history", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        if (history.isEmpty()) {
                            Text(
                                "Check-ins will show here as you care for this plant.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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

@Composable
private fun MissingPlant(modifier: Modifier = Modifier, onBack: () -> Unit) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Plant not found", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}
