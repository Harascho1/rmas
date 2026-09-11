package rs.coffeeconquest.app.ui.profile

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
import rs.coffeeconquest.app.data.firebase.AppException
import rs.coffeeconquest.app.ui.MainDispatcherRule
import rs.coffeeconquest.app.ui.UiState
import rs.coffeeconquest.app.ui.badge
import rs.coffeeconquest.app.ui.checkIn
import rs.coffeeconquest.app.ui.profile
import rs.coffeeconquest.app.ui.stats
import rs.coffeeconquest.shared.dto.FollowResponse
import rs.coffeeconquest.shared.model.Badges

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<CoffeeRepository>()

    @Before
    fun aProfileWithHistory() {
        coEvery { repository.userStats(any()) } returns stats()
        coEvery { repository.myCheckIns(any()) } returns listOf(checkIn("ci1"))
        coEvery { repository.userCheckIns(any(), any()) } returns listOf(checkIn("ci2"))
        coEvery { repository.follow(any(), any()) } returns
            FollowResponse(following = true, followerCount = 1)
    }

    /** My own history comes from a different query than someone else's. */
    @Test
    fun `my own profile reads my check-ins`() {
        val viewModel = ProfileViewModel(repository)

        viewModel.load("u1", ownProfile = true)

        coVerify { repository.myCheckIns(any()) }
        coVerify(exactly = 0) { repository.userCheckIns(any(), any()) }
        assertEquals(listOf(checkIn("ci1")), viewModel.state.value.history)
    }

    @Test
    fun `someone else's profile reads their check-ins`() {
        val viewModel = ProfileViewModel(repository)

        viewModel.load("u2", ownProfile = false)

        coVerify { repository.userCheckIns("u2", any()) }
        assertEquals(listOf(checkIn("ci2")), viewModel.state.value.history)
    }

    /** The board shows every badge in the game, earned or not. */
    @Test
    fun `the badge board covers all badges and marks the earned ones`() {
        val earned = Badges.all.first().code
        coEvery { repository.userStats(any()) } returns stats(badges = listOf(badge(earned)))
        val viewModel = ProfileViewModel(repository)

        viewModel.load("u1", ownProfile = true)

        val board = viewModel.state.value.badgeBoard
        assertEquals(Badges.all.size, board.size)
        assertTrue(board.first { it.code == earned }.earned)
        assertFalse(board.first { it.code != earned }.earned)
    }

    @Test
    fun `every badge slot carries a requirement`() {
        val viewModel = ProfileViewModel(repository)
        viewModel.load("u1", ownProfile = true)

        val board = viewModel.state.value.badgeBoard

        assertTrue(board.all { it.requirement.isNotBlank() })
        assertEquals(
            Badges.find(Badges.EXPLORER_5)?.requirement,
            board.first { it.code == Badges.EXPLORER_5 }.requirement,
        )
    }

    @Test
    fun `selecting a badge opens its slot, and null closes it`() {
        val earned = Badges.FIRST_SIP
        coEvery { repository.userStats(any()) } returns stats(badges = listOf(badge(earned)))
        val viewModel = ProfileViewModel(repository)
        viewModel.load("u1", ownProfile = true)

        viewModel.selectBadge(earned)

        val selected = viewModel.state.value.selectedBadge
        assertEquals(earned, selected?.code)
        assertTrue(selected!!.earned)

        viewModel.selectBadge(null)
        assertEquals(null, viewModel.state.value.selectedBadge)
    }

    @Test
    fun `an unearned badge can be selected too`() {
        val viewModel = ProfileViewModel(repository)
        viewModel.load("u1", ownProfile = true)

        viewModel.selectBadge(Badges.STREAK_30)

        val selected = viewModel.state.value.selectedBadge
        assertFalse(selected!!.earned)
        assertEquals(null, selected.earnedAtEpochMs)
        assertTrue(selected.requirement.isNotBlank())
    }

    @Test
    fun `a code that is not in the catalogue selects nothing`() {
        val viewModel = ProfileViewModel(repository)
        viewModel.load("u1", ownProfile = true)

        viewModel.selectBadge("NE_POSTOJI")

        assertEquals(null, viewModel.state.value.selectedBadge)
    }

    @Test
    fun `following flips the state and reloads the profile`() {
        coEvery { repository.userStats("u2") } returns
            stats(profile = profile(id = "u2", username = "mina", isFollowedByMe = false))
        val viewModel = ProfileViewModel(repository)
        viewModel.load("u2", ownProfile = false)

        viewModel.toggleFollow()

        coVerify { repository.follow("u2", true) }
        assertEquals(false, viewModel.state.value.followBusy)
        coVerify(exactly = 2) { repository.userStats("u2") }
    }

    @Test
    fun `unfollowing sends the opposite flag`() {
        coEvery { repository.userStats("u2") } returns
            stats(profile = profile(id = "u2", username = "mina", isFollowedByMe = true))
        val viewModel = ProfileViewModel(repository)
        viewModel.load("u2", ownProfile = false)

        viewModel.toggleFollow()

        coVerify { repository.follow("u2", false) }
    }

    /** Nothing to follow until the profile has actually loaded. */
    @Test
    fun `toggleFollow does nothing before the profile is ready`() {
        val viewModel = ProfileViewModel(repository)

        viewModel.toggleFollow()

        coVerify(exactly = 0) { repository.follow(any(), any()) }
    }

    @Test
    fun `a refused follow reports the reason and stops being busy`() {
        coEvery { repository.follow(any(), any()) } throws AppException("Nalog je blokiran.")
        val viewModel = ProfileViewModel(repository)
        viewModel.load("u2", ownProfile = false)

        viewModel.toggleFollow()

        assertEquals("Nalog je blokiran.", viewModel.state.value.message)
        assertEquals(false, viewModel.state.value.followBusy)
    }

    @Test
    fun `a missing user becomes an error, and history stays empty`() {
        coEvery { repository.userStats(any()) } throws AppException("Korisnik ne postoji.")
        coEvery { repository.myCheckIns(any()) } returns emptyList()
        val viewModel = ProfileViewModel(repository)

        viewModel.load("nema", ownProfile = true)

        assertEquals(UiState.Error("Korisnik ne postoji."), viewModel.state.value.stats)
        assertEquals(emptyList<Any>(), viewModel.state.value.history)
    }
}
