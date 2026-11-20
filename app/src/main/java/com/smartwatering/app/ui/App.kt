package com.smartwatering.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartwatering.app.ui.state.SmartWateringViewModel
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.smartwatering.app.ui.screens.*

@Composable
fun SmartWateringApp() {
    var darkMode by rememberSaveable { mutableStateOf(false) }
    val view = LocalView.current
    val activity = view.context as? android.app.Activity
    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkMode
        }
    }
    val garden: SmartWateringViewModel = viewModel()
    SmartWateringTheme(darkTheme = darkMode) {
        val nav = rememberNavController()
        val route = nav.currentBackStackEntryAsState().value?.destination?.route
        val tabs = listOf(
            AppTab("Today", "today", Icons.Filled.WaterDrop, Icons.Outlined.WaterDrop),
            AppTab("Plants", "plants", Icons.Filled.LocalFlorist, Icons.Outlined.LocalFlorist),
            AppTab("Settings", "settings", Icons.Filled.Settings, Icons.Outlined.Settings)
        )
        Scaffold(bottomBar = {
            if (tabs.any { it.route == route }) NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp, windowInsets = WindowInsets.navigationBars) {
                tabs.forEach { tab ->
                    val selected = route == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) nav.navigate(tab.route) {
                                popUpTo("today") { saveState = true }
                                restoreState = true
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = .13f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }) { padding ->
            NavHost(navController = nav, startDestination = "today", modifier = Modifier.padding(padding)) {
                composable("today") { TodayScreen(nav, garden) }
                composable("plants") { PlantsScreen(nav, garden) }
                composable("add") { AddPlantScreen(nav, garden) }
                composable("settings") {
                    SettingsScreen(darkMode = darkMode, onDarkModeChange = { darkMode = it }, vm = garden)
                }
                composable("plant/{id}") { entry ->
                    PlantDetailScreen(entry.arguments?.getString("id")?.toLongOrNull(), nav, garden)
                }
            }
        }
    }
}

private data class AppTab(
    val label: String,
    val route: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
