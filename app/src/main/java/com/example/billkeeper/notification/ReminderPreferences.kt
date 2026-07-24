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

    fun isEnabled(type: ReminderType): Boolean = when (type) {
        ReminderType.MIDDAY -> middayEnabled
        ReminderType.EVENING -> eveningEnabled
    }

    companion object {
        private const val PREFERENCES_NAME = "reminder_preferences"
        private const val KEY_MIDDAY_ENABLED = "midday_enabled"
        private const val KEY_EVENING_ENABLED = "evening_enabled"
    }
}
