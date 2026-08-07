package com.example.billkeeper.domain.recurring

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.min

enum class RecurringEntryType {
    EXPENSE,
    INCOME
}

enum class RecurringFrequency {
    WEEKLY,
    MONTHLY
}

sealed interface RecurringSchedule {
    data class Weekly(val dayOfWeek: DayOfWeek) : RecurringSchedule

    data class Monthly(val dayOfMonth: Int) : RecurringSchedule {
        init {
            require(dayOfMonth in 1..31) { "dayOfMonth must be between 1 and 31" }
        }
    }
}

internal fun occurrenceOnOrAfter(
    schedule: RecurringSchedule,
    fromDate: LocalDate
): LocalDate = when (schedule) {
    is RecurringSchedule.Weekly -> {
        val daysUntilOccurrence =
            (schedule.dayOfWeek.value - fromDate.dayOfWeek.value + DAYS_PER_WEEK) % DAYS_PER_WEEK
        fromDate.plusDays(daysUntilOccurrence.toLong())
    }

    is RecurringSchedule.Monthly -> {
        val currentMonth = YearMonth.from(fromDate)
        val currentCandidate = currentMonth.dateFor(schedule.dayOfMonth)
        if (currentCandidate.isBefore(fromDate)) {
            currentMonth.plusMonths(1).dateFor(schedule.dayOfMonth)
        } else {
            currentCandidate
        }
    }
}

internal fun nextOccurrenceAfter(
    schedule: RecurringSchedule,
    currentOccurrence: LocalDate
): LocalDate = occurrenceOnOrAfter(schedule, currentOccurrence.plusDays(1))

internal fun dueOccurrences(
    schedule: RecurringSchedule,
    firstDueDate: LocalDate,
    throughDate: LocalDate,
    maxOccurrences: Int = DEFAULT_MAX_CATCH_UP_OCCURRENCES
): List<LocalDate> {
    require(maxOccurrences > 0) { "maxOccurrences must be greater than 0" }
    if (firstDueDate.isAfter(throughDate)) return emptyList()

    return buildList {
        var occurrence = firstDueDate
        while (!occurrence.isAfter(throughDate) && size < maxOccurrences) {
            add(occurrence)
            occurrence = nextOccurrenceAfter(schedule, occurrence)
        }
    }
}

private fun YearMonth.dateFor(dayOfMonth: Int): LocalDate =
    atDay(min(dayOfMonth, lengthOfMonth()))

private const val DAYS_PER_WEEK = 7
internal const val DEFAULT_MAX_CATCH_UP_OCCURRENCES = 12
