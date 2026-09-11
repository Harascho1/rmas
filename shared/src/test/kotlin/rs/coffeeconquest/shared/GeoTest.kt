package rs.coffeeconquest.shared

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import rs.coffeeconquest.shared.rules.Geo

class GeoTest {

    @Test
    fun `distance to itself is zero`() {
        assertEquals(0.0, Geo.distanceMeters(44.8189, 20.4562, 44.8189, 20.4562), 0.001)
    }

    @Test
    fun `belgrade to nis is roughly 200 km`() {
        val meters = Geo.distanceMeters(44.8189, 20.4562, 43.3199, 21.8958)
        assertTrue(meters in 190_000.0..220_000.0, "bilo je $meters m")
    }

    @Test
    fun `a hundred meters away is still in check-in range`() {
        val meters = Geo.distanceMeters(44.8189, 20.4562, 44.8198, 20.4562)
        assertTrue(meters < Geo.MAX_CHECKIN_DISTANCE_M, "bilo je $meters m")
    }

    @Test
    fun `bounding box covers the requested radius`() {
        val box = Geo.boundingBox(44.8189, 20.4562, 1_000.0)
        val northEdge = Geo.distanceMeters(44.8189, 20.4562, box.maxLat, 20.4562)
        assertTrue(abs(northEdge - 1_000.0) < 5.0, "bilo je $northEdge m")
    }

    @Test
    fun `coordinates outside the globe are rejected`() {
        assertTrue(Geo.isValidCoordinate(44.8, 20.4))
        assertFalse(Geo.isValidCoordinate(91.0, 20.4))
        assertFalse(Geo.isValidCoordinate(44.8, 181.0))
    }
}
