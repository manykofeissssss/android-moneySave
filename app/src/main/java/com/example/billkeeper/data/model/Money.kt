package com.example.billkeeper.data.model

import java.util.Locale

private val yuanInputPattern = Regex("[0-9]*(?:\\.[0-9]{0,2})?")

// Allow unfinished input such as "" and "1." while editing.
fun isValidYuanInput(input: String): Boolean = yuanInputPattern.matches(input)

fun parseYuanToCents(input: String): Long? {
    val normalizedInput = input.trim()
    if (!isValidYuanInput(normalizedInput)) return null
    val value = normalizedInput.toBigDecimalOrNull() ?: return null
    if (value <= java.math.BigDecimal.ZERO) return null

    return runCatching {
        value.movePointRight(2).longValueExact()
    }.getOrNull()
}

fun centsToYuanText(cents: Long): String =
    String.format(Locale.US, "%.2f", cents / 100.0)

fun formatCurrency(cents: Long): String =
    "¥${centsToYuanText(cents)}"
