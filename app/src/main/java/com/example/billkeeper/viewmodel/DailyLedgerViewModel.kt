package com.example.billkeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.billkeeper.data.local.entity.BillItem
import com.example.billkeeper.data.local.entity.IncomeItem
import com.example.billkeeper.data.repository.LedgerRepository
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class DailyLedgerUiState(
    val selectedDate: LocalDate,
    val bills: List<BillItem>,
    val incomes: List<IncomeItem>,
    val totalExpenseCents: Long,
    val totalIncomeCents: Long
) {
    val balanceCents: Long get() = totalIncomeCents - totalExpenseCents
    val entryCount: Int get() = bills.size + incomes.size
}

@OptIn(ExperimentalCoroutinesApi::class)
class DailyLedgerViewModel(
    private val repository: LedgerRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now(zoneId))

    val uiState: StateFlow<DailyLedgerUiState> = selectedDate
        .flatMapLatest { date ->
            val (start, endExclusive) = dayRangeMillis(date, zoneId)
            combine(
                repository.getBillsBetween(start, endExclusive),
                repository.getIncomesBetween(start, endExclusive)
            ) { bills, incomes ->
                DailyLedgerUiState(
                    selectedDate = date,
                    bills = bills,
                    incomes = incomes,
                    totalExpenseCents = bills.sumOf { it.amountCents },
                    totalIncomeCents = incomes.sumOf { it.amountCents }
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DailyLedgerUiState(
                selectedDate = selectedDate.value,
                bills = emptyList(),
                incomes = emptyList(),
                totalExpenseCents = 0L,
                totalIncomeCents = 0L
            )
        )

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun selectPreviousDay() {
        selectedDate.value = selectedDate.value.minusDays(1)
    }

    fun selectNextDay() {
        selectedDate.value = selectedDate.value.plusDays(1)
    }
}

class DailyLedgerViewModelFactory(
    private val repository: LedgerRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DailyLedgerViewModel::class.java)) {
            return DailyLedgerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

internal fun dayRangeMillis(
    date: LocalDate,
    zoneId: ZoneId = ZoneId.systemDefault()
): Pair<Long, Long> {
    val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val endExclusive = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    return start to endExclusive
}
