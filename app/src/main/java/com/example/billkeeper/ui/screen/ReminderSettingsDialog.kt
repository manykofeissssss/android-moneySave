package com.example.billkeeper.ui.screen

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import com.example.billkeeper.notification.ReminderScheduler
import com.example.billkeeper.notification.ReminderType
import com.example.billkeeper.notification.canPostNotifications

@Composable
fun ReminderSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferences = remember { ReminderPreferences(context) }
    val scheduler = remember { ReminderScheduler(context) }
    var middayEnabled by remember { mutableStateOf(preferences.middayEnabled) }
    var eveningEnabled by remember { mutableStateOf(preferences.eveningEnabled) }
    var permissionGranted by remember { mutableStateOf(canPostNotifications(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) scheduler.syncSchedules()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = canPostNotifications(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun requestPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionGranted) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记账提醒") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderToggleRow(
                    title = "午间提醒",
                    time = "每天 12:00",
                    checked = middayEnabled,
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
                    title = "晚间提醒",
                    time = "每天 22:00",
                    checked = eveningEnabled,
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
                        TextButton(onClick = {
                            val settingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            context.startActivity(settingsIntent)
                        }) {
                            Text("去设置")
                        }
                    }
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
    time: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(time, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
