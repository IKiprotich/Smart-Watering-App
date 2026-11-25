package com.smartwatering.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.smartwatering.app.ui.components.SettingsRow

@Composable
internal fun ReminderSettingsRow() {
    val context = LocalContext.current
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    SettingsRow(
        Icons.Filled.Notifications,
        "Morning reminder",
        "Around 7:00 a.m. Nairobi time, when plants need water."
    ) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            })
        }
    }
}
