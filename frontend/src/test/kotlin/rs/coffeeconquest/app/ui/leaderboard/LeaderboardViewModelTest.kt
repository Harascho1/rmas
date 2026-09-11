package rs.coffeeconquest.app.ui.leaderboard

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import rs.coffeeconquest.app.data.CoffeeRepository
import rs.coffeeconquest.app.data.firebase.AppException
import rs.coffeeconquest.app.ui.MainDispatcherRule
import rs.coffeeconquest.app.ui.UiState
import rs.coffeeconquest.app.ui.dataOrNull
import rs.coffeeconquest.shared.dto.CityChampion
import rs.coffeeconquest.shared.dto.LeaderboardEntry
import rs.coffeeconquest.shared.dto.LeaderboardResponse
import rs.coffeeconquest.shared.model.LeaderboardScope

class LeaderboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<CoffeeRepository>()

    private val luka = entry(rank = 1, username = "luka", points = 340)
    private val mina = entry(rank = 2, username = "mina", points = 120)

    @Before
    fun boardWithTwoRows() {
        coEvery { repository.leaderboard(any(), any(), any(), any()) } returns
            LeaderboardResponse(scope = LeaderboardScope.GLOBAL, entries = listOf(luka, mina))
        coEvery { repository.cityChampion(any()) } returns
            CityChampion(city = "Nis", user = luka, weekStartEpochMs = 0)
    }

    @Test
    fun `start loads the global board and remembers the city`() {
        val viewModel = LeaderboardViewModel(repository)

        viewModel.start("Nis")

        assertEquals(listOf(luka, mina), viewModel.state.value.board.dataOrNull?.entries)
        assertEquals("Nis", viewModel.state.value.city)
    }

    @Test
    fun `the global scope asks for no city`() {
        val viewModel = LeaderboardViewModel(repository)
        viewModel.start("Nis")

        coVerify { repository.leaderboard(LeaderboardScope.GLOBAL, null, false, any()) }
    }

    @Test
    fun `switching to the city scope sends the city along`() {
        val viewModel = LeaderboardViewModel(repository)
        viewModel.start("Nis")

        viewModel.onScopeChange(LeaderboardScope.CITY)

        coVerify { repository.leaderboard(LeaderboardScope.CITY, "Nis", false, any()) }
    }

    @Test
    fun `the weekly toggle reloads with the window on`() {
        val viewModel = LeaderboardViewModel(repository)
        viewModel.start(null)

        viewModel.onPeriodChange(true)

        assertEquals(true, viewModel.state.value.weekly)
        coVerify { repository.leaderboard(any(), any(), true, any()) }
    }

    @Test
    fun `the champion is fetched for a known city and skipped without one`() {
        LeaderboardViewModel(repository).start("Nis")
        coVerify(exactly = 1) { repository.cityChampion("Nis") }

        val homeless = LeaderboardViewModel(repository)
        homeless.start(null)
        assertNull(homeless.state.value.champion)
    }

    @Test
    fun `a failing board becomes an error the screen can show`() {
        coEvery { repository.leaderboard(any(), any(), any(), any()) } throws
            AppException("Nemate dozvolu za ovu akciju.")
        val viewModel = LeaderboardViewModel(repository)

        viewModel.start(null)

        assertEquals(UiState.Error("Nemate dozvolu za ovu akciju."), viewModel.state.value.board)
    }

    private fun entry(rank: Int, username: String, points: Int) = LeaderboardEntry(
        rank = rank,
        userId = username,
        username = username,
        displayName = username.replaceFirstChar { it.uppercase() },
        points = points,
        level = 1,
        checkInCount = points / 10,
    )
}
