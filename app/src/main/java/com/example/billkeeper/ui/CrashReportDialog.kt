package com.example.billkeeper.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import io.github.manykofeissssss.kdiagnostics.core.model.DiagnosticEvent

@Composable
internal fun CrashReportDialog(
    event: DiagnosticEvent,
    onUpload: () -> Unit,
    onDiscard: () -> Unit,
    onLater: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text("检测到上次运行异常") },
        text = {
            Text(
                "类型：${event.type}\n" +
                    "线程：${event.threadName.orEmpty()}\n" +
                    "异常：${event.message ?: "未提供异常信息"}\n\n" +
                    "是否上报诊断事件，帮助改进应用？"
            )
        },
        confirmButton = {
            TextButton(onClick = onUpload) {
                Text("上报")
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text("删除")
            }
        },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
    )
}
