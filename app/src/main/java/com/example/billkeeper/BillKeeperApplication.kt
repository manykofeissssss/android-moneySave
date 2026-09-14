package com.example.billkeeper

import android.app.Application
import androidx.room.Room
import com.example.billkeeper.background.RecurringWorkScheduler
import com.example.billkeeper.data.local.db.AppDatabase
import com.example.billkeeper.data.repository.LedgerRepository
import com.example.billkeeper.diagnostics.BillKeeperDiagnosticReporter
import com.example.billkeeper.diagnostics.SupabaseDiagnosticApi
import com.example.billkeeper.diagnostics.SupabaseDiagnosticApiProvider
import com.example.billkeeper.notification.ReminderNotifications
import com.example.billkeeper.notification.ReminderScheduler
import io.github.manykofeissssss.kdiagnostics.android.runtime.Diagnostics
import io.github.manykofeissssss.kdiagnostics.android.runtime.DiagnosticsConfig
import io.github.manykofeissssss.kdiagnostics.android.runtime.DiagnosticsHandle
import io.github.manykofeissssss.kdiagnostics.core.api.DiagnosticReporter
import io.github.manykofeissssss.kdiagnostics.core.model.DiagnosticEventType
import io.github.manykofeissssss.kdiagnostics.core.model.DiagnosticStatus
import io.github.manykofeissssss.kdiagnostics.work.DiagnosticUploadRegistry
import io.github.manykofeissssss.kdiagnostics.work.DiagnosticUploadScheduler

class BillKeeperApplication : Application() {
    val diagnostics: DiagnosticsHandle by lazy {
        Diagnostics.init(
            context = this,
            config = DiagnosticsConfig(
                enableAnrWatchdog = true,
                enableUiBlockMonitor = BuildConfig.DEBUG,
                anrTimeoutMillis = 5_000L,
                anrForegroundGraceMillis = 5_000L,
                uiBlockThresholdMillis = 500L,
                // BK keeps its custom splash visible for three seconds; allow
                // the first business screen to settle before measuring stalls.
                uiBlockForegroundGraceMillis = 5_000L,
                uiBlockCooldownMillis = 60_000L,
                uiBlockMaxEventsPerSession = 5,
                debuggerAware = true
            )
        )
    }

    /** BK owns the backend adapter; k-diagnostics remains backend-agnostic. */
    private val supabaseDiagnosticApi: SupabaseDiagnosticApi =
        SupabaseDiagnosticApiProvider.fromBuildConfig()

    val diagnosticReporter: DiagnosticReporter by lazy {
        BillKeeperDiagnosticReporter(supabaseDiagnosticApi)
    }

    override fun onCreate() {
        super.onCreate()
        DiagnosticUploadRegistry.configure(diagnostics.store, diagnosticReporter)
        enqueuePendingNonCrashDiagnostics()
        ReminderNotifications.createChannel(this)
        ReminderScheduler(this).syncSchedules()
        recurringScheduler.startPeriodicChecks()
        recurringScheduler.enqueueImmediateCheck()
    }

    fun enqueueDiagnosticUpload(includeCrash: Boolean = false) {
        DiagnosticUploadScheduler.enqueue(
            context = this,
            includeCrash = includeCrash,
            // A crash consent must upgrade an already queued non-crash upload.
            // Replacing the same unique work also prevents two workers from
            // uploading the same non-crash batch concurrently.
            replaceExisting = includeCrash
        )
    }

    private fun enqueuePendingNonCrashDiagnostics() {
        val hasPendingNonCrash = diagnostics.store
            .list(DiagnosticStatus.PENDING)
            .any { it.type != DiagnosticEventType.CRASH }
        if (hasPendingNonCrash) enqueueDiagnosticUpload()
    }

    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "billkeeper.db")
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5
            )
            .build()
    }

    val repository: LedgerRepository by lazy {
        LedgerRepository(database)
    }

    val recurringScheduler: RecurringWorkScheduler by lazy {
        RecurringWorkScheduler(this)
    }
}
