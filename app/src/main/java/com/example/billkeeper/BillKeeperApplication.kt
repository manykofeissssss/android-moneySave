package com.example.billkeeper

import android.app.Application
import androidx.room.Room
import com.example.billkeeper.background.RecurringWorkScheduler
import com.example.billkeeper.data.local.db.AppDatabase
import com.example.billkeeper.data.repository.LedgerRepository
import com.example.billkeeper.notification.ReminderNotifications
import com.example.billkeeper.notification.ReminderScheduler

class BillKeeperApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.createChannel(this)
        ReminderScheduler(this).syncSchedules()
        recurringScheduler.startPeriodicChecks()
        recurringScheduler.enqueueImmediateCheck()
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
