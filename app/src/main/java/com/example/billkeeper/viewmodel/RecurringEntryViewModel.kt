package com.example.billkeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.billkeeper.background.RecurringEntryScheduler
import com.example.billkeeper.data.local.entity.RecurringEntry
import com.example.billkeeper.data.repository.LedgerRepository
import com.example.billkeeper.domain.recurring.ProcessDueRecurringEntries
import com.example.billkeeper.domain.recurring.RecurringEntryType
import com.example.billkeeper.domain.recurring.RecurringFrequency
import com.example.billkeeper.domain.recurring.RecurringSchedule
import com.example.billkeeper.domain.recurring.nextOccurrenceAfter
import com.example.billkeeper.domain.recurring.occurrenceOnOrAfter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

data class RecurringEntryDraft(
    val id: Int = 0,
    val entryType: RecurringEntryType,
    val categoryOrSource: String,
    val amountCents: Long,
    val note: String,
    val frequency: RecurringFrequency,
    val selectedDay: Int
)

class RecurringEntryViewModel(
    private val repository: LedgerRepository,
    private val scheduler: RecurringEntryScheduler,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {
    val entries = repository.recurringEntries.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun save(draft: RecurringEntryDraft) {
        viewModelScope.launch {
            try {
                val categoryOrSource = draft.categoryOrSource.trim()
                require(categoryOrSource.isNotEmpty()) { "请选择分类或来源" }
                require(draft.amountCents > 0) { "金额必须大于 0" }
                validateSelectedDay(draft.frequency, draft.selectedDay)

                val existing = draft.id.takeIf { it != 0 }
                    ?.let { repository.getRecurringEntryById(it) }
                val nextRunAt = if (existing != null && !existing.hasScheduleChanged(draft)) {
                    existing.nextRunAt
                } else {
                    calculateFirstRunAt(draft, existing?.lastExecutedAt)
                }
                val now = clock.millis()
                repository.upsertRecurringEntry(
                    RecurringEntry(
                        id = existing?.id ?: 0,
                        entryType = draft.entryType,
                        categoryOrSource = categoryOrSource,
                        amountCents = draft.amountCents,
                        note = draft.note.trim(),
                        frequency = draft.frequency,
                        dayOfWeek = draft.selectedDay.takeIf {
                            draft.frequency == RecurringFrequency.WEEKLY
                        },
                        dayOfMonth = draft.selectedDay.takeIf {
                            draft.frequency == RecurringFrequency.MONTHLY
                        },
                        nextRunAt = nextRunAt,
                        lastExecutedAt = existing?.lastExecutedAt,
                        enabled = existing?.enabled ?: true,
                        createdAt = existing?.createdAt ?: now
                    )
                )
                scheduler.enqueueImmediateCheck()
                _messages.emit(if (existing == null) "周期记账已创建" else "周期记账已更新")
            } catch (error: Exception) {
                _messages.emit("保存失败：${error.localizedMessage ?: "未知错误"}")
            }
        }
    }

    fun setEnabled(entry: RecurringEntry, enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.upsertRecurringEntry(entry.copy(enabled = enabled))
                if (enabled) scheduler.enqueueImmediateCheck()
                _messages.emit(if (enabled) "周期记账已启用" else "周期记账已暂停")
            } catch (error: Exception) {
                _messages.emit("状态更新失败：${error.localizedMessage ?: "未知错误"}")
            }
        }
    }

    fun delete(entry: RecurringEntry) {
        viewModelScope.launch {
            try {
                repository.deleteRecurringEntry(entry.id)
                _messages.emit("周期记账已删除")
            } catch (error: Exception) {
                _messages.emit("删除失败：${error.localizedMessage ?: "未知错误"}")
            }
        }
    }

    fun runDueEntriesNow() {
        viewModelScope.launch {
            try {
                val result = ProcessDueRecurringEntries(repository, clock.zone)(clock.millis())
                val message = if (result.generatedEntryCount == 0) {
                    "当前没有到期账目"
                } else {
                    "已自动生成 ${result.generatedEntryCount} 条账目"
                }
                _messages.emit(message)
            } catch (error: Exception) {
                _messages.emit("检查失败：${error.localizedMessage ?: "未知错误"}")
            }
        }
    }

    private fun calculateFirstRunAt(
        draft: RecurringEntryDraft,
        lastExecutedAt: Long?
    ): Long {
        val schedule = draft.toSchedule()
        val today = LocalDate.now(clock)
        var firstDate = occurrenceOnOrAfter(schedule, today)
        val lastExecutedDate = lastExecutedAt?.let {
            Instant.ofEpochMilli(it).atZone(clock.zone).toLocalDate()
        }
        if (lastExecutedDate == firstDate) {
            firstDate = nextOccurrenceAfter(schedule, firstDate)
        }
        return firstDate.atStartOfDay(clock.zone).toInstant().toEpochMilli()
    }

    private fun RecurringEntry.hasScheduleChanged(draft: RecurringEntryDraft): Boolean =
        frequency != draft.frequency ||
            dayOfWeek != draft.selectedDay.takeIf { draft.frequency == RecurringFrequency.WEEKLY } ||
            dayOfMonth != draft.selectedDay.takeIf { draft.frequency == RecurringFrequency.MONTHLY }

    private fun RecurringEntryDraft.toSchedule(): RecurringSchedule = when (frequency) {
        RecurringFrequency.WEEKLY -> RecurringSchedule.Weekly(DayOfWeek.of(selectedDay))
        RecurringFrequency.MONTHLY -> RecurringSchedule.Monthly(selectedDay)
    }

    private fun validateSelectedDay(frequency: RecurringFrequency, selectedDay: Int) {
        when (frequency) {
            RecurringFrequency.WEEKLY -> require(selectedDay in 1..7) { "请选择星期" }
            RecurringFrequency.MONTHLY -> require(selectedDay in 1..31) { "请选择每月日期" }
        }
    }
}

class RecurringEntryViewModelFactory(
    private val repository: LedgerRepository,
    private val scheduler: RecurringEntryScheduler
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecurringEntryViewModel::class.java)) {
            return RecurringEntryViewModel(repository, scheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
