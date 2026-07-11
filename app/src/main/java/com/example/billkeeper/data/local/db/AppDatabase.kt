package com.example.billkeeper.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.billkeeper.data.local.dao.BillDao
import com.example.billkeeper.data.local.dao.IncomeDao
import com.example.billkeeper.data.local.entity.BillItem
import com.example.billkeeper.data.local.entity.IncomeItem

@Database(entities = [BillItem::class, IncomeItem::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun billDao(): BillDao
    abstract fun incomeDao(): IncomeDao

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
    }
}
