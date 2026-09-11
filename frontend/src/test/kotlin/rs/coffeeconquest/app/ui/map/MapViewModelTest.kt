package rs.coffeeconquest.app.ui.map

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import rs.coffeeconquest.app.data.CoffeeRepository
import rs.coffeeconquest.app.data.LocationFix
import rs.coffeeconquest.app.data.LocationProvider
import rs.coffeeconquest.app.data.firebase.AppException
import rs.coffeeconquest.app.ui.MainDispatcherRule
import rs.coffeeconquest.app.ui.UiState
import rs.coffeeconquest.app.ui.cafe
import rs.coffeeconquest.app.ui.dataOrNull
import rs.coffeeconquest.shared.dto.Cafe
import rs.coffeeconquest.shared.model.CafeType

class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<CoffeeRepository>()
    private val location = mockk<LocationProvider>()

    private val kafeteriJa = cafe("c1", "Kafeterija")
    private val bigmaJa = cafe("c2", "Bigmajstor")

    @Before
    fun noFixByDefault() {
        coEvery { location.current(any(), any()) } returns null
    }

    @Test
    fun `cafes from the repository end up in state`() {
        streamsBack(flowOf(listOf(kafeteriJa, bigmaJa)))

        val viewModel = MapViewModel(repository, location)

        assertEquals(listOf(kafeteriJa, bigmaJa), viewModel.state.value.cafes.dataOrNull)
    }

    /**
     * The point of the whole Flow refactor: nobody calls load() a second time,
     * the data layer pushes and the screen follows.
     */
    @Test
    fun `a later emission reaches the screen with no second call`() {
        val stream = MutableSharedFlow<List<Cafe>>(replay = 1)
        stream.tryEmit(listOf(kafeteriJa))
        streamsBack(stream)

        val viewModel = MapViewModel(repository, location)
        assertEquals(1, viewModel.state.value.cafes.dataOrNull?.size)

        stream.tryEmit(listOf(kafeteriJa, bigmaJa))

        assertEquals(2, viewModel.state.value.cafes.dataOrNull?.size)
    }

    @Test
    fun `a failing stream becomes an error message, not a crash`() {
        streamsBack(flow { throw AppException("Server nije dostupan.") })

        val viewModel = MapViewModel(repository, location)

        assertEquals(UiState.Error("Server nije dostupan."), viewModel.state.value.cafes)
    }

    @Test
    fun `a location fix moves the centre and is labelled with its source`() {
        coEvery { location.current(any(), any()) } returns LocationFix(
            latitude = 43.3209,
            longitude = 21.8958,
            source = LocationFix.Source.GPS,
            accuracyMeters = 8f,
        )
        streamsBack(flowOf(emptyList()))

        val state = MapViewModel(repository, location).state.value

        assertTrue(state.hasFix)
        assertEquals(43.3209, state.center.latitude, 0.0)
        assertEquals(LocationFix.Source.GPS, state.fixSource)
    }

    @Test
    fun `without a fix the map stays on the default centre`() {
        streamsBack(flowOf(emptyList()))

        val state = MapViewModel(repository, location).state.value

        assertEquals(LocationProvider.DEFAULT, state.center)
        assertEquals(false, state.hasFix)
    }

    @Test
    fun `toggling an attribute adds it once and removes it again`() {
        streamsBack(flowOf(emptyList()))
        val viewModel = MapViewModel(repository, location)

        viewModel.toggleAttribute("wifi")
        assertEquals(listOf("wifi"), viewModel.state.value.filter.attributes)

        viewModel.toggleAttribute("wifi")
        assertEquals(emptyList<String>(), viewModel.state.value.filter.attributes)
    }

    @Test
    fun `resetting filters keeps the typed query`() {
        streamsBack(flowOf(emptyList()))
        val viewModel = MapViewModel(repository, location)

        viewModel.onQueryChange("bigmajstor")
        viewModel.onTypeChange(CafeType.KAFIC)
        viewModel.onOnlyMineChange(true)

        viewModel.resetFilters()

        val filter = viewModel.state.value.filter
        assertEquals("bigmajstor", filter.query)
        assertEquals(false, filter.isActive)
    }

    /** Every search goes through the same stream, whatever the filter says. */
    private fun streamsBack(stream: kotlinx.coroutines.flow.Flow<List<Cafe>>) {
        every {
            repository.cafesStream(any(), any(), any(), any(), any(), any())
        } returns stream
    }
}
