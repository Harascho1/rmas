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
import rs.coffeeconquest.app.data.CoffeeRepository
import rs.coffeeconquest.app.data.LocationFix
import rs.coffeeconquest.app.data.LocationProvider
import rs.coffeeconquest.app.data.firebase.AppException
import rs.coffeeconquest.app.ui.MainDispatcherRule
import rs.coffeeconquest.app.ui.cafe
import rs.coffeeconquest.shared.model.CafeType

class AddCafeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<CoffeeRepository>()
    private val location = mockk<LocationProvider>()

    @Before
    fun creationSucceeds() {
        coEvery { repository.createCafe(any()) } returns cafe("c1")
        coEvery { location.current(any(), any()) } returns null
    }

    @Test
    fun `a name that is too short keeps the button disabled`() {
        val viewModel = AddCafeViewModel(repository, location)

        viewModel.onNameChange("K")

        assertFalse(viewModel.state.value.canSubmit)
    }

    @Test
    fun `a valid name enables the button`() {
        val viewModel = AddCafeViewModel(repository, location)

        viewModel.onNameChange("Kafeterija")

        assertTrue(viewModel.state.value.canSubmit)
    }

    @Test
    fun `submitting an invalid name never reaches the repository`() {
        val viewModel = AddCafeViewModel(repository, location)
        viewModel.onNameChange("K")

        viewModel.submit()

        assertEquals("Naziv kafica mora imati 2-80 karaktera.", viewModel.state.value.error)
        coVerify(exactly = 0) { repository.createCafe(any()) }
    }

    /** Chip choices and hand-typed tags end up in one list, without duplicates. */
    @Test
    fun `chosen attributes and typed tags are merged and deduplicated`() {
        val viewModel = AddCafeViewModel(repository, location)
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
        val viewModel = AddCafeViewModel(repository, location)
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
        val viewModel = AddCafeViewModel(repository, location)

        viewModel.toggleAttribute("wifi")
        viewModel.toggleAttribute("wifi")

        assertEquals(emptyList<String>(), viewModel.state.value.attributes)
    }

    @Test
    fun `the chosen type is carried through`() {
        val viewModel = AddCafeViewModel(repository, location)
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
        val viewModel = AddCafeViewModel(repository, location)

        viewModel.useCurrentLocation()

        assertEquals(43.3209, viewModel.state.value.latitude, 0.0)
        assertEquals(21.8958, viewModel.state.value.longitude, 0.0)
    }

    /** Without a fix the pin must not silently stay on Belgrade as if it were real. */
    @Test
    fun `no fix is reported instead of guessed`() {
        val viewModel = AddCafeViewModel(repository, location)

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
        val viewModel = AddCafeViewModel(repository, location)
        viewModel.onNameChange("Kafeterija")

        viewModel.submit()

        assertEquals("Taj kafic je vec na mapi.", viewModel.state.value.error)
        assertFalse(viewModel.state.value.created)
        assertFalse(viewModel.state.value.submitting)
    }
}
