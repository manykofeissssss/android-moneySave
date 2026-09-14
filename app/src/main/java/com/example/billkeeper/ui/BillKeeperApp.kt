package com.example.billkeeper.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.billkeeper.background.AppearancePreferences
import com.example.billkeeper.background.AppearanceSettings
import com.example.billkeeper.background.BackgroundPreferences
import com.example.billkeeper.diagnostics.DiagnosticEventSummary
import com.example.billkeeper.diagnostics.SYNTHETIC_EVENT_METADATA_KEY
import com.example.billkeeper.diagnostics.isPendingAutomaticCrashPrompt
import com.example.billkeeper.diagnostics.isPendingAutomaticUpload
import com.example.billkeeper.diagnostics.isPendingSyntheticConsentEvent
import com.example.billkeeper.ui.background.AppBackground
import com.example.billkeeper.ui.diagnostics.DiagnosticTestPanel
import com.example.billkeeper.ui.navigation.BillKeeperNavigation
import com.example.billkeeper.viewmodel.DailyLedgerViewModel
import com.example.billkeeper.viewmodel.LedgerViewModel
import com.example.billkeeper.viewmodel.RecurringEntryViewModel
import io.github.manykofeissssss.kdiagnostics.android.runtime.DiagnosticsHandle
import io.github.manykofeissssss.kdiagnostics.core.model.DiagnosticEvent
import io.github.manykofeissssss.kdiagnostics.core.model.DiagnosticEventType
import kotlinx.coroutines.delay
import java.util.UUID

