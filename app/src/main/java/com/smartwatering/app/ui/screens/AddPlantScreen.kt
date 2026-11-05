package com.smartwatering.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.smartwatering.app.model.Species
import com.smartwatering.app.ui.state.SmartWateringViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddPlantScreen(nav: NavHostController, vm: SmartWateringViewModel = viewModel()) {
    var name by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(Species.HERB.name) }
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
        Icon(Icons.Filled.LocalFlorist, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("Add a plant", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Text("We’ll use its type to shape a simple daily care plan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Text("Plant name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; error = null },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Kitchen basil") },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            isError = error != null,
            supportingText = { error?.let { Text(it) } }
        )
        Spacer(Modifier.height(22.dp))
        Text("Plant type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Species.entries.forEach { species ->
                FilterChip(
                    selected = selected == species.name,
                    onClick = { selected = species.name },
                    label = { Text(species.label) }
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                if (name.isBlank()) {
                    error = "Enter a name for your plant"
                    return@Button
                }
                scope.launch {
                    vm.addPlant(name, Species.valueOf(selected))
                        .onSuccess { nav.popBackStack() }
                        .onFailure { error = it.message ?: "Could not save this plant" }
                }
            },
            enabled = !state.isSavingPlant,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = MaterialTheme.shapes.large
        ) {
            if (state.isSavingPlant) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Add to my plants")
            }
        }
    }
}
