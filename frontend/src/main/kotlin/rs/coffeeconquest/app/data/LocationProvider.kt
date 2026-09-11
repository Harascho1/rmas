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

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val source: Source,
    val accuracyMeters: Float?,
) {
    val position: LatLon get() = LatLon(latitude, longitude)

    enum class Source(val label: String) {
        GPS("GPS"),
        NETWORK("mreza"),
    }
}

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

    fun enabledSources(): List<LocationFix.Source> = buildList {
        if (isEnabled(LocationManager.GPS_PROVIDER)) add(LocationFix.Source.GPS)
        if (isEnabled(LocationManager.NETWORK_PROVIDER)) add(LocationFix.Source.NETWORK)
    }

    private fun isEnabled(provider: String): Boolean =
        runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)

    suspend fun current(timeoutMs: Long = 10_000, gpsGraceMs: Long = 4_000): LocationFix? {
        if (!hasPermission()) return null

        val fixes = Channel<LocationFix>(Channel.UNLIMITED)
        val listeners = mutableListOf<LocationListener>()

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
                }
            }
            if (listeners.isEmpty()) return lastKnown()

            val first = withTimeoutOrNull(timeoutMs) { fixes.receive() } ?: return lastKnown()
            if (first.source == LocationFix.Source.GPS) return first

            val better = withTimeoutOrNull(gpsGraceMs) { fixes.receive() }
            return better ?: first
        } finally {
            listeners.forEach { listener ->
                runCatching { manager.removeUpdates(listener) }
            }
            fixes.close()
        }
    }

    fun lastKnown(): LocationFix? {
        if (!hasPermission()) return null
        val candidates = buildList {
            if (hasFine()) add(LocationManager.GPS_PROVIDER to LocationFix.Source.GPS)
            add(LocationManager.NETWORK_PROVIDER to LocationFix.Source.NETWORK)
        }
        return candidates
            .mapNotNull { (provider, source) ->
                try {
                    manager.getLastKnownLocation(provider)?.toFix(source)
                } catch (_: SecurityException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
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
        val DEFAULT = LatLon(43.3209, 21.8958)
    }
}
