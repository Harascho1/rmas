package rs.coffeeconquest.shared.rules

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * All timestamps are stored as epoch milliseconds, and all "which day is it"
 * questions (streaks, daily limits) are answered in one fixed zone so the
 * streak does not break when a user travels.
 *
 * This used to live in the backend. With Firestore the client does the scoring,
 * so the clock moved next to [ScoreRules].
 */
object Time {

    val zone: ZoneId = ZoneId.of("Europe/Belgrade")

    fun now(): Long = System.currentTimeMillis()

    fun epochDay(epochMs: Long): Long =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate().toEpochDay()

    fun today(): Long = epochDay(now())

    fun startOfDayMs(epochDay: Long): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(zone).toInstant().toEpochMilli()

    fun hourOfDay(epochMs: Long): Int =
        Instant.ofEpochMilli(epochMs).atZone(zone).hour

    fun daysAgoMs(days: Long): Long = now() - days * 24L * 60 * 60 * 1000

    /** Monday 00:00 of the week containing [epochMs] - the reset point for city champions. */
    fun startOfWeekMs(epochMs: Long = now()): Long {
        val date = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
        val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
        return monday.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
