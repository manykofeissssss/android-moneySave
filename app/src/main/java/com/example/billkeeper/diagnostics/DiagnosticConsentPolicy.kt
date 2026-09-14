package com.example.billkeeper.diagnostics

import io.github.manykofeissssss.kdiagnostics.core.model.DiagnosticEvent
import io.github.manykofeissssss.kdiagnostics.core.model.DiagnosticEventType
import io.github.manykofeissssss.kdiagnostics.core.model.DiagnosticStatus

internal const val SYNTHETIC_EVENT_METADATA_KEY = "synthetic"

internal fun DiagnosticEvent.requiresUserConsent(): Boolean =
    type == DiagnosticEventType.CRASH ||
        metadata[DiagnosticEvent.REQUIRES_USER_CONSENT_METADATA_KEY].toBoolean()

internal fun DiagnosticEvent.isPendingAutomaticUpload(): Boolean =
    status == DiagnosticStatus.PENDING && !requiresUserConsent()

internal fun DiagnosticEvent.isPendingSyntheticConsentEvent(): Boolean =
    status == DiagnosticStatus.PENDING &&
        metadata[SYNTHETIC_EVENT_METADATA_KEY].toBoolean() &&
        type != DiagnosticEventType.UI_BLOCK &&
        requiresUserConsent()

internal fun DiagnosticEvent.isPendingAutomaticCrashPrompt(): Boolean =
    status == DiagnosticStatus.PENDING &&
        type == DiagnosticEventType.CRASH &&
        !metadata[SYNTHETIC_EVENT_METADATA_KEY].toBoolean()
