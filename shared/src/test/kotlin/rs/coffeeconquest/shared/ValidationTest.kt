package rs.coffeeconquest.shared

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import rs.coffeeconquest.shared.rules.Validation

class ValidationTest {

    @Test
    fun `usernames must be reasonable`() {
        assertNull(Validation.username("marko_92"))
        assertNotNull(Validation.username("ab"))
        assertNotNull(Validation.username("marko petrovic"))
    }

    @Test
    fun `emails need an at and a dot`() {
        assertNull(Validation.email("marko@coffee.rs"))
        assertNotNull(Validation.email("marko.coffee.rs"))
        assertNotNull(Validation.email("marko@coffee"))
    }

    @Test
    fun `passwords have a minimum length`() {
        assertNull(Validation.password("coffee123"))
        assertNotNull(Validation.password("kratka"))
    }

    @Test
    fun `ratings stay between one and five`() {
        assertNull(Validation.rating(5))
        assertNotNull(Validation.rating(0))
        assertNotNull(Validation.rating(6))
    }
}
