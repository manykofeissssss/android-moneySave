package com.example.billkeeper.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.billkeeper.R
import com.example.billkeeper.ui.MainActivity

object ReminderNotifications {
    const val CHANNEL_ID = "daily_ledger_reminders"
    private const val TEST_NOTIFICATION_ID = 3200

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "每日记账提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "每天两次可自定义时间的记账提醒"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun postTestNotification(context: Context): Boolean {
        createChannel(context)
        if (!canPostNotifications(context)) return false

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            TEST_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("测试提醒")
            .setContentText("通知功能正常，两次记账提醒会按设置时间发送。")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
        return true
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = ReminderType.fromAction(intent.action) ?: return
        val preferences = ReminderPreferences(context)
        if (!preferences.isEnabled(type)) return

        ReminderNotifications.createChannel(context)
        if (canPostNotifications(context)) {
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentIntent = PendingIntent.getActivity(
                context,
                type.requestCode,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, ReminderNotifications.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(type.title)
                .setContentText(type.message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(type.notificationId, notification)
        }

        ReminderScheduler(context).schedule(type)
    }
}

class ReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler(context).syncSchedules()
    }
}

internal fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

internal fun canPostNotifications(context: Context): Boolean {
    if (!hasNotificationPermission(context) ||
        !NotificationManagerCompat.from(context).areNotificationsEnabled()
    ) return false

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(ReminderNotifications.CHANNEL_ID)
        if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) return false
    }
    return true
}
