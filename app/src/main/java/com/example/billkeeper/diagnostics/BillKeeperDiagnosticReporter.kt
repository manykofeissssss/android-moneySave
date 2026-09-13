package com.example.billkeeper.diagnostics

import io.github.manykofeissssss.kdiagnostics.core.api.DiagnosticReporter
import io.github.manykofeissssss.kdiagnostics.core.model.DiagnosticEvent
import io.github.manykofeissssss.kdiagnostics.core.model.UploadResult

class BillKeeperDiagnosticReporter(
    private val api: SupabaseDiagnosticApi
) : DiagnosticReporter {
    override suspend fun upload(events: List<DiagnosticEvent>): UploadResult =
        api.upload(events)
}
