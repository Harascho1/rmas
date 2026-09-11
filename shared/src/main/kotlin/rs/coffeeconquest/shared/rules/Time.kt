package rs.coffeeconquest.shared.rules

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

    fun startOfWeekMs(epochMs: Long = now()): Long {
        val date = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
        val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
        return monday.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
