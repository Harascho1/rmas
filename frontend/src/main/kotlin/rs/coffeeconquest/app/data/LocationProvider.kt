package rs.coffeeconquest.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull

data class LatLon(val latitude: Double, val longitude: Double)

/** A position together with the provider that produced it. */
data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val source: Source,
    val accuracyMeters: Float?,
) {
    val position: LatLon get() = LatLon(latitude, longitude)

    /** Which of Android's two location providers answered. */
    enum class Source(val label: String) {
        /** Satellite fix: accurate to a few metres, needs sky and takes longer. */
        GPS("GPS"),

        /** Derived from cell towers and Wi-Fi: fast and works indoors, far coarser. */
        NETWORK("mreza"),
    }
}

/**
 * Reads the device position from **both** of Android's providers.
 *
 * GPS and network answer different questions: the network provider replies in
 * about a second and works indoors but can be hundreds of metres out, while GPS
 * is accurate enough for the 150 m check-in rule but may never answer inside a
 * cafe. So both are asked at once and GPS wins when it arrives in time.
 */
class LocationProvider(private val context: Context) {

    private val manager: LocationManager
        get() = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun hasPermission(): Boolean = hasFine() || hasCoarse()

    private fun hasFine(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasCoarse(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** The providers this device has switched on right now, for messages to the user. */
    fun enabledSources(): List<LocationFix.Source> = buildList {
        if (isEnabled(LocationManager.GPS_PROVIDER)) add(LocationFix.Source.GPS)
        if (isEnabled(LocationManager.NETWORK_PROVIDER)) add(LocationFix.Source.NETWORK)
    }

    private fun isEnabled(provider: String): Boolean =
        runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)

    /**
     * Listens to every available provider and returns the best fix it can get
     * within [timeoutMs].
     *
     * A GPS fix ends the wait immediately. A network fix is held for
     * [gpsGraceMs] first, in case GPS is about to answer with something better.
     * If nothing arrives at all, the last known position is used rather than
     * leaving the caller with nothing.
     */
    suspend fun current(timeoutMs: Long = 10_000, gpsGraceMs: Long = 4_000): LocationFix? {
        if (!hasPermission()) return null

        val fixes = Channel<LocationFix>(Channel.UNLIMITED)
        val listeners = mutableListOf<LocationListener>()

        // Fine location covers both providers; with only coarse granted, GPS is
        // unavailable and the network provider answers on its own.
        val requested = buildList {
            if (hasFine() && isEnabled(LocationManager.GPS_PROVIDER)) {
                add(LocationManager.GPS_PROVIDER to LocationFix.Source.GPS)
            }
            if (isEnabled(LocationManager.NETWORK_PROVIDER)) {
                add(LocationManager.NETWORK_PROVIDER to LocationFix.Source.NETWORK)
            }
        }
        if (requested.isEmpty()) return lastKnown()

        try {
            for ((provider, source) in requested) {
                val listener = LocationListener { location ->
                    fixes.trySend(location.toFix(source))
                }
                try {
                    manager.requestLocationUpdates(
                        provider,
                        0L,
                        0f,
                        listener,
                        Looper.getMainLooper(),
                    )
                    listeners += listener
                } catch (_: SecurityException) {
                    // Permission was revoked between the check and the request.
                }
            }
            if (listeners.isEmpty()) return lastKnown()

            val first = withTimeoutOrNull(timeoutMs) { fixes.receive() } ?: return lastKnown()
            if (first.source == LocationFix.Source.GPS) return first

            // The first answer came from the network; wait a moment for GPS.
            val better = withTimeoutOrNull(gpsGraceMs) { fixes.receive() }
            return better ?: first
        } finally {
            listeners.forEach { listener ->
                runCatching { manager.removeUpdates(listener) }
            }
            fixes.close()
        }
    }

    /** The freshest cached position from either provider - instant, possibly stale. */
    fun lastKnown(): LocationFix? {
        if (!hasPermission()) return null
        val candidates = buildList {
            if (hasFine()) add(LocationManager.GPS_PROVIDER to LocationFix.Source.GPS)
            add(LocationManager.NETWORK_PROVIDER to LocationFix.Source.NETWORK)
        }
        return candidates
            .mapNotNull { (provider, source) ->
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()?.toFix(source)
            }
            .minByOrNull { it.accuracyMeters ?: Float.MAX_VALUE }
    }

    private fun Location.toFix(source: LocationFix.Source) = LocationFix(
        latitude = latitude,
        longitude = longitude,
        source = source,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
    )

    companion object {
        /** Where the map opens when there is no fix yet: Republic Square, Belgrade. */
        val DEFAULT = LatLon(44.8167, 20.4600)
    }
}
