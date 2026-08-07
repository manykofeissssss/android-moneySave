package com.example.billkeeper.domain.recurring

import com.example.billkeeper.data.local.entity.RecurringEntry
import com.example.billkeeper.data.repository.LedgerRepository
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class RecurringExecutionResult(
    val generatedEntryCount: Int,
    val processedScheduleCount: Int
)

class ProcessDueRecurringEntries(
    private val repository: LedgerRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    suspend operator fun invoke(nowMillis: Long = System.currentTimeMillis()): RecurringExecutionResult {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        var generatedEntryCount = 0
        var processedScheduleCount = 0

        repository.getDueRecurringEntries(nowMillis).forEach { entry ->
            val occurrenceDates = dueOccurrences(
                schedule = entry.toSchedule(),
                firstDueDate = entry.nextRunAt.toLocalDate(),
                throughDate = today
            )
            if (occurrenceDates.isEmpty()) return@forEach

            val nextDate = nextOccurrenceAfter(entry.toSchedule(), occurrenceDates.last())
            val applied = repository.executeRecurringOccurrences(
                entryId = entry.id,
                expectedNextRunAt = entry.nextRunAt,
                occurrenceTimes = occurrenceDates.map { it.atStartOfDayMillis() },
                nextRunAt = nextDate.atStartOfDayMillis()
            )
            if (applied) {
                generatedEntryCount += occurrenceDates.size
                processedScheduleCount++
            }
        }

        return RecurringExecutionResult(generatedEntryCount, processedScheduleCount)
    }

    private fun RecurringEntry.toSchedule(): RecurringSchedule = when (frequency) {
        RecurringFrequency.WEEKLY -> RecurringSchedule.Weekly(DayOfWeek.of(requireNotNull(dayOfWeek)))
        RecurringFrequency.MONTHLY -> RecurringSchedule.Monthly(requireNotNull(dayOfMonth))
    }

    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

    private fun LocalDate.atStartOfDayMillis(): Long =
        atStartOfDay(zoneId).toInstant().toEpochMilli()
}
