package com.example.billkeeper.data.local.db

import androidx.room.TypeConverter
import com.example.billkeeper.domain.recurring.RecurringEntryType
import com.example.billkeeper.domain.recurring.RecurringFrequency

class RecurringEntryConverters {
    @TypeConverter
    fun entryTypeToString(value: RecurringEntryType): String = value.name

    @TypeConverter
    fun stringToEntryType(value: String): RecurringEntryType = RecurringEntryType.valueOf(value)

    @TypeConverter
    fun frequencyToString(value: RecurringFrequency): String = value.name

    @TypeConverter
    fun stringToFrequency(value: String): RecurringFrequency = RecurringFrequency.valueOf(value)
}
