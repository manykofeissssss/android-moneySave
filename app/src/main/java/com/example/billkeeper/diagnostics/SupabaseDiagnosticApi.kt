package com.example.billkeeper.diagnostics

import io.github.manykofeissssss.kdiagnostics.core.model.DiagnosticEvent
import io.github.manykofeissssss.kdiagnostics.core.model.UploadResult

/**
 * BK-specific boundary for the future Supabase implementation.
 *
 * TODO: implement this interface with the project's Supabase client. Keep
 * credentials and RLS-aware request code in BK; do not move them into
 * k-diagnostics.
 */
fun interface SupabaseDiagnosticApi {
    suspend fun upload(events: List<DiagnosticEvent>): UploadResult
}

/** Keeps local events visible until the real Supabase adapter is supplied. */
object UnconfiguredSupabaseDiagnosticApi : SupabaseDiagnosticApi {
    override suspend fun upload(events: List<DiagnosticEvent>): UploadResult =
        UploadResult.PermanentFailure(
            message = "SupabaseDiagnosticApi is not configured in BillKeeper"
        )
}
