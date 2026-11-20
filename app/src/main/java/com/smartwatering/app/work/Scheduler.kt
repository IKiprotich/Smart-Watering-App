package com.smartwatering.app.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkInfo
import androidx.work.workDataOf
import java.time.Duration
import java.time.ZonedDateTime
import com.smartwatering.app.ui.util.NairobiZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import java.util.concurrent.TimeUnit

object Scheduler {
    const val MORNING_RUN = "morning_run"
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
        val now = ZonedDateTime.now(NairobiZone)
        val todayAtSeven = now.toLocalDate().atTime(7, 0).atZone(NairobiZone)
        val next = if (todayAtSeven.isAfter(now)) todayAtSeven else todayAtSeven.plusDays(1)
        val request = PeriodicWorkRequestBuilder<DailyDecisionWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(MORNING_RUN to true))
            .build()
        val manager = WorkManager.getInstance(context)
        manager.cancelUniqueWork("daily-decision")
        manager.enqueueUniquePeriodicWork(
            "morning-care",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
