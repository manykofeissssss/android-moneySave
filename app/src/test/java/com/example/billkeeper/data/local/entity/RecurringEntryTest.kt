package com.example.billkeeper.data.local.entity

import com.example.billkeeper.domain.recurring.RecurringEntryType
import com.example.billkeeper.domain.recurring.RecurringFrequency
import org.junit.Assert.assertThrows
import org.junit.Test

class RecurringEntryTest {
    @Test
    fun weeklyEntry_rejectsMonthlyDay() {
        assertThrows(IllegalArgumentException::class.java) {
            RecurringEntry(
                entryType = RecurringEntryType.EXPENSE,
                categoryOrSource = "餐饮",
                amountCents = 1000,
                frequency = RecurringFrequency.WEEKLY,
                dayOfWeek = 1,
                dayOfMonth = 1,
                nextRunAt = 1,
                createdAt = 1
            )
        }
    }

    @Test
    fun monthlyEntry_rejectsInvalidAmount() {
        assertThrows(IllegalArgumentException::class.java) {
            RecurringEntry(
                entryType = RecurringEntryType.INCOME,
                categoryOrSource = "工资",
                amountCents = 0,
                frequency = RecurringFrequency.MONTHLY,
                dayOfMonth = 1,
                nextRunAt = 1,
                createdAt = 1
            )
        }
    }
}
