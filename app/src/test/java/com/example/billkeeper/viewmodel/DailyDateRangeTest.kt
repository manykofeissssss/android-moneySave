package com.example.billkeeper.viewmodel

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyDateRangeTest {
    @Test
    fun dayRange_usesLocalMidnightBoundaries() {
        val zone = ZoneId.of("Asia/Shanghai")

        val (start, endExclusive) = dayRangeMillis(LocalDate.of(2026, 8, 22), zone)

        assertEquals(Instant.parse("2026-08-21T16:00:00Z").toEpochMilli(), start)
        assertEquals(Instant.parse("2026-08-22T16:00:00Z").toEpochMilli(), endExclusive)
    }

    @Test
    fun dayRange_respectsDaylightSavingTime() {
        val zone = ZoneId.of("America/New_York")

        val (start, endExclusive) = dayRangeMillis(LocalDate.of(2026, 3, 8), zone)

        assertEquals(Duration.ofHours(23).toMillis(), endExclusive - start)
    }
}
