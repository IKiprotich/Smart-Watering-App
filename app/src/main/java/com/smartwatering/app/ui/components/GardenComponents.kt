package com.smartwatering.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Eco
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
    val icon = when (species) {
        Species.SUCCULENT -> Icons.Filled.Park
        Species.HERB -> Icons.Filled.Eco
        Species.FLOWERING -> Icons.Filled.LocalFlorist
        Species.FERN -> Icons.Filled.Grass
        Species.LEAFY_VEGETABLE -> Icons.Filled.Spa
    }
    Box(Modifier.size(size).background(MaterialTheme.colorScheme.primary.copy(alpha = .13f), CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size * .52f))
    }
}

@Composable
fun StatusPill(label: String, needsWater: Boolean) {
    val colors = MaterialTheme.colorScheme
    val bg = if (needsWater) colors.primaryContainer else colors.surfaceContainer
    val fg = if (needsWater) colors.onPrimaryContainer else colors.onSurfaceVariant
    Surface(color = bg, shape = CircleShape) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = fg, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, Modifier.padding(start = 4.dp, bottom = 8.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedCard(
            shape = MaterialTheme.shapes.large,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            content = content
        )
    }
}

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    val action = if (onClick == null) Modifier else Modifier.clickable(
        role = androidx.compose.ui.semantics.Role.Button,
        onClick = onClick
    )
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).then(action).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
