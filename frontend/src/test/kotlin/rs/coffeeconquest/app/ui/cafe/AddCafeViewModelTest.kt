package rs.coffeeconquest.app.ui.cafe

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import rs.coffeeconquest.app.data.AddressLookup
import rs.coffeeconquest.app.data.CoffeeRepository
import rs.coffeeconquest.app.data.LatLon
import rs.coffeeconquest.app.data.LocationFix
import rs.coffeeconquest.app.data.LocationProvider
import rs.coffeeconquest.app.data.ResolvedAddress
import rs.coffeeconquest.app.data.firebase.AppException
import rs.coffeeconquest.app.ui.MainDispatcherRule
import rs.coffeeconquest.app.ui.cafe
import rs.coffeeconquest.shared.model.CafeType

class AddCafeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<CoffeeRepository>()
    private val location = mockk<LocationProvider>()
    private val addresses = mockk<AddressLookup>()

    private fun viewModel() = AddCafeViewModel(repository, location, addresses)

    @Before
    fun creationSucceeds() {
        coEvery { repository.createCafe(any()) } returns cafe("c1")
        coEvery { location.current(any(), any()) } returns null
        coEvery { addresses.reverse(any()) } returns null
    }

    @Test
    fun `a name that is too short keeps the button disabled`() {
        val viewModel = viewModel()

        viewModel.onNameChange("K")

        assertFalse(viewModel.state.value.canSubmit)
    }

    @Test
    fun `a valid name enables the button`() {
        val viewModel = viewModel()

        viewModel.onNameChange("Kafeterija")

        assertTrue(viewModel.state.value.canSubmit)
    }

    @Test
    fun `submitting an invalid name never reaches the repository`() {
        val viewModel = viewModel()
        viewModel.onNameChange("K")

        viewModel.submit()

        assertEquals("Naziv kafica mora imati 2-80 karaktera.", viewModel.state.value.error)
        coVerify(exactly = 0) { repository.createCafe(any()) }
    }

    /** Chip choices and hand-typed tags end up in one list, without duplicates. */
    @Test
    fun `chosen attributes and typed tags are merged and deduplicated`() {
        val viewModel = viewModel()
        viewModel.onNameChange("Kafeterija")
        viewModel.toggleAttribute("wifi")
        viewModel.toggleAttribute("terasa")
        viewModel.onTagsChange("terasa, pet friendly ,")

        viewModel.submit()

        coVerify {
            repository.createCafe(
                match { it.tags == listOf("wifi", "terasa", "pet friendly") },
            )
        }
    }

    @Test
    fun `blank optional fields are sent as null rather than empty strings`() {
        val viewModel = viewModel()
        viewModel.onNameChange("  Kafeterija  ")
        viewModel.onAddressChange("   ")

        viewModel.submit()

        coVerify {
            repository.createCafe(
                match {
                    it.name == "Kafeterija" &&
                        it.address == null &&
                        it.city == null &&
                        it.description == null
                },
            )
        }
        assertTrue(viewModel.state.value.created)
    }

    @Test
    fun `toggling an attribute twice removes it`() {
        val viewModel = viewModel()

        viewModel.toggleAttribute("wifi")
        viewModel.toggleAttribute("wifi")

        assertEquals(emptyList<String>(), viewModel.state.value.attributes)
    }

    @Test
    fun `the chosen type is carried through`() {
        val viewModel = viewModel()
        viewModel.onNameChange("Przionica")
        viewModel.onTypeChange(CafeType.PRZIONICA)

        viewModel.submit()

        coVerify { repository.createCafe(match { it.type == CafeType.PRZIONICA }) }
    }

    @Test
    fun `a location fix replaces the default coordinates`() {
        coEvery { location.current(any(), any()) } returns LocationFix(
            latitude = 43.3209,
            longitude = 21.8958,
            source = LocationFix.Source.GPS,
            accuracyMeters = 5f,
        )
        val viewModel = viewModel()

        viewModel.useCurrentLocation()

        assertEquals(43.3209, viewModel.state.value.latitude, 0.0)
        assertEquals(21.8958, viewModel.state.value.longitude, 0.0)
    }

    /** Without a fix the pin must not silently stay on Belgrade as if it were real. */
    @Test
    fun `no fix is reported instead of guessed`() {
        val viewModel = viewModel()

        viewModel.useCurrentLocation()

        assertEquals(
            "Lokacija nije dostupna - proverite dozvolu i GPS.",
            viewModel.state.value.error,
        )
        assertEquals(LocationProvider.DEFAULT.latitude, viewModel.state.value.latitude, 0.0)
    }

    @Test
    fun `a duplicate cafe is reported and the form stays open`() {
        coEvery { repository.createCafe(any()) } throws AppException("Taj kafic je vec na mapi.")
        val viewModel = viewModel()
        viewModel.onNameChange("Kafeterija")

        viewModel.submit()

        assertEquals("Taj kafic je vec na mapi.", viewModel.state.value.error)
        assertFalse(viewModel.state.value.created)
        assertFalse(viewModel.state.value.submitting)
    }

    @Test
    fun `a pin dropped on the map becomes the coordinates that get saved`() {
        val viewModel = viewModel()
        viewModel.onNameChange("Kafeterija")

        viewModel.onMapPick(LatLon(43.3209, 21.8958))
        viewModel.submit()

        coVerify {
            repository.createCafe(match { it.latitude == 43.3209 && it.longitude == 21.8958 })
        }
    }

    @Test
    fun `the pin fills in address and city from the reverse lookup`() {
        coEvery { addresses.reverse(LatLon(43.3209, 21.8958)) } returns
            ResolvedAddress(street = "Obrenoviceva 10", city = "Nis")
        val viewModel = viewModel()

        viewModel.onMapPick(LatLon(43.3209, 21.8958))

        assertEquals("Obrenoviceva 10", viewModel.state.value.address)
        assertEquals("Nis", viewModel.state.value.city)
        assertFalse(viewModel.state.value.resolvingAddress)
    }

    /** A pin dropped next door must not wipe an address the user knows is right. */
    @Test
    fun `a hand-typed address survives the reverse lookup`() {
        coEvery { addresses.reverse(any()) } returns
            ResolvedAddress(street = "Obrenoviceva 10", city = "Nis")
        val viewModel = viewModel()
        viewModel.onAddressChange("Kod stare cesme bb")

        viewModel.onMapPick(LatLon(43.3209, 21.8958))

        assertEquals("Kod stare cesme bb", viewModel.state.value.address)
        // The city was never touched, so it still follows the pin.
        assertEquals("Nis", viewModel.state.value.city)
    }

    @Test
    fun `a lookup that fails leaves the fields alone instead of crashing`() {
        coEvery { addresses.reverse(any()) } throws IllegalStateException("offline")
        val viewModel = viewModel()

        viewModel.onMapPick(LatLon(43.3209, 21.8958))

        assertEquals("", viewModel.state.value.address)
        assertFalse(viewModel.state.value.resolvingAddress)
        assertEquals(43.3209, viewModel.state.value.latitude, 0.0)
    }

    @Test
    fun `only a location fix recentres the map, a pin does not`() {
        coEvery { location.current(any(), any()) } returns LocationFix(
            latitude = 44.7866,
            longitude = 20.4489,
            source = LocationFix.Source.GPS,
            accuracyMeters = 5f,
        )
        val viewModel = viewModel()
        viewModel.useCurrentLocation()

        viewModel.onMapPick(LatLon(43.3209, 21.8958))

        assertEquals(LatLon(44.7866, 20.4489), viewModel.state.value.mapCenter)
        assertEquals(LatLon(43.3209, 21.8958), viewModel.state.value.pin)
    }
}
