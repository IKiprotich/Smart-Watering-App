package com.smartwatering.app.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import java.util.concurrent.TimeUnit

object Scheduler {
    fun runNow(context: Context): Flow<WorkInfo> {
        val request = OneTimeWorkRequestBuilder<DailyDecisionWorker>().build()
        val manager = WorkManager.getInstance(context)
        manager.enqueueUniqueWork(
            "decision-now",
            ExistingWorkPolicy.REPLACE,
            request
        )
        return manager.getWorkInfoByIdFlow(request.id).filterNotNull()
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
