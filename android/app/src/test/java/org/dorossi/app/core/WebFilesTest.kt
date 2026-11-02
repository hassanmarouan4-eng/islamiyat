package org.dorossi.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebFilesTest {
    @Test
    fun arabicNameFromPlatformHeader() {
        // Header sent by the platform: encodeURIComponent of «ملخص الدرس.pdf»
        val header = "attachment; filename*=UTF-8''%D9%85%D9%84%D8%AE%D8%B5%20%D8%A7%D9%84%D8%AF%D8%B1%D8%B3.pdf"
        assertEquals("ملخص الدرس.pdf", WebFiles.downloadName(header, "https://dorossi.org/api/file/a1?t=x&dl=1", "pdf"))
    }

    @Test
    fun plusSignIsKeptAndQuotedNamesWork() {
        assertEquals("C++ notes.docx", WebFiles.dispositionName("attachment; filename*=UTF-8''C%2B%2B%20notes.docx"))
        assertEquals("devoir 1.pdf", WebFiles.dispositionName("attachment; filename=\"devoir 1.pdf\""))
        assertEquals("plain.pdf", WebFiles.dispositionName("attachment; filename=plain.pdf"))
        assertNull(WebFiles.dispositionName("inline"))
        assertNull(WebFiles.dispositionName(null))
    }

    @Test
    fun fallsBackToUrlThenToFile() {
        assertEquals("a1.docx", WebFiles.downloadName(null, "https://dorossi.org/api/file/a1?t=1", "docx"))
        assertEquals("file.pdf", WebFiles.downloadName("attachment", "https://dorossi.org", "pdf"))
        assertEquals("file", WebFiles.downloadName(null, "https://dorossi.org/", null))
    }

    @Test
    fun forbiddenCharactersAndLongNames() {
        assertEquals("a_b_c.pdf", WebFiles.sanitize("a/b:c.pdf"))
        val long = "س".repeat(200) + ".pptx"
        val cut = WebFiles.sanitize(long)
        assertTrue(cut.length <= 120)
        assertTrue(cut.endsWith(".pptx"))
    }

    @Test
    fun malformedPercentEncoding() {
        assertNull(WebFiles.percentDecode("%E", "UTF-8"))
        assertNull(WebFiles.percentDecode("%ZZ", "UTF-8"))
        assertEquals("a+b", WebFiles.percentDecode("a+b", "UTF-8"))
    }

    @Test
    fun pickerFiltersOnlyPlainMimeTypes() {
        assertEquals(listOf("image/*"), WebFiles.pickerMimeTypes(arrayOf("image/*")))
        assertEquals(listOf("image/png", "image/jpeg"), WebFiles.pickerMimeTypes(arrayOf("image/png, image/jpeg")))
        assertEquals(emptyList<String>(), WebFiles.pickerMimeTypes(arrayOf(".pptx,.docx,.pdf")))
        assertEquals(emptyList<String>(), WebFiles.pickerMimeTypes(arrayOf("image/*,.pdf")))
        assertEquals(emptyList<String>(), WebFiles.pickerMimeTypes(arrayOf("")))
        assertEquals(emptyList<String>(), WebFiles.pickerMimeTypes(null))
    }
}
