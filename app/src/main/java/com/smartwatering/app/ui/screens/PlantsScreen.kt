package com.smartwatering.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.smartwatering.app.data.Plant
import com.smartwatering.app.ui.components.EmptyPanel
import com.smartwatering.app.ui.components.SpeciesAvatar
import com.smartwatering.app.ui.components.StatusPill
import com.smartwatering.app.ui.plants.PlantFilter
import com.smartwatering.app.ui.state.GardenUiState
import com.smartwatering.app.ui.state.SmartWateringViewModel
import com.smartwatering.app.ui.util.relativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantsScreen(nav: NavHostController, vm: SmartWateringViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable { mutableStateOf(PlantFilter.ALL.name) }
    var pendingDelete by remember { mutableStateOf<Plant?>(null) }
    val filter = PlantFilter.valueOf(filterName)
    val visiblePlants = state.plants.filter { plant ->
        plant.matches(query, filter, state)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        PlantsHeader(onAdd = { nav.navigate("add") })
        Spacer(Modifier.height(14.dp))
        PlantSearchField(query, onQueryChange = { query = it })
        Spacer(Modifier.height(10.dp))
        PlantFilters(filter, onFilterSelected = { filterName = it.name })
        Spacer(Modifier.height(8.dp))
        PlantListContent(
            state = state,
            plants = visiblePlants,
            onOpen = { nav.navigate("plant/${it.id}") },
            onAdd = { nav.navigate("add") },
            onDelete = { pendingDelete = it },
            onClearFilters = {
                query = ""
                filterName = PlantFilter.ALL.name
            }
        )
    }

    pendingDelete?.let { plant ->
        RemovePlantDialog(
            plant = plant,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                vm.removePlant(plant.id)
                pendingDelete = null
            }
        )
    }
}

@Composable
private fun PlantsHeader(onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                "Your collection",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Plants",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        FilledTonalIconButton(
            onClick = onAdd,
            modifier = Modifier.semantics { contentDescription = "Add a plant" }
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
        }
    }
}

@Composable
private fun PlantSearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search your plants") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large
    )
}

@Composable
private fun PlantFilters(selected: PlantFilter, onFilterSelected: (PlantFilter) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(PlantFilter.entries) { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) }
            )
        }
    }
}

@Composable
private fun PlantListContent(
    state: GardenUiState,
    plants: List<Plant>,
    onOpen: (Plant) -> Unit,
    onAdd: () -> Unit,
    onDelete: (Plant) -> Unit,
    onClearFilters: () -> Unit
) {
    when {
        state.plants.isEmpty() -> EmptyPanel(
            "A little room to grow",
            "Add your first plant and its daily care plan will appear here.",
            "Add plant",
            onAdd
        )

        plants.isEmpty() -> EmptyPanel(
            "No plants found",
            "Try a different search or filter.",
            "Clear filters",
            onClearFilters
        )

        else -> LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(plants, key = { it.id }) { plant ->
                PlantListItem(
                    plant = plant,
                    state = state,
                    onOpen = { onOpen(plant) },
                    onDelete = { onDelete(plant) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantListItem(
    plant: Plant,
    state: GardenUiState,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val decision = state.decisions.firstOrNull { it.plantId == plant.id }
    val lastCheckIn = state.checkIns
        .filter { it.plantId == plant.id }
        .maxByOrNull { it.timestamp }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart ||
                value == SwipeToDismissBoxValue.StartToEnd
            ) {
                onDelete()
                false
            } else {
                true
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.shapes.large
                    ),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete ${plant.name}",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        Card(
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpeciesAvatar(plant.species, 52.dp)
                Spacer(Modifier.width(14.dp))
                PlantSummary(plant, lastCheckIn, Modifier.weight(1f))
                StatusPill(
                    label = when {
                        decision?.water == true -> "Water"
                        decision == null -> "Checking"
                        else -> "Doing well"
                    },
                    needsWater = decision?.water == true
                )
            }
        }
    }
}

@Composable
private fun PlantSummary(plant: Plant, lastCheckIn: com.smartwatering.app.data.CheckIn?, modifier: Modifier) {
    Column(modifier) {
        Text(plant.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            plant.species.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text(
            lastCheckIn?.let { "${it.type.label} · ${relativeTime(it.timestamp)}" }
                ?: "No check-ins yet",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RemovePlantDialog(plant: Plant, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove ${plant.name}?") },
        text = { Text("Its care history and watering decisions will also be removed.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Remove", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun Plant.matches(query: String, filter: PlantFilter, state: GardenUiState): Boolean {
    val normalizedQuery = query.trim()
    val matchesQuery = name.contains(normalizedQuery, ignoreCase = true) ||
        species.label.contains(normalizedQuery, ignoreCase = true)
    val needsWater = state.decisions.any { it.plantId == id && it.water }
    val recentlyWatered = state.checkIns
        .asSequence()
        .filter { it.plantId == id }
        .maxOfOrNull { it.timestamp }
        ?.let { System.currentTimeMillis() - it < WEEK_MS }
        ?: false

    return matchesQuery && when (filter) {
        PlantFilter.ALL -> true
        PlantFilter.NEEDS_WATER -> needsWater
        PlantFilter.DOING_WELL -> !needsWater
        PlantFilter.RECENTLY_ADDED -> recentlyWatered
    }
}

private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
