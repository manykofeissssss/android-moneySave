package com.example.billkeeper.diagnostics

import io.github.manykofeissssss.kdiagnostics.core.model.DiagnosticEvent
import io.github.manykofeissssss.kdiagnostics.core.model.DiagnosticEventType
import io.github.manykofeissssss.kdiagnostics.core.model.DiagnosticStatus

data class DiagnosticEventSummary(
    val total: Int,
    val crash: Int,
    val anr: Int,
    val uiBlock: Int,
    val pending: Int,
    val uploading: Int,
    val failed: Int,
    val reported: Int
) {
    companion object {
        fun from(events: List<DiagnosticEvent>): DiagnosticEventSummary = DiagnosticEventSummary(
            total = events.size,
            crash = events.count { it.type == DiagnosticEventType.CRASH },
            anr = events.count { it.type == DiagnosticEventType.ANR },
            uiBlock = events.count { it.type == DiagnosticEventType.UI_BLOCK },
            pending = events.count { it.status == DiagnosticStatus.PENDING },
            uploading = events.count { it.status == DiagnosticStatus.UPLOADING },
            failed = events.count { it.status == DiagnosticStatus.FAILED },
            reported = events.count { it.status == DiagnosticStatus.REPORTED }
        )
    }
}
