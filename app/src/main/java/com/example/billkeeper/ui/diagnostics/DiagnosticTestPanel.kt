package com.example.billkeeper.ui.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.billkeeper.diagnostics.DiagnosticEventSummary

@Composable
internal fun DiagnosticTestPanel(
    modifier: Modifier = Modifier,
    summary: DiagnosticEventSummary,
    onCrash: () -> Unit,
    onAnr: () -> Unit,
    onUiBlock: () -> Unit,
    onSyntheticCrash: () -> Unit,
    onSyntheticAnr: () -> Unit,
    onSyntheticUiBlock: () -> Unit,
    canSubmitSynthetic: Boolean,
    onSubmitSynthetic: () -> Unit
) {
    if (!com.example.billkeeper.BuildConfig.DEBUG) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Diagnostics 测试入口", style = MaterialTheme.typography.titleMedium)
            Text(
                if (com.example.billkeeper.BuildConfig.SUPABASE_URL.isBlank()) {
                    "Supabase：未配置"
                } else {
                    "Supabase：已配置"
                },
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "总计 ${summary.total} · Crash ${summary.crash} · ANR ${summary.anr} · " +
                    "UI_BLOCK ${summary.uiBlock}"
            )
            Text(
                "PENDING ${summary.pending} · UPLOADING ${summary.uploading} · " +
                    "FAILED ${summary.failed} · REPORTED ${summary.reported}"
            )
            if (summary.maxRetryCount > 0) {
                Text("最大重试次数：${summary.maxRetryCount}")
            }
            summary.lastError?.let { error ->
                Text(
                    "最近失败原因：$error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCrash) { Text("测试崩溃") }
                Button(onClick = onAnr) { Text("测试 ANR") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onUiBlock) { Text("测试卡顿") }
                Button(onClick = onSyntheticUiBlock) { Text("写入 UI_BLOCK") }
            }
            Text(
                "写入 Crash/ANR 后点击“提交模拟事件”，使用与崩溃重启相同的确认框决定是否上报；" +
                    "UI_BLOCK 保持自动上报。",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSyntheticCrash) { Text("写入 Crash") }
                Button(onClick = onSyntheticAnr) { Text("写入 ANR") }
            }
            Button(
                onClick = onSubmitSynthetic,
                enabled = canSubmitSynthetic,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("提交模拟事件")
            }
        }
    }
}
