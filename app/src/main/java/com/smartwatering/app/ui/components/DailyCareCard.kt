package com.smartwatering.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.smartwatering.app.ui.state.GardenUiState

private val LakeInk = Color(0xFF162F38)
private val Chalk = Color(0xFFF1F5EE)
private val Citron = Color(0xFFD8E8A6)

@Composable
internal fun DailyCareCard(state: GardenUiState, onAdd: () -> Unit) {
    val decisions = state.decisions.associateBy { it.plantId }
    val due = state.plants.count { decisions[it.id]?.water == true }
    val pending = state.plants.count { decisions[it.id] == null }
    val headline = when {
        !state.plantsLoaded -> "Loading your garden"
        state.plants.isEmpty() -> "Grow your garden"
        due == 1 -> "One plant needs water"
        due > 1 -> "Water $due plants today"
        pending > 0 -> "Checking your garden"
        else -> "No watering due today"
    }
    val detail = when {
        !state.plantsLoaded -> null
        state.plants.isEmpty() -> "Add a plant to get your first watering recommendation."
        pending > 0 && state.refreshMessage != null -> "Today’s advice is unavailable for some plants."
        pending > 0 -> "Waiting on advice for $pending ${if (pending == 1) "plant" else "plants"}."
        else -> null
    }

    Surface(
        color = LakeInk,
        contentColor = Chalk,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Watering plan", style = MaterialTheme.typography.labelLarge, color = Citron)
                    Text(headline, style = MaterialTheme.typography.headlineLarge)
                }
                RippleMark(Modifier.size(82.dp).padding(start = 8.dp))
            }
            if (state.plants.isNotEmpty()) {
                Text("$due of ${state.plants.size} ${if (state.plants.size == 1) "plant needs" else "plants need"} water",
                    style = MaterialTheme.typography.bodyMedium, color = Citron)
            }
            detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (state.plantsLoaded && state.plants.isEmpty()) {
                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(containerColor = Citron, contentColor = LakeInk)
                ) { Text("Add a plant") }
            }
        }
    }
}

@Composable
private fun RippleMark(modifier: Modifier) {
    Canvas(modifier.clip(MaterialTheme.shapes.medium)) {
        val line = 1.dp.toPx()
        val w = size.width
        val h = size.height
        repeat(4) { ring ->
            val spread = w * (.25f + .22f * ring)
            val height = spread * .34f
            drawOval(
                color = Citron.copy(alpha = .65f - .13f * ring),
                topLeft = Offset((w - spread) / 2f, h * .75f - height / 2f),
                size = Size(spread, height),
                style = Stroke(line)
            )
        }
        val drop = Path().apply {
            moveTo(w * .5f, h * .11f)
            cubicTo(w * .44f, h * .23f, w * .32f, h * .35f, w * .32f, h * .46f)
            cubicTo(w * .32f, h * .66f, w * .68f, h * .66f, w * .68f, h * .46f)
            cubicTo(w * .68f, h * .35f, w * .56f, h * .23f, w * .5f, h * .11f)
            close()
        }
        drawPath(drop, Citron)
    }
}
