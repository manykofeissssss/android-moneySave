package com.example.billkeeper.notification

import android.content.Context

class ReminderPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    var middayEnabled: Boolean
        get() = preferences.getBoolean(KEY_MIDDAY_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_MIDDAY_ENABLED, value).apply()

    var eveningEnabled: Boolean
        get() = preferences.getBoolean(KEY_EVENING_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_EVENING_ENABLED, value).apply()

    var middayHour: Int
        get() = preferences.getInt(KEY_MIDDAY_HOUR, ReminderType.MIDDAY.defaultHour)
        set(value) = preferences.edit().putInt(KEY_MIDDAY_HOUR, value).apply()

    var middayMinute: Int
        get() = preferences.getInt(KEY_MIDDAY_MINUTE, ReminderType.MIDDAY.defaultMinute)
        set(value) = preferences.edit().putInt(KEY_MIDDAY_MINUTE, value).apply()

    var eveningHour: Int
        get() = preferences.getInt(KEY_EVENING_HOUR, ReminderType.EVENING.defaultHour)
        set(value) = preferences.edit().putInt(KEY_EVENING_HOUR, value).apply()

    var eveningMinute: Int
        get() = preferences.getInt(KEY_EVENING_MINUTE, ReminderType.EVENING.defaultMinute)
        set(value) = preferences.edit().putInt(KEY_EVENING_MINUTE, value).apply()

    fun isEnabled(type: ReminderType): Boolean = when (type) {
        ReminderType.MIDDAY -> middayEnabled
        ReminderType.EVENING -> eveningEnabled
    }

    fun getTime(type: ReminderType): ReminderTime = when (type) {
        ReminderType.MIDDAY -> ReminderTime(middayHour, middayMinute)
        ReminderType.EVENING -> ReminderTime(eveningHour, eveningMinute)
    }

    fun setTime(type: ReminderType, hour: Int, minute: Int) {
        require(hour in 0..23 && minute in 0..59) { "Invalid reminder time" }
        when (type) {
            ReminderType.MIDDAY -> {
                middayHour = hour
                middayMinute = minute
            }
            ReminderType.EVENING -> {
                eveningHour = hour
                eveningMinute = minute
            }
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "reminder_preferences"
        private const val KEY_MIDDAY_ENABLED = "midday_enabled"
        private const val KEY_EVENING_ENABLED = "evening_enabled"
        private const val KEY_MIDDAY_HOUR = "midday_hour"
        private const val KEY_MIDDAY_MINUTE = "midday_minute"
        private const val KEY_EVENING_HOUR = "evening_hour"
        private const val KEY_EVENING_MINUTE = "evening_minute"
    }
}
