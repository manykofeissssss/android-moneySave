package com.example.billkeeper.data.repository

import com.example.billkeeper.data.local.db.AppDatabase
import com.example.billkeeper.data.local.entity.BillItem
import com.example.billkeeper.data.local.entity.IncomeItem
import com.example.billkeeper.data.model.CategorySummary
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

    suspend fun insertIncome(income: IncomeItem) = db.incomeDao().insert(income)
    suspend fun updateIncome(income: IncomeItem) = db.incomeDao().update(income)
    suspend fun deleteIncome(income: IncomeItem) = db.incomeDao().delete(income)
    suspend fun getIncomeById(id: Int) = db.incomeDao().getById(id)

    fun getIncomesByMonth(startMs: Long, endMs: Long): Flow<List<IncomeItem>> =
        db.incomeDao().getByMonth(startMs, endMs)

    fun getTotalIncomeByMonth(startMs: Long, endMs: Long): Flow<Long> =
        db.incomeDao().getTotalIncomeByMonth(startMs, endMs)
}
