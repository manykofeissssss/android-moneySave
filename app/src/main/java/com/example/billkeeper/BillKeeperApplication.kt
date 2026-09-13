package com.example.billkeeper

import android.app.Application
import androidx.room.Room
import com.example.billkeeper.background.RecurringWorkScheduler
import com.example.billkeeper.data.local.db.AppDatabase
import com.example.billkeeper.data.repository.LedgerRepository
import com.example.billkeeper.diagnostics.BillKeeperDiagnosticReporter
import com.example.billkeeper.diagnostics.SupabaseDiagnosticApi
import com.example.billkeeper.diagnostics.UnconfiguredSupabaseDiagnosticApi
import com.example.billkeeper.notification.ReminderNotifications
import com.example.billkeeper.notification.ReminderScheduler
import io.github.manykofeissssss.kdiagnostics.android.runtime.Diagnostics
import io.github.manykofeissssss.kdiagnostics.android.runtime.DiagnosticsConfig
import io.github.manykofeissssss.kdiagnostics.android.runtime.DiagnosticsHandle
import io.github.manykofeissssss.kdiagnostics.core.api.DiagnosticReporter
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
                uiBlockThresholdMillis = 250L,
                debuggerAware = true
            )
        )
    }

    /** Replace only this API with the real Supabase adapter when it is ready. */
    private val supabaseDiagnosticApi: SupabaseDiagnosticApi =
        UnconfiguredSupabaseDiagnosticApi

    val diagnosticReporter: DiagnosticReporter by lazy {
        BillKeeperDiagnosticReporter(supabaseDiagnosticApi)
    }

    override fun onCreate() {
        super.onCreate()
        DiagnosticUploadRegistry.configure(diagnostics.store, diagnosticReporter)
        ReminderNotifications.createChannel(this)
        ReminderScheduler(this).syncSchedules()
        recurringScheduler.startPeriodicChecks()
        recurringScheduler.enqueueImmediateCheck()
    }

    fun enqueueDiagnosticUpload() {
        DiagnosticUploadScheduler.enqueue(this)
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
