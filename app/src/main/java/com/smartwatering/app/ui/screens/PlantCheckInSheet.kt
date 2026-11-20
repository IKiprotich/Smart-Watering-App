package com.smartwatering.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartwatering.app.data.CheckInType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlantCheckInSheet(onDismiss: () -> Unit, onSelect: (CheckInType) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            Text("How’s your plant doing?", Modifier.padding(20.dp), style = MaterialTheme.typography.titleLarge)
            CheckInType.entries.forEach { type ->
                ListItem(
                    headlineContent = { Text(type.label) },
                    modifier = Modifier.clickable { onSelect(type) }
                )
            }
        }
    }
}
