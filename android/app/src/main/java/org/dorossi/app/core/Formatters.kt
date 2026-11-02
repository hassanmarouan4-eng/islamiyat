package org.dorossi.app.core

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Number and date formatting used across the app (always Western digits 0-9). */
object Formatters {
    const val DECIMAL_SEPARATOR: Char = ','
    const val MILLIS_PER_DAY: Long = 86_400_000L

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /** Dates are stored as epoch days (LocalDate.toEpochDay) in the database. */
    fun date(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dateFormatter)

    fun today(): Long = LocalDate.now().toEpochDay()

    /** Times are stored as minutes since midnight (8:30 -> 510). */
    fun time(minutesOfDay: Int): String =
        String.format(Locale.ROOT, "%02d:%02d", minutesOfDay / 60, minutesOfDay % 60)

    /** Selectable times from [fromHour]:00 to [toHour]:00 every [stepMinutes] minutes. */
    fun timeOptions(stepMinutes: Int, fromHour: Int, toHour: Int): List<Pair<Int, String>> =
        (fromHour * 60..toHour * 60 step stepMinutes).map { it to time(it) }

    fun decimal(value: Double, maxFractionDigits: Int = 2): String {
        val symbols = DecimalFormatSymbols(Locale.ROOT).apply { decimalSeparator = DECIMAL_SEPARATOR }
        val pattern = if (maxFractionDigits <= 0) "0" else "0." + "#".repeat(maxFractionDigits)
        return DecimalFormat(pattern, symbols).format(value)
    }
}

/** Lenient parsing of numbers typed by users: "12,5" and "12.5" are both accepted. */
object Parsing {
    fun decimal(text: String): Double? =
        text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

    fun integer(text: String): Int? = text.trim().toIntOrNull()
}
