package com.example.billkeeper.data.local.db

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.withTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.billkeeper.data.local.entity.BillItem
import com.example.billkeeper.data.local.entity.IncomeItem
import com.example.billkeeper.data.local.entity.MonthlyBudget
import com.example.billkeeper.data.local.entity.RecurringEntry
import com.example.billkeeper.data.model.CategorySummary
import com.example.billkeeper.data.repository.LedgerRepository
import com.example.billkeeper.viewmodel.LedgerViewModel
import com.example.billkeeper.viewmodel.LedgerViewModelFactory
import com.example.billkeeper.viewmodel.calculateRemainingFunds
import com.example.billkeeper.domain.recurring.RecurringEntryType
import com.example.billkeeper.domain.recurring.RecurringFrequency
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val databaseName = "migration-test.db"

    @Test
    fun categoryExpenses_keepsSelectedMonthAndRefreshesAfterRecordChanges() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val zone = ZoneId.systemDefault()
        fun atMidnight(month: Int, day: Int): Long = LocalDate.of(2026, month, day)
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val first = BillItem(id = 1, category = "餐饮", amountCents = 1250L, date = atMidnight(9, 1))
        val last = BillItem(id = 2, category = "餐饮", amountCents = 2000L, date = atMidnight(10, 1) - 1L)

        try {
            database.billDao().insert(first)
            database.billDao().insert(last)
            database.billDao().insert(BillItem(id = 3, category = "餐饮", amountCents = 9999L, date = atMidnight(9, 1) - 1L))
            database.billDao().insert(BillItem(id = 4, category = "餐饮", amountCents = 9999L, date = atMidnight(10, 1)))
            database.billDao().insert(BillItem(id = 5, category = "交通", amountCents = 800L, date = atMidnight(9, 5)))
            val vm = LedgerViewModel(LedgerRepository(database))
            val expenses = vm.observeCategoryExpenses("餐饮", 2026, 9)

            val initial = expenses.first()
            assertEquals(listOf(last, first), initial.bills)
            assertEquals(3250L, initial.totalExpenseCents)
            assertEquals(2, initial.entryCount)

            val sameTime = last.copy(id = 6, amountCents = 500L)
            database.billDao().insert(sameTime)
            assertEquals(listOf(sameTime, last, first), expenses.first().bills)
            database.billDao().delete(sameTime)

            val updated = first.copy(amountCents = 1500L)
            database.billDao().update(updated)
            assertEquals(3500L, expenses.first().totalExpenseCents)
            database.billDao().delete(last)
            assertEquals(listOf(updated), expenses.first().bills)
            database.billDao().update(updated.copy(category = "交通"))
            assertEquals(0, expenses.first().entryCount)
        } finally {
            database.close()
        }
    }

    @Test
    fun monthlySummaries_groupOnlySelectedMonthAndRefreshAfterEdits() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repository = LedgerRepository(database)
        val start = 100L
        val end = 200L
        val bill = BillItem(id = 1, category = "餐饮", amountCents = 1_250L, date = start)
        val income = IncomeItem(id = 1, source = "工资", amountCents = 5_000L, date = start)
        try {
            val expenses = repository.getCategorySummaryByMonth(start, end)
            val incomes = repository.getIncomeSummaryByMonth(start, end)
            assertEquals(emptyList<CategorySummary>(), expenses.first())
            assertEquals(emptyList<CategorySummary>(), incomes.first())
            repository.insertBill(bill)
            repository.insertBill(bill.copy(id = 2, amountCents = 750L, date = end - 1))
            repository.insertBill(bill.copy(id = 3, category = "交通", amountCents = 800L))
            repository.insertIncome(income)
            repository.insertIncome(income.copy(id = 2, amountCents = 500L, date = end - 1))
            repository.insertIncome(income.copy(id = 3, source = "奖金", amountCents = 1_000L))
            // Neither side of the selected month's [start, end) boundary may leak into totals.
            for ((index, date) in listOf(start - 1, end).withIndex()) {
                repository.insertBill(bill.copy(id = index + 4, amountCents = 99_999L, date = date))
                repository.insertIncome(income.copy(id = index + 4, amountCents = 99_999L, date = date))
            }
            assertEquals(
                listOf(CategorySummary("餐饮", 2_000L), CategorySummary("交通", 800L)),
                expenses.first()
            )
            assertEquals(
                listOf(CategorySummary("工资", 5_500L), CategorySummary("奖金", 1_000L)),
                incomes.first()
            )
            repository.updateBill(bill.copy(category = "交通"))
            repository.updateIncome(income.copy(source = "奖金"))
            assertEquals(
                listOf(CategorySummary("交通", 2_050L), CategorySummary("餐饮", 750L)),
                expenses.first()
            )
            assertEquals(
                listOf(CategorySummary("奖金", 6_000L), CategorySummary("工资", 500L)),
                incomes.first()
            )
            repository.deleteBill(bill.copy(id = 2))
            repository.deleteIncome(income.copy(id = 2))
            assertEquals(listOf(CategorySummary("交通", 2_050L)), expenses.first())
            assertEquals(listOf(CategorySummary("奖金", 6_000L)), incomes.first())
        } finally {
            database.close()
        }
    }

    @Test
    fun currentFunds_resetAfterIncomeAndExpenseChanges_preservesTheCurrentBalance() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repository = LedgerRepository(database)
        val store = ViewModelStore()
        val vm = ViewModelProvider(store, LedgerViewModelFactory(repository))[LedgerViewModel::class.java]
        try {
            repository.saveCurrentFunds(10_000)
            repository.insertBill(BillItem(category = "餐饮", amountCents = 2_000, date = 1L))
            withTimeout(5_000) { vm.remainingFundsCents.first { it == 8_000L } }
            repository.insertIncome(IncomeItem(source = "工资", amountCents = 500, date = 2L))
            val currentBalance = withTimeout(5_000) {
                vm.remainingFundsCents.first { it == 8_500L }
            }

            repository.saveCurrentFunds(currentBalance)
            val saved = requireNotNull(repository.currentFunds.first())
            assertEquals(8_500L, saved.amountCents)
            assertEquals(500L, saved.incomeSnapshotCents)
            assertEquals(2_000L, saved.expenseSnapshotCents)

            repository.insertBill(BillItem(category = "交通", amountCents = 500, date = 3L))
            withTimeout(5_000) { vm.remainingFundsCents.first { it == 8_000L } }
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
            database.close()
        }
    }

    @Test
    fun currentFunds_concurrentLedgerTransaction_cannotSplitIncomeAndExpenseSnapshots() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repository = LedgerRepository(database)
        try {
            repeat(10) { index ->
                val start = CompletableDeferred<Unit>()
                coroutineScope {
                    val saveFunds = async(Dispatchers.Default) {
                        start.await()
                        repository.saveCurrentFunds(10_000)
                    }
                    val ledgerChange = async(Dispatchers.Default) {
                        start.await()
                        database.withTransaction {
                            repository.insertIncome(
                                IncomeItem(source = "工资", amountCents = 1_000, date = index.toLong())
                            )
                            repository.insertBill(
                                BillItem(category = "餐饮", amountCents = 1_000, date = index.toLong())
                            )
                        }
                    }
                    start.complete(Unit)
                    saveFunds.await()
                    ledgerChange.await()
                }

                val saved = requireNotNull(repository.currentFunds.first())
                assertEquals(saved.incomeSnapshotCents, saved.expenseSnapshotCents)
                assertEquals(
                    10_000L,
                    calculateRemainingFunds(saved, repository.totalIncome.first(), repository.totalExpense.first())
                )
            }
        } finally {
            database.close()
        }
    }

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
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
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
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
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
            .addMigrations(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6)
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

    @Test
    fun migrationFromVersionFour_addsExecutionTimeWithDefaultValues() = runBlocking {
        createVersionFourDatabase()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6)
            .build()

        try {
            database.openHelper.writableDatabase
            val entry = requireNotNull(database.recurringEntryDao().getById(11))

            assertEquals(0, entry.executionHour)
            assertEquals(0, entry.executionMinute)
            assertEquals("鎴跨", entry.categoryOrSource)
            assertEquals(250000, entry.amountCents)
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

    private fun createVersionFourDatabase() {
        createVersionThreeDatabase()

        val databaseFile = context.getDatabasePath(databaseName)
        SQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { database ->
            database.execSQL(
                """
                CREATE TABLE recurring_entries (
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
            database.execSQL(
                """
                CREATE INDEX index_recurring_entries_enabled_nextRunAt
                ON recurring_entries(enabled, nextRunAt)
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO recurring_entries (
                    id, entryType, categoryOrSource, amountCents, note, frequency,
                    dayOfWeek, dayOfMonth, nextRunAt, lastExecutedAt, enabled, createdAt
                ) VALUES (
                    11, 'EXPENSE', '鎴跨', 250000, '姣忔湀鎴跨', 'MONTHLY',
                    NULL, 31, 1788192000000, NULL, 1, 1785513600000
                )
                """.trimIndent()
            )
            database.version = 4
        }
    }
}
