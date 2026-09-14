package com.example.billkeeper.diagnostics

import io.github.manykofeissssss.kdiagnostics.core.model.DiagnosticEvent
import io.github.manykofeissssss.kdiagnostics.core.model.DiagnosticEventType
import io.github.manykofeissssss.kdiagnostics.core.model.DiagnosticStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticConsentPolicyTest {
    @Test
    fun crashAndConsentMarkedAnrWaitForUserSubmission() {
        val crash = event(DiagnosticEventType.CRASH)
        val syntheticAnr = event(
            type = DiagnosticEventType.ANR,
            metadata = mapOf(
                SYNTHETIC_EVENT_METADATA_KEY to "true",
                DiagnosticEvent.REQUIRES_USER_CONSENT_METADATA_KEY to "true"
            )
        )

        assertFalse(crash.isPendingAutomaticUpload())
        assertFalse(syntheticAnr.isPendingAutomaticUpload())
        assertTrue(syntheticAnr.isPendingSyntheticConsentEvent())
    }

    @Test
    fun uiBlockRemainsEligibleForAutomaticUpload() {
        val uiBlock = event(
            type = DiagnosticEventType.UI_BLOCK,
            metadata = mapOf(SYNTHETIC_EVENT_METADATA_KEY to "true")
        )

        assertTrue(uiBlock.isPendingAutomaticUpload())
        assertFalse(uiBlock.isPendingSyntheticConsentEvent())
    }

    @Test
    fun reportedEventsAreNotPendingSubmissionCandidates() {
        val event = event(
            type = DiagnosticEventType.ANR,
            status = DiagnosticStatus.REPORTED,
            metadata = mapOf(
                SYNTHETIC_EVENT_METADATA_KEY to "true",
                DiagnosticEvent.REQUIRES_USER_CONSENT_METADATA_KEY to "true"
            )
        )

        assertFalse(event.isPendingAutomaticUpload())
        assertFalse(event.isPendingSyntheticConsentEvent())
    }

    private fun event(
        type: DiagnosticEventType,
        status: DiagnosticStatus = DiagnosticStatus.PENDING,
        metadata: Map<String, String> = emptyMap()
    ) = DiagnosticEvent(
        eventId = "event-${type.name}-${status.name}",
        type = type,
        timestampMillis = 1L,
        metadata = metadata,
        status = status
    )
}
