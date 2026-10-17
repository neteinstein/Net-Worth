package com.neteinstein.networth.core.domain

import com.neteinstein.networth.core.domain.format.MoneyParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyParserTest {
    @Test
    fun parsesPortugueseAndEnglishFormats() {
        assertEquals(1_234_56, MoneyParser.parseMinor("1.234,56"))
        assertEquals(1_234_56, MoneyParser.parseMinor("1,234.56"))
        assertEquals(16_841_21, MoneyParser.parseMinor("16.841,21"))
        assertEquals(147_268_16, MoneyParser.parseMinor("147.268,16"))
        assertEquals(86_40, MoneyParser.parseMinor("€ 86,40"))
        assertEquals(86_40, MoneyParser.parseMinor("86,4"))
        assertEquals(12_50, MoneyParser.parseMinor("12.5"))
        assertEquals(1_234_00, MoneyParser.parseMinor("1.234"))
        assertEquals(1_234_567_00, MoneyParser.parseMinor("1,234,567"))
        assertEquals(10, MoneyParser.parseMinor("0,10"))
        assertEquals(1_000_00, MoneyParser.parseMinor("1 000"))
        assertEquals(250_00, MoneyParser.parseMinor("250"))
    }

    @Test
    fun parsesSigns() {
        assertEquals(-733_59, MoneyParser.parseMinor("−733,59"))
        assertEquals(-12_00, MoneyParser.parseMinor("-12"))
        assertEquals(-5_10, MoneyParser.parseMinor("5,10-"))
        assertEquals(30_00, MoneyParser.parseMinor("+30"))
    }

    @Test
    fun rejectsNonNumbers() {
        assertNull(MoneyParser.parseMinor(""))
        assertNull(MoneyParser.parseMinor("abc"))
        assertNull(MoneyParser.parseMinor("1,2,3"))
        assertNull(MoneyParser.parseMinor("1.2345"))
        assertNull(MoneyParser.parseMinor("1-2"))
        assertEquals(1_500, MoneyParser.parseMinor("1500", fractionDigits = 0))
    }
}
