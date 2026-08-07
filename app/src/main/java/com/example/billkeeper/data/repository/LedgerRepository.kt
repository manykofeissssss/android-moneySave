package com.example.billkeeper.data.repository

import androidx.room.withTransaction
import com.example.billkeeper.data.local.db.AppDatabase
import com.example.billkeeper.data.local.entity.BillItem
import com.example.billkeeper.data.local.entity.IncomeItem
import com.example.billkeeper.data.local.entity.MonthlyBudget
import com.example.billkeeper.data.local.entity.RecurringEntry
import com.example.billkeeper.data.model.CategorySummary
import com.example.billkeeper.domain.recurring.RecurringEntryType
import kotlinx.coroutines.flow.Flow

class LedgerRepository(private val db: AppDatabase) {
    val allBills: Flow<List<BillItem>> = db.billDao().getAll()
    val categorySummary: Flow<List<CategorySummary>> = db.billDao().getCategorySummary()
    val totalExpense: Flow<Long> = db.billDao().getTotalExpense()

    suspend fun insertBill(bill: BillItem) = db.billDao().insert(bill)
    suspend fun updateBill(bill: BillItem) = db.billDao().update(bill)
    suspend fun deleteBill(bill: BillItem) = db.billDao().delete(bill)
    suspend fun getBillById(id: Int) = db.billDao().getById(id)

    fun getBillsByMonth(startMs: Long, endMs: Long): Flow<List<BillItem>> =
        db.billDao().getByMonth(startMs, endMs)

    fun getTotalExpenseByMonth(startMs: Long, endMs: Long): Flow<Long> =
        db.billDao().getTotalExpenseByMonth(startMs, endMs)

    fun getCategorySummaryByMonth(startMs: Long, endMs: Long): Flow<List<CategorySummary>> =
        db.billDao().getCategorySummaryByMonth(startMs, endMs)

    val allIncomes: Flow<List<IncomeItem>> = db.incomeDao().getAll()
    val totalIncome: Flow<Long> = db.incomeDao().getTotalIncome()

    fun observeBudgetsByMonth(
        year: Int,
        month: Int
    ): Flow<List<MonthlyBudget>> =
        db.monthlyBudgetDao().observeByMonth(year, month)

    suspend fun upsertBudget(budget: MonthlyBudget) =
        db.monthlyBudgetDao().upsert(budget)

    suspend fun deleteBudget(
        year: Int,
        month: Int,
        category: String
    ) = db.monthlyBudgetDao().delete(year, month, category)

    suspend fun insertIncome(income: IncomeItem) = db.incomeDao().insert(income)
    suspend fun updateIncome(income: IncomeItem) = db.incomeDao().update(income)
    suspend fun deleteIncome(income: IncomeItem) = db.incomeDao().delete(income)
    suspend fun getIncomeById(id: Int) = db.incomeDao().getById(id)

    fun getIncomesByMonth(startMs: Long, endMs: Long): Flow<List<IncomeItem>> =
        db.incomeDao().getByMonth(startMs, endMs)

    fun getTotalIncomeByMonth(startMs: Long, endMs: Long): Flow<Long> =
        db.incomeDao().getTotalIncomeByMonth(startMs, endMs)

    val recurringEntries: Flow<List<RecurringEntry>> = db.recurringEntryDao().observeAll()

    suspend fun getRecurringEntryById(id: Int): RecurringEntry? =
        db.recurringEntryDao().getById(id)

    suspend fun getDueRecurringEntries(nowMillis: Long): List<RecurringEntry> =
        db.recurringEntryDao().getDueEntries(nowMillis)

    suspend fun upsertRecurringEntry(entry: RecurringEntry) =
        db.recurringEntryDao().upsert(entry)

    suspend fun deleteRecurringEntry(id: Int) =
        db.recurringEntryDao().deleteById(id)

    suspend fun executeRecurringOccurrences(
        entryId: Int,
        expectedNextRunAt: Long,
        occurrenceTimes: List<Long>,
        nextRunAt: Long
    ): Boolean = db.withTransaction {
        val entry = db.recurringEntryDao().getById(entryId)
            ?: return@withTransaction false
        if (!entry.enabled || entry.nextRunAt != expectedNextRunAt || occurrenceTimes.isEmpty()) {
            return@withTransaction false
        }

        occurrenceTimes.forEach { occurrenceTime ->
            when (entry.entryType) {
                RecurringEntryType.EXPENSE -> db.billDao().insert(
                    BillItem(
                        category = entry.categoryOrSource,
                        amountCents = entry.amountCents,
                        date = occurrenceTime,
                        note = entry.note
                    )
                )

                RecurringEntryType.INCOME -> db.incomeDao().insert(
                    IncomeItem(
                        source = entry.categoryOrSource,
                        amountCents = entry.amountCents,
                        date = occurrenceTime,
                        note = entry.note
                    )
                )
            }
        }

        db.recurringEntryDao().upsert(
            entry.copy(
                nextRunAt = nextRunAt,
                lastExecutedAt = occurrenceTimes.last()
            )
        )
        true
    }
}
