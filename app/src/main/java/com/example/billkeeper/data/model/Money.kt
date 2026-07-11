package com.example.billkeeper.data.model

import java.math.RoundingMode
import java.util.Locale

fun parseYuanToCents(input: String): Long? {
    val value = input.trim().toBigDecimalOrNull() ?: return null
    if (value <= java.math.BigDecimal.ZERO) return null

    return runCatching {
        value.setScale(2, RoundingMode.HALF_UP)
            .movePointRight(2)
            .longValueExact()
    }.getOrNull()
}

fun centsToYuanText(cents: Long): String =
    String.format(Locale.US, "%.2f", cents / 100.0)

fun formatCurrency(cents: Long): String =
    "¥${centsToYuanText(cents)}"
