package com.example.billkeeper.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.sharednav.diagnostics.CrashRecord
import com.sharednav.diagnostics.CrashReporter
import com.sharednav.diagnostics.Diagnostics
import com.sharednav.diagnostics.reportLastCrash
import kotlinx.coroutines.launch

@Composable
internal fun CrashReportDialog(
    record: CrashRecord,
    reporter: CrashReporter,
    onDiscard: () -> Unit,
    onHandled: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isReporting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {},
        title = { Text("检测到上次运行异常") },
        text = {
            Text(
                "应用上次运行时发生异常（${record.exceptionType}）。是否上报崩溃日志，帮助改进应用？"
            )
        },
        confirmButton = {
            TextButton(
                enabled = !isReporting,
                onClick = {
                    scope.launch {
                        isReporting = true
                        Diagnostics.reportLastCrash(reporter)
                        onHandled()
                    }
                }
            ) {
                Text(if (isReporting) "上报中..." else "上报")
            }
        },
        dismissButton = {
            TextButton(enabled = !isReporting, onClick = onDiscard) {
                Text("不上报")
            }
        }
    )
}
