package com.example.billkeeper.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun parseYuanToCents_roundsToTwoDecimals() {
        assertEquals(1235L, parseYuanToCents("12.345"))
        assertEquals(100L, parseYuanToCents("1"))
        assertEquals(1L, parseYuanToCents("0.01"))
    }

    @Test
    fun parseYuanToCents_rejectsInvalidValues() {
        assertNull(parseYuanToCents(""))
        assertNull(parseYuanToCents("abc"))
        assertNull(parseYuanToCents("0"))
        assertNull(parseYuanToCents("-1"))
    }
}
