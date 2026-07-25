package com.example.billkeeper.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.Instant
import java.time.ZoneId

enum class ReminderType(
    val action: String,
    val requestCode: Int,
    val notificationId: Int,
    val defaultHour: Int,
    val defaultMinute: Int,
    val title: String,
    val message: String
) {
    MIDDAY(
        action = "com.example.billkeeper.action.MIDDAY_REMINDER",
        requestCode = 1200,
        notificationId = 1200,
        defaultHour = 12,
        defaultMinute = 0,
        title = "记账提醒一",
        message = "花一分钟补上今天的收支吧。"
    ),
    EVENING(
        action = "com.example.billkeeper.action.EVENING_REMINDER",
        requestCode = 2200,
        notificationId = 2200,
        defaultHour = 22,
        defaultMinute = 0,
        title = "记账提醒二",
        message = "回顾一下今天，别忘了补全账目。"
    );

    companion object {
        fun fromAction(action: String?): ReminderType? = entries.firstOrNull { it.action == action }
    }
}

data class ReminderTime(val hour: Int, val minute: Int)

class ReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)
    private val preferences = ReminderPreferences(appContext)

    fun syncSchedules() {
        ReminderType.entries.forEach { type ->
            if (preferences.isEnabled(type)) schedule(type) else cancel(type)
        }
    }

    fun schedule(type: ReminderType) {
        val time = preferences.getTime(type)
        val triggerAtMillis = nextReminderTimeMillis(time.hour, time.minute)
        val operation = pendingIntent(type)
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                operation
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                operation
            )
        }
    }

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun cancel(type: ReminderType) {
        alarmManager.cancel(pendingIntent(type))
    }

    private fun pendingIntent(type: ReminderType): PendingIntent {
        val intent = Intent(appContext, ReminderReceiver::class.java).setAction(type.action)
        return PendingIntent.getBroadcast(
            appContext,
            type.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

internal fun nextReminderTimeMillis(
    hour: Int,
    minute: Int,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
    var candidate = now.toLocalDate().atTime(hour, minute).atZone(zoneId)
    if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
    return candidate.toInstant().toEpochMilli()
}
