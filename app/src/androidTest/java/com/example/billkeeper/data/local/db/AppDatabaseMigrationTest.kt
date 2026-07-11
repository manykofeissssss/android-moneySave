package com.example.billkeeper.data.local.db

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.billkeeper.data.local.entity.BillItem
import com.example.billkeeper.data.local.entity.IncomeItem
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
    fun createVersionOneDatabase() {
        context.deleteDatabase(databaseName)
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

    @After
    fun deleteDatabase() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationFromVersionOne_preservesRecordsAndConvertsAmountsToCents() = runBlocking {
        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
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
        } finally {
            database.close()
        }
    }
}
