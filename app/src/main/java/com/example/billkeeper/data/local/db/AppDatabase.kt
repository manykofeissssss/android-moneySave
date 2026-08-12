package com.example.billkeeper.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.billkeeper.data.local.dao.BillDao
import com.example.billkeeper.data.local.dao.IncomeDao
import com.example.billkeeper.data.local.dao.MonthlyBudgetDao
import com.example.billkeeper.data.local.dao.RecurringEntryDao
import com.example.billkeeper.data.local.entity.BillItem
import com.example.billkeeper.data.local.entity.IncomeItem
import com.example.billkeeper.data.local.entity.MonthlyBudget
import com.example.billkeeper.data.local.entity.RecurringEntry

@Database(
    entities = [
        BillItem::class,
        IncomeItem::class,
        MonthlyBudget::class,
        RecurringEntry::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(RecurringEntryConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun billDao(): BillDao
    abstract fun incomeDao(): IncomeDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
    abstract fun recurringEntryDao(): RecurringEntryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE bills_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        category TEXT NOT NULL,
                        amountCents INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        note TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO bills_new (id, category, amountCents, date, note)
                    SELECT id, category, CAST(ROUND(amount * 100) AS INTEGER), date, note FROM bills
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE bills")
                db.execSQL("ALTER TABLE bills_new RENAME TO bills")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bills_date ON bills(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bills_category ON bills(category)")

                db.execSQL(
                    """
                    CREATE TABLE incomes_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        source TEXT NOT NULL,
                        amountCents INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        note TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO incomes_new (id, source, amountCents, date, note)
                    SELECT id, source, CAST(ROUND(amount * 100) AS INTEGER), date, note FROM incomes
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE incomes")
                db.execSQL("ALTER TABLE incomes_new RENAME TO incomes")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_incomes_date ON incomes(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_incomes_source ON incomes(source)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS monthly_budgets (
                year INTEGER NOT NULL,
                month INTEGER NOT NULL,
                category TEXT NOT NULL,
                limitCents INTEGER NOT NULL,
                PRIMARY KEY(year, month, category)
            )
            """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recurring_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entryType TEXT NOT NULL,
                        categoryOrSource TEXT NOT NULL,
                        amountCents INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        frequency TEXT NOT NULL,
                        dayOfWeek INTEGER,
                        dayOfMonth INTEGER,
                        nextRunAt INTEGER NOT NULL,
                        lastExecutedAt INTEGER,
                        enabled INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_recurring_entries_enabled_nextRunAt
                    ON recurring_entries(enabled, nextRunAt)
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE recurring_entries " +
                            "ADD COLUMN executionHour INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE recurring_entries " +
                            "ADD COLUMN executionMinute INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
