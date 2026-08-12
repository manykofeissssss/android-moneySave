package com.example.billkeeper.domain.recurring

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.billkeeper.data.local.db.AppDatabase
import com.example.billkeeper.data.local.entity.RecurringEntry
import com.example.billkeeper.data.repository.LedgerRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class RecurringEntryExecutionTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: LedgerRepository
    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        repository = LedgerRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun processingDueMonthlyEntry_catchesUpAndDoesNotInsertDuplicates() = runBlocking {
        val executionTime = LocalTime.of(8, 30)
        val firstRunAt = LocalDate.of(2026, 1, 31).atTimeMillis(executionTime)
        repository.upsertRecurringEntry(
            RecurringEntry(
                id = 7,
                entryType = RecurringEntryType.EXPENSE,
                categoryOrSource = "住房",
                amountCents = 250000,
                note = "房租",
                frequency = RecurringFrequency.MONTHLY,
                dayOfMonth = 31,
                nextRunAt = firstRunAt,
                createdAt = firstRunAt,
                executionHour = executionTime.hour,
                executionMinute = executionTime.minute
            )
        )
        val now = LocalDate.of(2026, 4, 30)
            .atTime(12, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val useCase = ProcessDueRecurringEntries(repository, zoneId)

        val firstResult = useCase(now)
        val secondResult = useCase(now)
        val bills = repository.allBills.first()
        val updatedEntry = repository.getRecurringEntryById(7)!!

        assertEquals(4, firstResult.generatedEntryCount)
        assertEquals(1, firstResult.processedScheduleCount)
        assertEquals(0, secondResult.generatedEntryCount)
        assertEquals(
            listOf(
                LocalDate.of(2026, 4, 30),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 1, 31)
            ),
            bills.map { it.date.toLocalDate() }
        )
        assertEquals(LocalDate.of(2026, 4, 30), updatedEntry.lastExecutedAt!!.toLocalDate())
        assertEquals(LocalDate.of(2026, 5, 31), updatedEntry.nextRunAt.toLocalDate())
        assertEquals(
            listOf(
                LocalDateTime.of(2026, 4, 30, 8, 30),
                LocalDateTime.of(2026, 3, 31, 8, 30),
                LocalDateTime.of(2026, 2, 28, 8, 30),
                LocalDateTime.of(2026, 1, 31, 8, 30)
            ),
            bills.map { it.date.toLocalDateTime() }
        )
        assertEquals(LocalDateTime.of(2026, 4, 30, 8, 30), updatedEntry.lastExecutedAt!!.toLocalDateTime())
        assertEquals(LocalDateTime.of(2026, 5, 31, 8, 30), updatedEntry.nextRunAt.toLocalDateTime())
    }

    private fun LocalDate.atTimeMillis(time: LocalTime): Long =
        atTime(time).atZone(zoneId).toInstant().toEpochMilli()

    private fun Long.toLocalDate(): LocalDate =
        java.time.Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

    private fun Long.toLocalDateTime(): LocalDateTime =
        java.time.Instant.ofEpochMilli(this).atZone(zoneId).toLocalDateTime()
}
