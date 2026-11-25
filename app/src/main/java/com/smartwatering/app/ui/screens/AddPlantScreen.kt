package com.smartwatering.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.smartwatering.app.model.Species
import com.smartwatering.app.model.PlantInput
import com.smartwatering.app.ui.util.NairobiZone
import java.time.LocalDate
import com.smartwatering.app.ui.state.SmartWateringViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddPlantScreen(nav: NavHostController, vm: SmartWateringViewModel = viewModel()) {
    var name by rememberSaveable { mutableStateOf("") }
    var chosenType by rememberSaveable { mutableStateOf<Species?>(null) }
    val suggestedType = remember(name) { PlantInput.suggest(name) }
    val selected = chosenType ?: suggestedType
    var wateredDaysAgo by rememberSaveable { mutableStateOf<Int?>(null) }
    var typeError by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        IconButton(onClick = nav::popBackStack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        Spacer(Modifier.height(8.dp))
        Text("Add a plant", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Text("Pick the type that best matches your plant.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; error = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Plant name") },
            placeholder = { Text("e.g. Kitchen basil") },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            isError = error != null,
            supportingText = { error?.let { Text(it) } }
        )
        Spacer(Modifier.height(22.dp))
        Text("Plant type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        if (chosenType == null && suggestedType != null) {
            Text("Suggested: ${suggestedType.label}", color = MaterialTheme.colorScheme.primary)
        }
        PlantTypeOptions(selected) {
            chosenType = it
            typeError = false
        }
        if (typeError && selected == null) Text("Choose a plant type", color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(22.dp))
        LastWateredPicker(wateredDaysAgo) { wateredDaysAgo = it }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                val cleanName = PlantInput.validName(name)
                if (cleanName == null) {
                    error = "Enter a name for your plant"
                    return@Button
                }
                if (selected == null) {
                    typeError = true
                    return@Button
                }
                scope.launch {
                    val timestamp = wateredDaysAgo?.let { days ->
                        if (days == 0) System.currentTimeMillis()
                        else LocalDate.now(NairobiZone).minusDays(days.toLong())
                            .atStartOfDay(NairobiZone).toInstant().toEpochMilli()
                    }
                    vm.addPlant(cleanName, selected, timestamp)
                        .onSuccess { nav.popBackStack() }
                        .onFailure { error = "Couldn’t save your plant. Try again." }
                }
            },
            enabled = !state.isSavingPlant,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = MaterialTheme.shapes.large
        ) {
            if (state.isSavingPlant) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Save plant")
            }
        }
    }
}
