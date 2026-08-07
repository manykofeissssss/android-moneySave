package com.example.billkeeper.domain.recurring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RecurringScheduleTest {
    @Test
    fun weeklyOccurrence_usesSameDayWhenItMatches() {
        val schedule = RecurringSchedule.Weekly(DayOfWeek.MONDAY)

        val result = occurrenceOnOrAfter(schedule, LocalDate.of(2026, 8, 3))

        assertEquals(LocalDate.of(2026, 8, 3), result)
    }

    @Test
    fun weeklyOccurrence_movesToNextSelectedDay() {
        val schedule = RecurringSchedule.Weekly(DayOfWeek.MONDAY)

        val result = occurrenceOnOrAfter(schedule, LocalDate.of(2026, 8, 4))

        assertEquals(LocalDate.of(2026, 8, 10), result)
    }

    @Test
    fun monthlyOccurrence_usesCurrentMonthWhenDateHasNotPassed() {
        val schedule = RecurringSchedule.Monthly(dayOfMonth = 10)

        val result = occurrenceOnOrAfter(schedule, LocalDate.of(2026, 8, 7))

        assertEquals(LocalDate.of(2026, 8, 10), result)
    }

    @Test
    fun monthlyOccurrence_movesToNextMonthWhenDateHasPassed() {
        val schedule = RecurringSchedule.Monthly(dayOfMonth = 10)

        val result = occurrenceOnOrAfter(schedule, LocalDate.of(2026, 8, 11))

        assertEquals(LocalDate.of(2026, 9, 10), result)
    }

    @Test
    fun monthlyOccurrence_clampsDayThirtyOneToEndOfFebruary() {
        val schedule = RecurringSchedule.Monthly(dayOfMonth = 31)

        val normalYear = occurrenceOnOrAfter(schedule, LocalDate.of(2026, 2, 1))
        val leapYear = occurrenceOnOrAfter(schedule, LocalDate.of(2028, 2, 1))

        assertEquals(LocalDate.of(2026, 2, 28), normalYear)
        assertEquals(LocalDate.of(2028, 2, 29), leapYear)
    }

    @Test
    fun nextOccurrenceAfter_clampedFebruaryMovesToMarchThirtyOne() {
        val schedule = RecurringSchedule.Monthly(dayOfMonth = 31)

        val result = nextOccurrenceAfter(schedule, LocalDate.of(2026, 2, 28))

        assertEquals(LocalDate.of(2026, 3, 31), result)
    }

    @Test
    fun dueOccurrences_returnsEveryMissedPeriodUpToLimit() {
        val schedule = RecurringSchedule.Monthly(dayOfMonth = 31)

        val result = dueOccurrences(
            schedule = schedule,
            firstDueDate = LocalDate.of(2026, 1, 31),
            throughDate = LocalDate.of(2026, 4, 30),
            maxOccurrences = 3
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 31)
            ),
            result
        )
    }

    @Test
    fun monthlySchedule_rejectsInvalidDay() {
        assertThrows(IllegalArgumentException::class.java) {
            RecurringSchedule.Monthly(dayOfMonth = 32)
        }
    }
}
