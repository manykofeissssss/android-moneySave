package com.example.billkeeper.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {
    @Test
    fun yuanInput_allowsIncompleteInputAndAtMostTwoDecimals() {
        listOf("", ".", ".1", ".12", "0", "0.", "0.0", "0.01", "12", "12.", "12.3", "12.34")
            .forEach { input -> assertTrue(input, isValidYuanInput(input)) }
    }

    @Test
    fun yuanInput_rejectsExtraDecimalsAndInvalidPastedText() {
        listOf("12.345", "0.001", "1.230", "1.2.3", "1,23", "1e2", "-1", "12a", "12.34\n")
            .forEach { input -> assertFalse(input, isValidYuanInput(input)) }
    }

    @Test
    fun parseYuanToCents_preservesAmountsWithoutRounding() {
        assertEquals(1234L, parseYuanToCents("12.34"))
        assertEquals(100L, parseYuanToCents("1"))
        assertEquals(120L, parseYuanToCents("1.2"))
        assertEquals(1L, parseYuanToCents("0.01"))
        assertEquals(1L, parseYuanToCents(".01"))
        assertEquals(100L, parseYuanToCents("1."))
        assertEquals(1234L, parseYuanToCents(" 12.34 "))
    }

    @Test
    fun parseYuanToCents_rejectsExtraPrecisionInsteadOfRoundingToZero() {
        listOf("0.001", "0.004", "0.005", "12.345", "1.230")
            .forEach { input -> assertNull(input, parseYuanToCents(input)) }
    }

    @Test
    fun parseYuanToCents_rejectsInvalidValues() {
        assertNull(parseYuanToCents(""))
        assertNull(parseYuanToCents("abc"))
        assertNull(parseYuanToCents("0"))
        assertNull(parseYuanToCents("0.00"))
        assertNull(parseYuanToCents("-1"))
        assertNull(parseYuanToCents("."))
        assertNull(parseYuanToCents("1e2"))
    }

    @Test
    fun parseYuanToCents_preservesLongBoundaryAndRejectsOverflow() {
        assertEquals(Long.MAX_VALUE, parseYuanToCents("92233720368547758.07"))
        assertNull(parseYuanToCents("92233720368547758.08"))
    }
}
