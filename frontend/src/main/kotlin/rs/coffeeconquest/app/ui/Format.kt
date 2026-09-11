package rs.coffeeconquest.app.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val serbian: Locale = Locale.forLanguageTag("sr-RS")
private val dayFormat = SimpleDateFormat("d. MMM", serbian)
private val dateTimeFormat = SimpleDateFormat("d. MMM yyyy. HH:mm", serbian)

fun relativeTime(epochMs: Long): String {
    val diff = System.currentTimeMillis() - epochMs
    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "upravo sad"
        minutes < 60 -> "pre $minutes min"
        hours < 24 -> "pre $hours h"
        days == 1L -> "juce"
        days < 7 -> "pre $days dana"
        else -> dayFormat.format(Date(epochMs))
    }
}

fun formatDateTime(epochMs: Long): String = dateTimeFormat.format(Date(epochMs))

fun formatDay(epochMs: Long): String = dayFormat.format(Date(epochMs))

fun formatDistance(meters: Double?): String? = when {
    meters == null -> null
    meters < 1000 -> "${meters.roundToInt()} m"
    else -> String.format(serbian, "%.1f km", meters / 1000)
}

fun formatRating(rating: Double): String =
    if (rating <= 0.0) "-" else String.format(serbian, "%.1f", rating)

fun points(value: Int): String {
    val mod100 = value % 100
    val mod10 = value % 10
    val word = when {
        mod100 in 11..14 -> "poena"
        mod10 == 1 -> "poen"
        mod10 in 2..4 -> "poena"
        else -> "poena"
    }
    return "$value $word"
}

fun days(value: Int): String {
    val word = if (value % 10 == 1 && value % 100 != 11) "dan" else "dana"
    return "$value $word"
}
