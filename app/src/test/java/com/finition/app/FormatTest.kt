package com.finition.app

import com.finition.app.util.Format
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatTest {

    @Test
    fun `parses whole amounts to cents`() {
        assertEquals(1200L, Format.parseAmountToCents("12"))
    }

    @Test
    fun `parses decimal amounts to cents`() {
        assertEquals(1234L, Format.parseAmountToCents("12.34"))
    }

    @Test
    fun `rounds beyond two decimals instead of crashing`() {
        assertEquals(1100L, Format.parseAmountToCents("10.999"))
        assertEquals(1099L, Format.parseAmountToCents("10.994"))
    }

    @Test
    fun `accepts thousands separators`() {
        assertEquals(123456L, Format.parseAmountToCents("1,234.56"))
    }

    @Test
    fun `rejects invalid input`() {
        assertNull(Format.parseAmountToCents(""))
        assertNull(Format.parseAmountToCents("abc"))
        assertNull(Format.parseAmountToCents("12.34.56"))
    }

    @Test
    fun `rejects negative amounts`() {
        assertNull(Format.parseAmountToCents("-5.00"))
    }

    @Test
    fun `money formatting divides cents`() {
        // Locale-dependent symbol, but the numeric part must be exact.
        val formatted = Format.money(123456L)
        assert(formatted.contains("1,234.56")) { "Unexpected format: $formatted" }
    }
}
