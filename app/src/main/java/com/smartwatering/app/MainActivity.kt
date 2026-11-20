package com.smartwatering.app

import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.mutableIntStateOf
import com.smartwatering.app.work.CareReminder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.smartwatering.app.ui.SmartWateringApp
import com.smartwatering.app.work.Scheduler

class MainActivity : ComponentActivity() {
    private val todayRequest = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CareReminder.createChannel(this)
        Scheduler.schedule(this)
        setContent { SmartWateringApp(todayRequest.intValue) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == CareReminder.OPEN_TODAY) todayRequest.intValue++
    }
}
