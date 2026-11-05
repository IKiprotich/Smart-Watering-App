package com.smartwatering.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.smartwatering.app.ui.SmartWateringApp
import com.smartwatering.app.work.Scheduler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Scheduler.schedule(this)
        setContent { SmartWateringApp() }
    }
}
