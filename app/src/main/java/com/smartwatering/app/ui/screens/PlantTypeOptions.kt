package com.smartwatering.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.smartwatering.app.model.PlantInput
import com.smartwatering.app.model.Species

@Composable
internal fun PlantTypeOptions(selected: Species?, onSelect: (Species) -> Unit) {
    Column(Modifier.selectableGroup()) {
        Species.entries.forEach { type ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .selectable(selected == type, role = Role.RadioButton, onClick = { onSelect(type) })
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected == type, onClick = null, modifier = Modifier.padding(horizontal = 12.dp))
                Column {
                    Text(type.label, style = MaterialTheme.typography.titleSmall)
                    Text(PlantInput.examplesFor(type), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
