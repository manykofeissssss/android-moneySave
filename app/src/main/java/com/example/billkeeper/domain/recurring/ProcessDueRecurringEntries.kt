package com.example.billkeeper.domain.recurring

import com.example.billkeeper.data.local.entity.RecurringEntry
import com.example.billkeeper.data.repository.LedgerRepository
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val today = now.toLocalDate()
        var generatedEntryCount = 0
        var processedScheduleCount = 0

        repository.getDueRecurringEntries(nowMillis).forEach { entry ->
            val executionTime = LocalTime.of(entry.executionHour, entry.executionMinute)
            val throughDate = if (now.toLocalTime().isBefore(executionTime)) today.minusDays(1) else today
            val occurrenceDates = dueOccurrences(
                schedule = entry.toSchedule(),
                firstDueDate = entry.nextRunAt.toLocalDate(),
                throughDate = throughDate
            )
            if (occurrenceDates.isEmpty()) return@forEach

            val nextDate = nextOccurrenceAfter(entry.toSchedule(), occurrenceDates.last())
            val applied = repository.executeRecurringOccurrences(
                entryId = entry.id,
                expectedNextRunAt = entry.nextRunAt,
                occurrenceTimes = occurrenceDates.map { it.atTimeMillis(executionTime) },
                nextRunAt = nextDate.atTimeMillis(executionTime)
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

    private fun LocalDate.atTimeMillis(time: LocalTime): Long =
        atTime(time).atZone(zoneId).toInstant().toEpochMilli()
}
