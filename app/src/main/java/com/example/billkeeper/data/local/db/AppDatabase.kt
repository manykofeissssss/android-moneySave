package com.example.billkeeper

import androidx.room.*

@Database(entities = [BillItem::class, IncomeItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun billDao(): BillDao
    abstract fun incomeDao(): IncomeDao
}
