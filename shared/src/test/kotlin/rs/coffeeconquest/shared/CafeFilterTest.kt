package rs.coffeeconquest.shared

import rs.coffeeconquest.shared.dto.CafeFilter
import rs.coffeeconquest.shared.model.CafeAttributes
import rs.coffeeconquest.shared.model.CafeType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CafeFilterTest {

    @Test
    fun `an empty filter is not active`() {
        assertFalse(CafeFilter().isActive)
        assertEquals(0, CafeFilter().activeCount)
    }

    @Test
    fun `free text alone does not count as a filter`() {
        val filter = CafeFilter(query = "dorcol")
        assertFalse(filter.isActive)
        assertEquals(0, filter.activeCount)
    }

    @Test
    fun `each narrowing choice is counted once`() {
        val filter = CafeFilter(
            type = CafeType.PRZIONICA,
            attributes = listOf(CafeAttributes.WIFI, CafeAttributes.TERASA),
            authorUsername = "marko",
            addedWithinDays = 30,
            minRating = 4.0,
        )
        assertEquals(5, filter.activeCount)
        assertTrue(filter.isActive)
    }

    @Test
    fun `only mine counts as an author filter`() {
        assertEquals(1, CafeFilter(onlyMine = true).activeCount)
    }

    @Test
    fun `date presets start with any time`() {
        assertEquals(null, CafeFilter.DATE_PRESETS.first().second)
        assertTrue(CafeFilter.DATE_PRESETS.all { (label, _) -> label.isNotBlank() })
    }
}
