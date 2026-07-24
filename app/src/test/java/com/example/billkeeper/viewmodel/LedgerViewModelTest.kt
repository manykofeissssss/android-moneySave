package com.example.billkeeper.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class LedgerViewModelTest {
    @Test
    fun monthRangeMillis_usesEndExclusiveBoundary() {
        val zoneId = ZoneId.of("Asia/Shanghai")
        val (start, endExclusive) = monthRangeMillis(2024, 1, zoneId)

        assertEquals("2024-02-01T00:00+08:00[Asia/Shanghai]", Instant.ofEpochMilli(start).atZone(zoneId).toString())
        assertEquals("2024-03-01T00:00+08:00[Asia/Shanghai]", Instant.ofEpochMilli(endExclusive).atZone(zoneId).toString())
    }

    @Test
    fun yearMonthForMillis_usesLocalTimeZone() {
        val timestamp = Instant.parse("2026-07-31T16:30:00Z").toEpochMilli()

        val month = yearMonthForMillis(timestamp, ZoneId.of("Asia/Shanghai"))

        assertEquals("2026-08", month.toString())
    }
}
