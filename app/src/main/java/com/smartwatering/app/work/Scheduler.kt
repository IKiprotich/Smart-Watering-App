package com.smartwatering.app.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object Scheduler {
    fun runNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<DailyDecisionWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "decision-now",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<DailyDecisionWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily-decision",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
