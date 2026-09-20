package com.finition.app.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale

object Format {

    private val currencyFormat: NumberFormat =
        NumberFormat.getCurrencyInstance().apply {
            maximumFractionDigits = 2
            currency = Currency.getInstance(Locale.getDefault())
        }

    fun money(cents: Long): String = currencyFormat.format(cents / 100.0)

    fun moneySigned(cents: Long): String =
        (if (cents < 0) "-" else "+") + money(kotlin.math.abs(cents))

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    fun date(epochMillis: Long): String = dateFormat.format(Date(epochMillis))

    fun monthLabel(year: Int, month: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, month - 1)
        return SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
    }

    /** Start of today in epoch millis — used for overdue comparisons. */
    fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Parses "12.34" to cents; returns null when invalid. */
    fun parseAmountToCents(raw: String): Long? {
        val cleaned = raw.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        return cleaned.toBigDecimalOrNull()
            ?.movePointRight(2)
            ?.setScale(0, java.math.RoundingMode.HALF_UP)
            ?.longValueExact()
            ?.takeIf { it >= 0 }
    }
}
