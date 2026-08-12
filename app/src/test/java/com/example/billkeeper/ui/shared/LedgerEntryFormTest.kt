package com.example.billkeeper.ui.shared

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class LedgerEntryFormTest {
    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun datePickerConversion_preservesLocalCalendarDate() {
        val localStart = LocalDate.of(2026, 7, 24)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val datePickerValue = localStart.toDatePickerUtcMillis(zoneId)
        val convertedBack = datePickerValue.toLocalStartOfDayMillis(zoneId)

        assertEquals("2026-07-24T00:00:00Z", Instant.ofEpochMilli(datePickerValue).toString())
        assertEquals(localStart, convertedBack)
    }

    @Test
    fun withTimeFrom_combinesSelectedDateAndSourceTime() {
        val selectedDate = LocalDate.of(2026, 8, 10)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val timeSource = LocalDateTime.of(2026, 8, 12, 14, 35, 27)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        val result = selectedDate.withTimeFrom(timeSource, zoneId)

        assertEquals(
            LocalDateTime.of(2026, 8, 10, 14, 35, 27),
            Instant.ofEpochMilli(result).atZone(zoneId).toLocalDateTime()
        )
    }
}
