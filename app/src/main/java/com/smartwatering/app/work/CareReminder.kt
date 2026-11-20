package com.smartwatering.app.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.smartwatering.app.MainActivity

object CareReminder {
    const val CHANNEL = "garden_care"
    const val OPEN_TODAY = "com.smartwatering.app.OPEN_TODAY"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, "Morning watering reminders", NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun post(context: Context, due: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        createChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return
        val intent = Intent(context, MainActivity::class.java)
            .setAction(OPEN_TODAY)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val openToday = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val message = if (due == 1) "One plant needs water today." else "$due plants need water today."
        manager.notify(
            1,
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Smart Watering")
                .setContentText(message)
                .setContentIntent(openToday)
                .setAutoCancel(true)
                .build()
        )
    }
}
