package org.dorossi.app.core

/**
 * CSV writing that Excel opens correctly with Arabic and accented text: UTF-8 with a byte order
 * mark and ';' as separator (the default list separator of French/Moroccan Excel).
 */
object Csv {
    const val SEPARATOR: Char = ';'

    fun escape(value: String): String {
        val needsQuotes = value.any { it == SEPARATOR || it == '"' || it == '\n' || it == '\r' } ||
            value.startsWith(' ') || value.endsWith(' ')
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    fun line(values: List<String>): String = values.joinToString(SEPARATOR.toString()) { escape(it) }

    fun document(header: List<String>, rows: List<List<String>>): String = buildString {
        append('\uFEFF')
        append(line(header)).append("\r\n")
        rows.forEach { append(line(it)).append("\r\n") }
    }
}
