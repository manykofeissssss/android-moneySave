package com.example.billkeeper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class LedgerViewModel(private val repo: LedgerRepository) : ViewModel() {
    val allBills = repo.allBills
    val allIncomes = repo.allIncomes
    val categorySummary = repo.categorySummary
    val totalExpense = repo.totalExpense
    val totalIncome = repo.totalIncome

    // 一次性事件：Snackbar 消息
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // ── 账单操作 ──

    fun addBill(category: String, amount: Double, note: String, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            try {
                repo.insertBill(BillItem(category = category, amount = amount, date = date, note = note))
            } catch (e: Exception) {
                _snackbarMessage.emit("添加失败：${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    fun updateBill(bill: BillItem) {
        viewModelScope.launch {
            try {
                repo.updateBill(bill)
                _snackbarMessage.emit("已更新")
            } catch (e: Exception) {
                _snackbarMessage.emit("更新失败：${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    fun deleteBill(bill: BillItem) {
        viewModelScope.launch {
            try {
                repo.deleteBill(bill)
                _snackbarMessage.emit("已删除")
            } catch (e: Exception) {
                _snackbarMessage.emit("删除失败：${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    suspend fun getBillById(id: Int) = repo.getBillById(id)

    // ── 收入操作 ──

    fun addIncome(source: String, amount: Double, note: String, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            try {
                repo.insertIncome(IncomeItem(source = source, amount = amount, date = date, note = note))
            } catch (e: Exception) {
                _snackbarMessage.emit("添加失败：${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    fun updateIncome(income: IncomeItem) {
        viewModelScope.launch {
            try {
                repo.updateIncome(income)
                _snackbarMessage.emit("已更新")
            } catch (e: Exception) {
                _snackbarMessage.emit("更新失败：${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    fun deleteIncome(income: IncomeItem) {
        viewModelScope.launch {
            try {
                repo.deleteIncome(income)
                _snackbarMessage.emit("已删除")
            } catch (e: Exception) {
                _snackbarMessage.emit("删除失败：${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    suspend fun getIncomeById(id: Int) = repo.getIncomeById(id)
}
