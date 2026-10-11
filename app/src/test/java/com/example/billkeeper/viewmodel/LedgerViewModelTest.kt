package com.example.billkeeper.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test
import com.example.billkeeper.data.local.entity.CurrentFunds
import com.example.billkeeper.data.local.entity.BillItem
import java.time.Instant
import java.time.ZoneId

class LedgerViewModelTest {
    @Test
    fun categoryExpenses_filtersCategoryAndKeepsTotalsInSyncWithSortedRecords() {
        val oldMeal = BillItem(id = 1, category = "餐饮", amountCents = 1250L, date = 100L)
        val newMeal = BillItem(id = 3, category = "餐饮", amountCents = 2300L, date = 200L)
        val sameTimeMeal = BillItem(id = 4, category = "餐饮", amountCents = 500L, date = 200L)
        val transport = BillItem(id = 2, category = "交通", amountCents = 800L, date = 300L)

        val state = categoryExpensesUiState(listOf(oldMeal, transport, newMeal, sameTimeMeal), "餐饮")

        assertEquals(listOf(sameTimeMeal, newMeal, oldMeal), state.bills)
        assertEquals(4050L, state.totalExpenseCents)
        assertEquals(3, state.entryCount)
    }

    @Test
    fun categoryExpenses_hasZeroTotalsWhenCategoryHasNoRecords() {
        val state = categoryExpensesUiState(
            listOf(BillItem(id = 1, category = "交通", amountCents = 800L, date = 100L)),
            "餐饮"
        )

        assertEquals(emptyList<BillItem>(), state.bills)
        assertEquals(0L, state.totalExpenseCents)
        assertEquals(0, state.entryCount)
    }

    @Test
    fun remainingFunds_usesOnlyChangesAfterFundsWereSet() {
        val funds = CurrentFunds(
            amountCents = 10_000,
            incomeSnapshotCents = 5_000,
            expenseSnapshotCents = 3_000
        )

        assertEquals(10_300, calculateRemainingFunds(funds, 5_500, 3_200))
    }

    @Test
    fun remainingFunds_isZeroBeforeFundsAreSet() {
        assertEquals(0, calculateRemainingFunds(null, 5_500, 3_200))
    }

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
