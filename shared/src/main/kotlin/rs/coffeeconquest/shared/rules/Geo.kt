package rs.coffeeconquest.shared.rules

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {

    private const val EARTH_RADIUS_M = 6_371_000.0

    const val MAX_CHECKIN_DISTANCE_M = 150.0

    const val MAX_PLAUSIBLE_SPEED_KMH = 130.0

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_M * asin(min(1.0, sqrt(a)))
    }

    fun boundingBox(lat: Double, lon: Double, radiusMeters: Double): BoundingBox {
        val latDelta = Math.toDegrees(radiusMeters / EARTH_RADIUS_M)
        val lonDelta = Math.toDegrees(radiusMeters / (EARTH_RADIUS_M * cos(Math.toRadians(lat)).coerceAtLeast(1e-6)))
        return BoundingBox(lat - latDelta, lat + latDelta, lon - lonDelta, lon + lonDelta)
    }

    data class BoundingBox(val minLat: Double, val maxLat: Double, val minLon: Double, val maxLon: Double)

    fun isValidCoordinate(lat: Double, lon: Double): Boolean =
        lat in -90.0..90.0 && lon in -180.0..180.0
}
