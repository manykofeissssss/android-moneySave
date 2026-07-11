package com.example.billkeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.billkeeper.data.local.entity.BillItem
import com.example.billkeeper.data.local.entity.IncomeItem
import com.example.billkeeper.data.model.CategorySummary
import com.example.billkeeper.data.repository.LedgerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import java.util.Calendar

data class MonthlyUiState(
    val monthLabel: String,
    val bills: List<BillItem>,
    val incomes: List<IncomeItem>,
    val categorySummary: List<CategorySummary>,
    val totalExpenseCents: Long,
    val totalIncomeCents: Long,
    val prevMonthExpenseCents: Long
)

@OptIn(ExperimentalCoroutinesApi::class)
class LedgerViewModel(private val repo: LedgerRepository) : ViewModel() {
    val allBills: StateFlow<List<BillItem>> = repo.allBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allIncomes: StateFlow<List<IncomeItem>> = repo.allIncomes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categorySummary: Flow<List<CategorySummary>> = repo.categorySummary
    val totalExpense: Flow<Long> = repo.totalExpense
    val totalIncome: Flow<Long> = repo.totalIncome

    private val calendar = Calendar.getInstance()
    private val _selectedYear = MutableStateFlow(calendar.get(Calendar.YEAR))
    private val _selectedMonth = MutableStateFlow(calendar.get(Calendar.MONTH))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    val monthLabel: StateFlow<String> = combine(_selectedYear, _selectedMonth) { year, month ->
        "${year}年${month + 1}月"
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "${_selectedYear.value}年${_selectedMonth.value + 1}月"
    )

    private val monthRange: Flow<Pair<Long, Long>> = combine(_selectedYear, _selectedMonth) { year, month ->
        monthRangeMillis(year, month)
    }

    private val prevMonthRange: Flow<Pair<Long, Long>> = combine(_selectedYear, _selectedMonth) { year, month ->
        val current = YearMonth.of(year, month + 1).minusMonths(1)
        monthRangeMillis(current.year, current.monthValue - 1)
    }

    val monthlyBills: StateFlow<List<BillItem>> = monthRange.flatMapLatest { (start, endExclusive) ->
        repo.getBillsByMonth(start, endExclusive)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyCategorySummary: StateFlow<List<CategorySummary>> = monthlyBills.map { bills ->
        bills.groupBy { it.category }
            .map { (category, entries) ->
                CategorySummary(category = category, totalCents = entries.sumOf { it.amountCents })
            }
            .sortedByDescending { it.totalCents }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyTotalExpense: StateFlow<Long> = monthlyBills.map { bills ->
        bills.sumOf { it.amountCents }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val monthlyIncomes: StateFlow<List<IncomeItem>> = monthRange.flatMapLatest { (start, endExclusive) ->
        repo.getIncomesByMonth(start, endExclusive)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyTotalIncome: StateFlow<Long> = monthlyIncomes.map { incomes ->
        incomes.sumOf { it.amountCents }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val prevMonthTotalExpense: StateFlow<Long> = prevMonthRange.flatMapLatest { (start, endExclusive) ->
        repo.getTotalExpenseByMonth(start, endExclusive)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val monthlyAmounts = combine(
        monthlyTotalExpense,
        monthlyTotalIncome,
        prevMonthTotalExpense
    ) { expense, income, prevExpense ->
        Triple(expense, income, prevExpense)
    }

    val monthlyUiState: StateFlow<MonthlyUiState> = combine(
        monthLabel,
        monthlyBills,
        monthlyCategorySummary,
        monthlyIncomes,
        monthlyAmounts
    ) { label, bills, categorySummary, incomes, amounts ->
        MonthlyUiState(
            monthLabel = label,
            bills = bills,
            incomes = incomes,
            categorySummary = categorySummary,
            totalExpenseCents = amounts.first,
            totalIncomeCents = amounts.second,
            prevMonthExpenseCents = amounts.third
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MonthlyUiState(
            monthLabel = monthLabel.value,
            bills = emptyList(),
            incomes = emptyList(),
            categorySummary = emptyList(),
            totalExpenseCents = 0L,
            totalIncomeCents = 0L,
            prevMonthExpenseCents = 0L
        )
    )

    fun goToPreviousMonth() {
        val current = YearMonth.of(_selectedYear.value, _selectedMonth.value + 1).minusMonths(1)
        _selectedYear.value = current.year
        _selectedMonth.value = current.monthValue - 1
    }

    fun goToNextMonth() {
        val current = YearMonth.of(_selectedYear.value, _selectedMonth.value + 1).plusMonths(1)
        _selectedYear.value = current.year
        _selectedMonth.value = current.monthValue - 1
    }

    fun jumpToCurrentMonth() {
        val now = YearMonth.now()
        _selectedYear.value = now.year
        _selectedMonth.value = now.monthValue - 1
    }

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    fun addBill(category: String, amountCents: Long, note: String, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            try {
                repo.insertBill(BillItem(category = category, amountCents = amountCents, date = date, note = note))
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

    fun addIncome(source: String, amountCents: Long, note: String, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            try {
                repo.insertIncome(IncomeItem(source = source, amountCents = amountCents, date = date, note = note))
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

class LedgerViewModelFactory(
    private val repo: LedgerRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LedgerViewModel::class.java)) {
            return LedgerViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

internal fun monthRangeMillis(
    year: Int,
    zeroBasedMonth: Int,
    zoneId: ZoneId = ZoneId.systemDefault()
): Pair<Long, Long> {
    val month = YearMonth.of(year, zeroBasedMonth + 1)
    val start = month.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    val endExclusive = month.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    return start to endExclusive
}