@Composable
fun BillKeeperApp(
    vm: LedgerViewModel,
    recurringVm: RecurringEntryViewModel,
    dailyLedgerVm: DailyLedgerViewModel,
    appearanceSettings: AppearanceSettings,
    appearancePreferences: AppearancePreferences,
    darkTheme: Boolean,
    diagnostics: DiagnosticsHandle,
    onEnqueueDiagnosticUpload: (includeCrash: Boolean) -> Unit
) {
    val context = LocalContext.current
    val backgroundPreferences = remember(context) { BackgroundPreferences(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    var backgroundRevision by remember { mutableIntStateOf(0) }
    var diagnosticEvents by remember { mutableStateOf(diagnostics.store.list()) }
    var dismissedConsentEventIds by remember { mutableStateOf(emptySet<String>()) }
    var selectedSyntheticEventId by remember { mutableStateOf<String?>(null) }
    val diagnosticSummary = remember(diagnosticEvents) {
        DiagnosticEventSummary.from(diagnosticEvents)
    }
    val refreshDiagnostics = {
        diagnosticEvents = diagnostics.store.list()
    }

    LaunchedEffect(diagnostics) {
        while (true) {
            delay(1_000L)
            val latestEvents = diagnostics.store.list()
            diagnosticEvents = latestEvents
            // Real ANR/UI_BLOCK events can upload automatically. Crash events and
            // synthetic Crash/ANR events remain behind the shared consent prompt.
            if (latestEvents.any { it.isPendingAutomaticUpload() }) {
                onEnqueueDiagnosticUpload(false)
            }
        }
    }

    LaunchedEffect(vm) {
        vm.snackbarEvents.collect { event ->
            val canUndo = event.deletedEntry != null
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = if (canUndo) "撤销" else null,
                withDismissAction = canUndo,
                duration = if (canUndo) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                event.deletedEntry?.let(vm::restoreDeletedEntry)
            }
        }
    }
    LaunchedEffect(recurringVm) {
        recurringVm.messages.collect(snackbarHostState::showSnackbar)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackground(
            preferences = backgroundPreferences,
            settings = appearanceSettings,
            darkTheme = darkTheme,
            revision = backgroundRevision,
            modifier = Modifier.fillMaxSize()
        )
        BillKeeperNavigation(
            ledgerViewModel = vm,
            recurringEntryViewModel = recurringVm,
            dailyLedgerViewModel = dailyLedgerVm,
            appearanceSettings = appearanceSettings,
            appearancePreferences = appearancePreferences,
            backgroundPreferences = backgroundPreferences,
            darkTheme = darkTheme,
            snackbarHostState = snackbarHostState,
            onBackgroundChanged = { backgroundRevision++ }
        )
        DiagnosticTestPanel(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(2f),
            summary = diagnosticSummary,
            onCrash = { throw RuntimeException("BK diagnostics manual crash") },
            onAnr = { Thread.sleep(diagnostics.config.anrTimeoutMillis + 1_000L) },
            onUiBlock = { Thread.sleep(diagnostics.config.uiBlockThresholdMillis + 150L) },
            onSyntheticCrash = {
                appendSyntheticDiagnosticEvent(diagnostics, DiagnosticEventType.CRASH)
                refreshDiagnostics()
            },
            onSyntheticAnr = {
                appendSyntheticDiagnosticEvent(diagnostics, DiagnosticEventType.ANR)
                refreshDiagnostics()
            },
            onSyntheticUiBlock = {
                appendSyntheticDiagnosticEvent(diagnostics, DiagnosticEventType.UI_BLOCK)
                refreshDiagnostics()
            },
            canSubmitSynthetic = diagnosticEvents.any { it.isPendingSyntheticConsentEvent() },
            onSubmitSynthetic = {
                diagnosticEvents
                    .firstOrNull { it.isPendingSyntheticConsentEvent() }
                    ?.let { event ->
                        dismissedConsentEventIds -= event.eventId
                        selectedSyntheticEventId = event.eventId
                    }
            }
        )
        val automaticCrashPrompt = diagnosticEvents.firstOrNull {
            it.isPendingAutomaticCrashPrompt() && it.eventId !in dismissedConsentEventIds
        }
        val selectedSyntheticPrompt = selectedSyntheticEventId?.let { selectedId ->
            diagnosticEvents.firstOrNull {
                it.eventId == selectedId && it.isPendingSyntheticConsentEvent()
            }
        }
        (automaticCrashPrompt ?: selectedSyntheticPrompt)
            ?.let { event ->
                CrashReportDialog(
                    event = event,
                    onUpload = {
                        dismissedConsentEventIds += event.eventId
                        if (selectedSyntheticEventId == event.eventId) {
                            selectedSyntheticEventId = null
                        }
                        onEnqueueDiagnosticUpload(true)
                    },
                    onDiscard = {
                        diagnostics.store.remove(event.eventId)
                        dismissedConsentEventIds += event.eventId
                        if (selectedSyntheticEventId == event.eventId) {
                            selectedSyntheticEventId = null
                        }
                        refreshDiagnostics()
                    },
                    onLater = {
                        dismissedConsentEventIds += event.eventId
                        if (selectedSyntheticEventId == event.eventId) {
                            selectedSyntheticEventId = null
                        }
                    }
                )
            }
    }
}

private fun appendSyntheticDiagnosticEvent(
    diagnostics: DiagnosticsHandle,
    type: DiagnosticEventType
) {
    val timestampMillis = System.currentTimeMillis()
    diagnostics.store.append(
        DiagnosticEvent(
            eventId = "debug-${type.name.lowercase()}-${timestampMillis}-${UUID.randomUUID()}",
            type = type,
            timestampMillis = timestampMillis,
            appVersion = com.example.billkeeper.BuildConfig.VERSION_NAME,
            deviceModel = "debug-device",
            androidVersion = android.os.Build.VERSION.RELEASE,
            threadName = "debug-test",
            durationMillis = when (type) {
                DiagnosticEventType.CRASH -> null
                DiagnosticEventType.ANR -> 6_000L
                DiagnosticEventType.UI_BLOCK -> 400L
            },
            message = "Synthetic ${type.name} event from DiagnosticsTestPanel",
            stackTrace = "Synthetic stack trace; no process crash was triggered.",
            metadata = buildMap {
                put("source", "billkeeper-debug-panel")
                put(SYNTHETIC_EVENT_METADATA_KEY, "true")
                if (type != DiagnosticEventType.UI_BLOCK) {
                    put(DiagnosticEvent.REQUIRES_USER_CONSENT_METADATA_KEY, "true")
                }
            }
        )
    )
}
