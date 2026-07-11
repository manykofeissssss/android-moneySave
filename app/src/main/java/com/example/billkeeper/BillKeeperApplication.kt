package com.example.billkeeper

import android.app.Application
import androidx.room.Room
import com.example.billkeeper.data.local.db.AppDatabase
import com.example.billkeeper.data.repository.LedgerRepository

class BillKeeperApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "billkeeper.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    val repository: LedgerRepository by lazy {
        LedgerRepository(database)
    }
}
