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
    onRefresh: () -> Unit,
    onUpload: () -> Unit,
    onDeletePending: () -> Unit
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
                "总计 ${summary.total} · Crash ${summary.crash} · ANR ${summary.anr} · " +
                    "UI_BLOCK ${summary.uiBlock}"
            )
            Text(
                "PENDING ${summary.pending} · UPLOADING ${summary.uploading} · " +
                    "FAILED ${summary.failed} · REPORTED ${summary.reported}"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCrash) { Text("测试崩溃") }
                Button(onClick = onAnr) { Text("测试 ANR") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onUiBlock) { Text("测试卡顿") }
                Button(onClick = onRefresh) { Text("刷新") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onUpload) { Text("触发上传") }
                Button(onClick = onDeletePending) { Text("清理待处理") }
            }
        }
    }
}
