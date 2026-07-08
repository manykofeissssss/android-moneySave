package com.example.billkeeper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class LedgerViewModel(private val repo: LedgerRepository) : ViewModel() {
    // ── 全量数据（底部栏） ──
    val allBills: StateFlow<List<BillItem>> = repo.allBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allIncomes: StateFlow<List<IncomeItem>> = repo.allIncomes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categorySummary = repo.categorySummary
    val totalExpense = repo.totalExpense
    val totalIncome = repo.totalIncome

    // ── 月份选择 ──
    private val calendar = Calendar.getInstance()
    private val _selectedYear = MutableStateFlow(calendar.get(Calendar.YEAR))
    private val _selectedMonth = MutableStateFlow(calendar.get(Calendar.MONTH)) // 0-based
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    val monthLabel: StateFlow<String> = combine(_selectedYear, _selectedMonth) { year, month ->
        "${year}年${month + 1}月"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // 当前选中月份的起止毫秒
    private val monthRange: Flow<Pair<Long, Long>> = combine(_selectedYear, _selectedMonth) { year, month ->
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(year, month, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        start to end
    }

    // 上月范围（用于环比）
    private val prevMonthRange: Flow<Pair<Long, Long>> = combine(_selectedYear, _selectedMonth) { year, month ->
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(year, month - 1, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        start to end
    }

    // ── 月度数据（StateFlow：切换 Tab 回来时立即拿到已缓存值，无初始空值闪烁） ──
    val monthlyBills: StateFlow<List<BillItem>> = monthRange.flatMapLatest { (s, e) ->
        repo.getBillsByMonth(s, e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyCategorySummary: StateFlow<List<CategorySummary>> = monthRange.flatMapLatest { (s, e) ->
        repo.getCategorySummaryByMonth(s, e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyTotalExpense: StateFlow<Double> = monthRange.flatMapLatest { (s, e) ->
        repo.getTotalExpenseByMonth(s, e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyIncomes: StateFlow<List<IncomeItem>> = monthRange.flatMapLatest { (s, e) ->
        repo.getIncomesByMonth(s, e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyTotalIncome: StateFlow<Double> = monthRange.flatMapLatest { (s, e) ->
        repo.getTotalIncomeByMonth(s, e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // 上月总支出（环比）
    val prevMonthTotalExpense: StateFlow<Double> = prevMonthRange.flatMapLatest { (s, e) ->
        repo.getTotalExpenseByMonth(s, e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun goToPreviousMonth() {
        val cal = Calendar.getInstance()
        cal.set(_selectedYear.value, _selectedMonth.value, 1)
        cal.add(Calendar.MONTH, -1)
        _selectedYear.value = cal.get(Calendar.YEAR)
        _selectedMonth.value = cal.get(Calendar.MONTH)
    }

    fun goToNextMonth() {
        val cal = Calendar.getInstance()
        cal.set(_selectedYear.value, _selectedMonth.value, 1)
        cal.add(Calendar.MONTH, 1)
        _selectedYear.value = cal.get(Calendar.YEAR)
        _selectedMonth.value = cal.get(Calendar.MONTH)
    }

    fun jumpToCurrentMonth() {
        val now = Calendar.getInstance()
        _selectedYear.value = now.get(Calendar.YEAR)
        _selectedMonth.value = now.get(Calendar.MONTH)
    }

    // ── 一次性事件：Snackbar 消息 ──
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // ── 账单操作（支持指定日期） ──

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

    // ── 收入操作（支持指定日期） ──

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