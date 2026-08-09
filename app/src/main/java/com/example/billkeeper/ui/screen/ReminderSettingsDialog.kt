package com.example.billkeeper.ui.screen

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.billkeeper.notification.ReminderPreferences
import com.example.billkeeper.notification.ReminderNotifications
import com.example.billkeeper.notification.ReminderScheduler
import com.example.billkeeper.notification.ReminderTime
import com.example.billkeeper.notification.ReminderType
import com.example.billkeeper.notification.canPostNotifications
import com.example.billkeeper.notification.hasNotificationPermission
import java.util.Locale

@Composable
fun ReminderSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferences = remember { ReminderPreferences(context) }
    val scheduler = remember { ReminderScheduler(context) }
    var middayEnabled by remember { mutableStateOf(preferences.middayEnabled) }
    var eveningEnabled by remember { mutableStateOf(preferences.eveningEnabled) }
    var middayTime by remember { mutableStateOf(preferences.getTime(ReminderType.MIDDAY)) }
    var eveningTime by remember { mutableStateOf(preferences.getTime(ReminderType.EVENING)) }
    var permissionGranted by remember { mutableStateOf(canPostNotifications(context)) }
    var exactAlarmGranted by remember { mutableStateOf(scheduler.canScheduleExactAlarms()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = canPostNotifications(context)
        if (granted) {
            scheduler.syncSchedules()
            statusMessage = "通知权限已开启，可点击测试通知验证"
        } else {
            statusMessage = "未获得通知权限，提醒无法显示"
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = canPostNotifications(context)
                exactAlarmGranted = scheduler.canScheduleExactAlarms()
                if (exactAlarmGranted) scheduler.syncSchedules()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun requestPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission(context)
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun openNotificationSettings() {
        val settingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        context.startActivity(settingsIntent)
    }

    fun chooseTime(type: ReminderType, current: ReminderTime) {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val updated = ReminderTime(hour, minute)
                preferences.setTime(type, hour, minute)
                when (type) {
                    ReminderType.MIDDAY -> middayTime = updated
                    ReminderType.EVENING -> eveningTime = updated
                }
                if (preferences.isEnabled(type)) scheduler.schedule(type)
                statusMessage = "${type.displayName}已改为 ${formatReminderTime(updated)}"
            },
            current.hour,
            current.minute,
            true
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记账提醒") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReminderToggleRow(
                    title = "提醒一",
                    time = middayTime,
                    checked = middayEnabled,
                    onTimeClick = { chooseTime(ReminderType.MIDDAY, middayTime) },
                    onCheckedChange = { enabled ->
                        middayEnabled = enabled
                        preferences.middayEnabled = enabled
                        if (enabled) {
                            scheduler.schedule(ReminderType.MIDDAY)
                            requestPermissionIfNeeded()
                        } else {
                            scheduler.cancel(ReminderType.MIDDAY)
                        }
                    }
                )
                ReminderToggleRow(
                    title = "提醒二",
                    time = eveningTime,
                    checked = eveningEnabled,
                    onTimeClick = { chooseTime(ReminderType.EVENING, eveningTime) },
                    onCheckedChange = { enabled ->
                        eveningEnabled = enabled
                        preferences.eveningEnabled = enabled
                        if (enabled) {
                            scheduler.schedule(ReminderType.EVENING)
                            requestPermissionIfNeeded()
                        } else {
                            scheduler.cancel(ReminderType.EVENING)
                        }
                    }
                )

                if (!permissionGranted && (middayEnabled || eveningEnabled)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = Color(0xFFC62828)
                        )
                        Text(
                            "通知权限未开启",
                            color = Color(0xFFC62828),
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        )
                        TextButton(onClick = ::openNotificationSettings) {
                            Text("去设置")
                        }
                    }
                }

                if (!exactAlarmGranted && (middayEnabled || eveningEnabled) &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFFF57C00)
                        )
                        Text(
                            "尚未允许准点提醒",
                            color = Color(0xFFF57C00),
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        )
                        TextButton(onClick = {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }) {
                            Text("去允许")
                        }
                    }
                }

                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        when {
                            !hasNotificationPermission(context) -> {
                                requestPermissionIfNeeded()
                                statusMessage = "请先允许通知权限"
                            }
                            ReminderNotifications.postTestNotification(context) -> {
                                permissionGranted = true
                                statusMessage = "测试通知已发送"
                            }
                            else -> {
                                statusMessage = "通知或提醒渠道已关闭，请在系统设置中开启"
                                openNotificationSettings()
                            }
                        }
                    }
                ) {
                    Text("发送测试通知")
                }

                statusMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}

@Composable
private fun ReminderToggleRow(
    title: String,
    time: ReminderTime,
    checked: Boolean,
    onTimeClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            TextButton(onClick = onTimeClick) {
                Icon(Icons.Default.Schedule, contentDescription = null)
                Text("每天 ${formatReminderTime(time)}", modifier = Modifier.padding(start = 6.dp))
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private val ReminderType.displayName: String
    get() = when (this) {
        ReminderType.MIDDAY -> "提醒一"
        ReminderType.EVENING -> "提醒二"
    }

private fun formatReminderTime(time: ReminderTime): String =
    String.format(Locale.getDefault(), "%02d:%02d", time.hour, time.minute)
