package com.example.billkeeper.data.local.db

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.billkeeper.data.local.entity.BillItem
import com.example.billkeeper.data.local.entity.IncomeItem
import com.example.billkeeper.data.local.entity.MonthlyBudget
import com.example.billkeeper.data.local.entity.RecurringEntry
import com.example.billkeeper.domain.recurring.RecurringEntryType
import com.example.billkeeper.domain.recurring.RecurringFrequency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val databaseName = "migration-test.db"

    @Before
    fun deleteExistingDatabase() {
        context.deleteDatabase(databaseName)
    }

    @After
    fun deleteDatabase() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationFromVersionOne_preservesRecordsAndConvertsAmountsToCents() = runBlocking {
        createVersionOneDatabase()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4
            )
            .build()

        try {
            database.openHelper.writableDatabase

            assertEquals(
                BillItem(id = 7, category = "餐饮", amountCents = 1235, date = 1704067200000, note = "午饭"),
                database.billDao().getById(7)
            )
            assertEquals(
                IncomeItem(id = 9, source = "工资", amountCents = 500000, date = 1704067200000, note = "一月工资"),
                database.incomeDao().getById(9)
            )
            assertEquals(
                emptyList<MonthlyBudget>(),
                database.monthlyBudgetDao().observeByMonth(2026, 7).first()
            )
            assertEquals(
                emptyList<RecurringEntry>(),
                database.recurringEntryDao().observeAll().first()
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun migrationFromVersionTwo_createsUsableMonthlyBudgetTable() = runBlocking {
        createVersionTwoDatabase()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4
            )
            .build()

        val budget = MonthlyBudget(
            year = 2026,
            month = 7,
            category = "餐饮",
            limitCents = 80000
        )

        try {
            database.openHelper.writableDatabase
            database.monthlyBudgetDao().upsert(budget)

            assertEquals(
                listOf(budget),
                database.monthlyBudgetDao().observeByMonth(2026, 7).first()
            )
            assertEquals(
                BillItem(id = 7, category = "餐饮", amountCents = 1235, date = 1704067200000, note = "午饭"),
                database.billDao().getById(7)
            )
            assertEquals(
                emptyList<RecurringEntry>(),
                database.recurringEntryDao().observeAll().first()
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun migrationFromVersionThree_createsUsableRecurringEntryTableAndPreservesData() = runBlocking {
        createVersionThreeDatabase()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(AppDatabase.MIGRATION_3_4)
            .build()
        val recurringEntry = RecurringEntry(
            id = 11,
            entryType = RecurringEntryType.EXPENSE,
            categoryOrSource = "房租",
            amountCents = 250000,
            note = "每月房租",
            frequency = RecurringFrequency.MONTHLY,
            dayOfMonth = 31,
            nextRunAt = 1788192000000,
            createdAt = 1785513600000
        )

        try {
            database.openHelper.writableDatabase
            database.recurringEntryDao().upsert(recurringEntry)

            assertEquals(recurringEntry, database.recurringEntryDao().getById(11))
            assertEquals(
                listOf(recurringEntry),
                database.recurringEntryDao().getDueEntries(recurringEntry.nextRunAt)
            )
            assertEquals(
                BillItem(id = 7, category = "餐饮", amountCents = 1235, date = 1704067200000, note = "午饭"),
                database.billDao().getById(7)
            )
            assertEquals(
                listOf(
                    MonthlyBudget(
                        year = 2026,
                        month = 7,
                        category = "餐饮",
                        limitCents = 80000
                    )
                ),
                database.monthlyBudgetDao().observeByMonth(2026, 7).first()
            )
        } finally {
            database.close()
        }
    }

    private fun createVersionOneDatabase() {
        val databaseFile = context.getDatabasePath(databaseName)
        databaseFile.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { database ->
            database.execSQL(
                """
                CREATE TABLE bills (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    category TEXT NOT NULL,
                    amount REAL NOT NULL,
                    date INTEGER NOT NULL,
                    note TEXT NOT NULL
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                CREATE TABLE incomes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    source TEXT NOT NULL,
                    amount REAL NOT NULL,
                    date INTEGER NOT NULL,
                    note TEXT NOT NULL
                )
                """.trimIndent()
            )
            database.execSQL("INSERT INTO bills (id, category, amount, date, note) VALUES (7, '餐饮', 12.345, 1704067200000, '午饭')")
            database.execSQL("INSERT INTO incomes (id, source, amount, date, note) VALUES (9, '工资', 5000.0, 1704067200000, '一月工资')")
            database.version = 1
        }
    }

    private fun createVersionTwoDatabase() {
        val databaseFile = context.getDatabasePath(databaseName)
        databaseFile.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { database ->
            database.execSQL(
                """
                CREATE TABLE bills (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    category TEXT NOT NULL,
                    amountCents INTEGER NOT NULL,
                    date INTEGER NOT NULL,
                    note TEXT NOT NULL
                )
                """.trimIndent()
            )
            database.execSQL("CREATE INDEX index_bills_date ON bills(date)")
            database.execSQL("CREATE INDEX index_bills_category ON bills(category)")
            database.execSQL(
                """
                CREATE TABLE incomes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    source TEXT NOT NULL,
                    amountCents INTEGER NOT NULL,
                    date INTEGER NOT NULL,
                    note TEXT NOT NULL
                )
                """.trimIndent()
            )
            database.execSQL("CREATE INDEX index_incomes_date ON incomes(date)")
            database.execSQL("CREATE INDEX index_incomes_source ON incomes(source)")
            database.execSQL(
                "INSERT INTO bills (id, category, amountCents, date, note) " +
                    "VALUES (7, '餐饮', 1235, 1704067200000, '午饭')"
            )
            database.execSQL(
                "INSERT INTO incomes (id, source, amountCents, date, note) " +
                    "VALUES (9, '工资', 500000, 1704067200000, '一月工资')"
            )
            database.version = 2
        }
    }

    private fun createVersionThreeDatabase() {
        createVersionTwoDatabase()

        val databaseFile = context.getDatabasePath(databaseName)
        SQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { database ->
            database.execSQL(
                """
                CREATE TABLE monthly_budgets (
                    year INTEGER NOT NULL,
                    month INTEGER NOT NULL,
                    category TEXT NOT NULL,
                    limitCents INTEGER NOT NULL,
                    PRIMARY KEY(year, month, category)
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO monthly_budgets (year, month, category, limitCents)
                VALUES (2026, 7, '餐饮', 80000)
                """.trimIndent()
            )
            database.version = 3
        }
    }
}
