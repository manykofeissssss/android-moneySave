package com.example.billkeeper.domain.recurring

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.billkeeper.data.local.db.AppDatabase
import com.example.billkeeper.data.local.entity.RecurringEntry
import com.example.billkeeper.data.repository.LedgerRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun togglingStaleEntry_preservesProgressAndDoesNotGenerateDuplicates() = runBlocking {
        val staleEntry = dueMonthlyEntry(8)
        repository.upsertRecurringEntry(staleEntry)
        val now = staleEntry.nextRunAt + 3_600_000L
        val useCase = ProcessDueRecurringEntries(repository, zoneId)
        assertEquals(1, useCase(now).generatedEntryCount)
        val executedEntry = requireNotNull(repository.getRecurringEntryById(staleEntry.id))

        repository.setRecurringEntryEnabled(staleEntry.id, false)
        val pausedEntry = requireNotNull(repository.getRecurringEntryById(staleEntry.id))
        assertFalse(pausedEntry.enabled)
        assertEquals(executedEntry.nextRunAt, pausedEntry.nextRunAt)
        assertEquals(executedEntry.lastExecutedAt, pausedEntry.lastExecutedAt)

        repository.setRecurringEntryEnabled(staleEntry.id, true)
        assertEquals(0, useCase(now).generatedEntryCount)
        assertEquals(1, repository.allBills.first().size)
    }

    @Test
    fun concurrentEditAndExecution_preserveBothTheEditAndExecutionProgress() = runBlocking {
        repeat(10) { index ->
            val entry = dueMonthlyEntry(20 + index)
            repository.upsertRecurringEntry(entry)
            val now = entry.nextRunAt + 3_600_000L
            val start = CompletableDeferred<Unit>()
            coroutineScope {
                val edit = async(Dispatchers.Default) {
                    start.await()
                    repository.saveRecurringEntry(entry.id) { latest ->
                        requireNotNull(latest).copy(note = "已修改备注")
                    }
                }
                val execution = async(Dispatchers.Default) {
                    start.await()
                    ProcessDueRecurringEntries(repository, zoneId)(now)
                }
                start.complete(Unit)
                edit.await()
                assertEquals(1, execution.await().generatedEntryCount)
            }

            val saved = requireNotNull(repository.getRecurringEntryById(entry.id))
            assertEquals("已修改备注", saved.note)
            assertEquals(entry.nextRunAt, saved.lastExecutedAt)
            assertEquals(LocalDate.of(2026, 2, 28), saved.nextRunAt.toLocalDate())
            assertEquals(0, ProcessDueRecurringEntries(repository, zoneId)(now).generatedEntryCount)
        }
        assertEquals(10, repository.allBills.first().size)
    }

    @Test
    fun savingDeletedEntry_doesNotRecreateThePlan() = runBlocking {
        val staleEntry = dueMonthlyEntry(9)
        repository.upsertRecurringEntry(staleEntry)
        repository.deleteRecurringEntry(staleEntry.id)

        val failure = runCatching {
            repository.saveRecurringEntry(staleEntry.id) { staleEntry }
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertNull(repository.getRecurringEntryById(staleEntry.id))
    }

    private fun dueMonthlyEntry(id: Int): RecurringEntry {
        val firstRunAt = LocalDate.of(2026, 1, 31).atTimeMillis(LocalTime.of(8, 30))
        return RecurringEntry(
            id = id,
            entryType = RecurringEntryType.EXPENSE,
            categoryOrSource = "住房",
            amountCents = 250_000,
            note = "房租",
            frequency = RecurringFrequency.MONTHLY,
            dayOfMonth = 31,
            nextRunAt = firstRunAt,
            createdAt = firstRunAt,
            executionHour = 8,
            executionMinute = 30
        )
    }

    private fun LocalDate.atTimeMillis(time: LocalTime): Long =
        atTime(time).atZone(zoneId).toInstant().toEpochMilli()

    private fun Long.toLocalDate(): LocalDate =
        java.time.Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

    private fun Long.toLocalDateTime(): LocalDateTime =
        java.time.Instant.ofEpochMilli(this).atZone(zoneId).toLocalDateTime()
}
