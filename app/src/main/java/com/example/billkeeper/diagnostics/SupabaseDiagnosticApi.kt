package com.example.billkeeper.diagnostics

import com.example.billkeeper.BuildConfig
import io.github.manykofeissssss.kdiagnostics.core.model.DiagnosticEvent
import io.github.manykofeissssss.kdiagnostics.core.model.UploadResult
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * BK-specific boundary for the diagnostics backend.
 *
 * The k-diagnostics library deliberately knows nothing about Supabase. This
 * adapter maps the library model to the `diagnostic_events` table exposed by
 * Supabase's Data API and keeps all backend configuration in BK.
 */
fun interface SupabaseDiagnosticApi {
    suspend fun upload(events: List<DiagnosticEvent>): UploadResult
}

/** Creates the configured adapter without exposing credentials to k-diagnostics. */
object SupabaseDiagnosticApiProvider {
    fun fromBuildConfig(): SupabaseDiagnosticApi {
        val url = BuildConfig.SUPABASE_URL.trim()
        val publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()
        if (url.isBlank() || publishableKey.isBlank()) {
            return UnconfiguredSupabaseDiagnosticApi
        }
        return SupabaseRestDiagnosticApi(url, publishableKey)
    }
}

/**
 * Minimal REST client for Supabase Data API.
 *
 * It uses only Android/JDK networking so BK does not need to couple the
 * reusable diagnostics library to supabase-kt. The publishable key is sent as
 * both `apikey` and bearer token, as required by the Data API.
 */
class SupabaseRestDiagnosticApi(
    projectUrl: String,
    publishableKey: String,
    private val connectTimeoutMillis: Int = DEFAULT_CONNECT_TIMEOUT_MILLIS,
    private val readTimeoutMillis: Int = DEFAULT_READ_TIMEOUT_MILLIS
) : SupabaseDiagnosticApi {
    private val projectUrl = projectUrl.trim().trimEnd('/')
    private val publishableKey = publishableKey.trim()

    override suspend fun upload(events: List<DiagnosticEvent>): UploadResult {
        if (events.isEmpty()) return UploadResult.Uploaded
        if (!isValidProjectUrl(projectUrl)) {
            return UploadResult.PermanentFailure("Supabase project URL is invalid")
        }
        if (publishableKey.isBlank()) {
            return UploadResult.PermanentFailure("Supabase publishable key is blank")
        }

        return try {
            withContext(Dispatchers.IO) {
                uploadBlocking(events)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (io: IOException) {
            UploadResult.RetryableFailure(
                message = "Supabase network request failed: ${io.message ?: "I/O error"}",
                cause = io
            )
        } catch (error: Exception) {
            UploadResult.PermanentFailure(
                message = "Supabase request could not be prepared: ${error.message ?: "unknown error"}",
                cause = error
            )
        }
    }

    private fun uploadBlocking(events: List<DiagnosticEvent>): UploadResult {
        val connection = (URL("$projectUrl/rest/v1/diagnostic_events")
            .openConnection() as HttpURLConnection)
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = connectTimeoutMillis
            connection.readTimeout = readTimeoutMillis
            connection.doOutput = true
            connection.useCaches = false
            connection.setRequestProperty("apikey", publishableKey)
            connection.setRequestProperty("Authorization", "Bearer $publishableKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            // Duplicate event IDs are considered success, which makes a
            // WorkManager retry idempotent after a process/network interruption.
            connection.setRequestProperty(
                "Prefer",
                "resolution=ignore-duplicates,return=minimal"
            )

            val body = JSONArray().apply {
                events.forEach { put(it.toSupabaseRecord()) }
            }.toString()
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }

            val statusCode = connection.responseCode
            val responseBody = readResponseBody(connection)
            when {
                statusCode in 200..299 -> UploadResult.Uploaded
                statusCode == HttpURLConnection.HTTP_CONFLICT -> UploadResult.Uploaded
                statusCode == HttpURLConnection.HTTP_CLIENT_TIMEOUT ||
                    statusCode == 425 ||
                    statusCode == 429 ||
                    statusCode >= 500 -> UploadResult.RetryableFailure(
                    message = formatHttpError(statusCode, responseBody)
                )

                else -> UploadResult.PermanentFailure(
                    message = formatHttpError(statusCode, responseBody)
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun readResponseBody(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        } ?: return ""
        return stream.bufferedReader().use { it.readText().take(MAX_RESPONSE_CHARS) }
    }

    private fun formatHttpError(statusCode: Int, responseBody: String): String {
        val suffix = responseBody.trim().takeIf { it.isNotEmpty() }?.let { ": $it" }.orEmpty()
        return "Supabase HTTP $statusCode$suffix"
    }

    private companion object {
        const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 10_000
        const val DEFAULT_READ_TIMEOUT_MILLIS = 15_000
        const val MAX_RESPONSE_CHARS = 4_000

        fun isValidProjectUrl(projectUrl: String): Boolean = runCatching {
            URL(projectUrl).let { it.protocol == "https" && it.host.isNotBlank() }
        }.getOrDefault(false)
    }
}

/** Keeps local events visible until valid Supabase configuration is supplied. */
object UnconfiguredSupabaseDiagnosticApi : SupabaseDiagnosticApi {
    override suspend fun upload(events: List<DiagnosticEvent>): UploadResult =
        if (events.isEmpty()) {
            UploadResult.Uploaded
        } else {
            UploadResult.PermanentFailure(
                message = "SupabaseDiagnosticApi is not configured in BillKeeper"
            )
        }
}

private fun DiagnosticEvent.toSupabaseRecord(): JSONObject = JSONObject().apply {
    put("event_id", eventId)
    put("event_type", type.name)
    put("occurred_at", Instant.ofEpochMilli(timestampMillis).toString())
    putNullable("app_version", appVersion)
    putNullable("device_model", deviceModel)
    putNullable("android_version", androidVersion)
    putNullable("thread_name", threadName)
    putNullable("duration_ms", durationMillis)
    putNullable("message", message)
    putNullable("stack_trace", stackTrace)
    put("metadata", JSONObject().apply {
        metadata.forEach { (key, value) -> put(key, value) }
    })
    put("schema_version", schemaVersion)
}

private fun JSONObject.putNullable(key: String, value: Any?) {
    put(key, value ?: JSONObject.NULL)
}
