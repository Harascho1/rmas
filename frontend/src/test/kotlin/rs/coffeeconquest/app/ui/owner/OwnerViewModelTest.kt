package rs.coffeeconquest.app.ui.owner

import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import rs.coffeeconquest.app.data.CoffeeRepository
import rs.coffeeconquest.app.data.firebase.AppException
import rs.coffeeconquest.app.ui.MainDispatcherRule
import rs.coffeeconquest.app.ui.UiState
import rs.coffeeconquest.app.ui.cafe
import rs.coffeeconquest.app.ui.dataOrNull

class OwnerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<CoffeeRepository>()

    @Test
    fun `the dashboard lists the cafes this account manages`() {
        val mine = listOf(cafe("c1"), cafe("c2", "Bigmajstor"))
        coEvery { repository.myCafes() } returns mine

        val viewModel = OwnerViewModel(repository)
        viewModel.load()

        assertEquals(mine, viewModel.state.value.cafes.dataOrNull)
    }

    @Test
    fun `an owner with no cafes gets an empty list, not an error`() {
        coEvery { repository.myCafes() } returns emptyList()

        val viewModel = OwnerViewModel(repository)
        viewModel.load()

        assertEquals(emptyList<Any>(), viewModel.state.value.cafes.dataOrNull)
    }

    @Test
    fun `a refused read becomes a message the screen can show`() {
        coEvery { repository.myCafes() } throws AppException("Nemate dozvolu za ovu akciju.")

        val viewModel = OwnerViewModel(repository)
        viewModel.load()

        assertEquals(UiState.Error("Nemate dozvolu za ovu akciju."), viewModel.state.value.cafes)
    }
}
