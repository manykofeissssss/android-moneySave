package com.example.billkeeper.diagnostics

import io.github.manykofeissssss.kdiagnostics.core.model.DiagnosticEvent
import io.github.manykofeissssss.kdiagnostics.core.model.DiagnosticEventType
import io.github.manykofeissssss.kdiagnostics.core.model.DiagnosticStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticEventSummaryTest {
    @Test
    fun summaryCountsTypesAndUploadStates() {
        val events = listOf(
            event("crash", DiagnosticEventType.CRASH, DiagnosticStatus.PENDING),
            event("anr", DiagnosticEventType.ANR, DiagnosticStatus.UPLOADING),
            event("ui", DiagnosticEventType.UI_BLOCK, DiagnosticStatus.FAILED),
            event("reported", DiagnosticEventType.CRASH, DiagnosticStatus.REPORTED)
        )

        val summary = DiagnosticEventSummary.from(events)

        assertEquals(4, summary.total)
        assertEquals(2, summary.crash)
        assertEquals(1, summary.anr)
        assertEquals(1, summary.uiBlock)
        assertEquals(1, summary.pending)
        assertEquals(1, summary.uploading)
        assertEquals(1, summary.failed)
        assertEquals(1, summary.reported)
    }

    private fun event(
        id: String,
        type: DiagnosticEventType,
        status: DiagnosticStatus
    ) = DiagnosticEvent(
        eventId = id,
        type = type,
        timestampMillis = 1L,
        status = status
    )
}
