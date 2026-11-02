package org.dorossi.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvTest {
    @Test
    fun plainValuesAreNotQuoted() {
        assertEquals("plain", Csv.escape("plain"))
    }

    @Test
    fun separatorsAndQuotesAreEscaped() {
        assertEquals("\"a;b\"", Csv.escape("a;b"))
        assertEquals("\"say \"\"hi\"\"\"", Csv.escape("say \"hi\""))
    }

    @Test
    fun documentStartsWithByteOrderMark() {
        val doc = Csv.document(listOf("name"), listOf(listOf("Ahmed")))
        assertTrue(doc.startsWith("\uFEFF"))
        assertEquals("\uFEFFname\r\nAhmed\r\n", doc)
    }

    @Test
    fun decimalParsingAcceptsCommaAndDot() {
        assertEquals(12.5, Parsing.decimal("12,5")!!, 0.0)
        assertEquals(12.5, Parsing.decimal(" 12.5 ")!!, 0.0)
        assertEquals(null, Parsing.decimal("abc"))
        assertEquals(null, Parsing.decimal("NaN"))
    }
}
