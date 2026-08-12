package com.example.billkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import com.example.billkeeper.domain.recurring.RecurringEntryType
import com.example.billkeeper.domain.recurring.RecurringFrequency

@Entity(
    tableName = "recurring_entries",
    indices = [Index(value = ["enabled", "nextRunAt"])]
)
data class RecurringEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entryType: RecurringEntryType,
    val categoryOrSource: String,
    val amountCents: Long,
    val note: String = "",
    val frequency: RecurringFrequency,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val nextRunAt: Long,
    val lastExecutedAt: Long? = null,
    val enabled: Boolean = true,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "0") val executionHour: Int = 0,
    @ColumnInfo(defaultValue = "0") val executionMinute: Int = 0
) {
    init {
        require(categoryOrSource.isNotBlank()) { "categoryOrSource must not be blank" }
        require(amountCents > 0) { "amountCents must be greater than 0" }
        require(executionHour in 0..23)
        require(executionMinute in 0..59)

        when (frequency) {
            RecurringFrequency.WEEKLY -> {
                require(dayOfWeek in 1..7) { "weekly entries require dayOfWeek between 1 and 7" }
                require(dayOfMonth == null) { "weekly entries must not define dayOfMonth" }
            }

            RecurringFrequency.MONTHLY -> {
                require(dayOfMonth in 1..31) { "monthly entries require dayOfMonth between 1 and 31" }
                require(dayOfWeek == null) { "monthly entries must not define dayOfWeek" }
            }
        }
    }
}
