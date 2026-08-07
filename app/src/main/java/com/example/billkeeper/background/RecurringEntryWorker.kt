package com.example.billkeeper.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.billkeeper.BillKeeperApplication
import com.example.billkeeper.domain.recurring.ProcessDueRecurringEntries
import java.util.concurrent.TimeUnit

interface RecurringEntryScheduler {
    fun enqueueImmediateCheck()
}

class RecurringWorkScheduler(context: Context) : RecurringEntryScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun startPeriodicChecks() {
        val request = PeriodicWorkRequestBuilder<RecurringEntryWorker>(
            PERIODIC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        ).build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override fun enqueueImmediateCheck() {
        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RecurringEntryWorker>().build()
        )
    }

    private companion object {
        const val PERIODIC_WORK_NAME = "recurring-entry-periodic-check"
        const val IMMEDIATE_WORK_NAME = "recurring-entry-immediate-check"
        const val PERIODIC_INTERVAL_MINUTES = 15L
    }
}

class RecurringEntryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = try {
        val application = applicationContext as BillKeeperApplication
        ProcessDueRecurringEntries(application.repository)()
        Result.success()
    } catch (error: Exception) {
        if (runAttemptCount < MAX_RETRY_COUNT) Result.retry() else Result.failure()
    }

    private companion object {
        const val MAX_RETRY_COUNT = 3
    }
}
