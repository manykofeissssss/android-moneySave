package com.example.billkeeper.notification

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ReminderSchedulerTest {
    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun nextReminderTimeMillis_usesTodayWhenTimeHasNotPassed() {
        val now = Instant.parse("2026-07-23T03:00:00Z").toEpochMilli()

        val result = nextReminderTimeMillis(12, 0, now, zoneId)

        assertEquals("2026-07-23T04:00:00Z", Instant.ofEpochMilli(result).toString())
    }

    @Test
    fun nextReminderTimeMillis_usesTomorrowWhenTimeHasPassed() {
        val now = Instant.parse("2026-07-23T15:00:00Z").toEpochMilli()

        val result = nextReminderTimeMillis(22, 0, now, zoneId)

        assertEquals("2026-07-24T14:00:00Z", Instant.ofEpochMilli(result).toString())
    }
}
