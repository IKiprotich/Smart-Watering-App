package com.smartwatering.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun GardenProfile(name: String, plants: Int, checkIns: Int, onSave: (String) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf(name) }

    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                        if (name.isBlank()) {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        } else {
                            Text(name.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(name.ifBlank { "Name your garden" }, style = MaterialTheme.typography.titleLarge)
                    Text("Garden name", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { draft = name; editing = true }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit garden name")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProfileStat(plants, if (plants == 1) "Plant" else "Plants", Modifier.weight(1f))
                ProfileStat(checkIns, if (checkIns == 1) "Check-in" else "Check-ins", Modifier.weight(1f))
            }
        }
    }

    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Edit garden name") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(48) },
                    label = { Text("Garden name") },
                    supportingText = { Text("Shown on Today. Leave blank to clear it.") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { onSave(draft.trim()); editing = false }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ProfileStat(value: Int, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value.toString(), style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
