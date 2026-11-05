package com.smartwatering.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smartwatering.app.model.Species

@Composable
fun EmptyPanel(title: String, message: String, action: String, onAction: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp)) {
            Icon(Icons.Filled.LocalFlorist, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
fun SpeciesAvatar(species: Species, size: Dp = 44.dp) {
    Box(Modifier.size(size).background(MaterialTheme.colorScheme.primary.copy(alpha = .13f), CircleShape), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.LocalFlorist, contentDescription = species.label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size * .52f))
    }
}

@Composable
fun StatusPill(label: String, needsWater: Boolean) {
    val bg = if (needsWater) MaterialTheme.colorScheme.secondary.copy(alpha = .22f) else MaterialTheme.colorScheme.primary.copy(alpha = .12f)
    val fg = if (needsWater) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
    Surface(color = bg, shape = CircleShape) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = fg, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, Modifier.padding(start = 4.dp, bottom = 8.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), content = content)
    }
}

@Composable
fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.padding(start = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
