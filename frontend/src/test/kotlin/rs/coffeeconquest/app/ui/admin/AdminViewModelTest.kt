package rs.coffeeconquest.app.ui.admin

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
import rs.coffeeconquest.app.ui.cafe
import rs.coffeeconquest.app.ui.checkIn
import rs.coffeeconquest.app.ui.dataOrNull
import rs.coffeeconquest.app.ui.profile
import rs.coffeeconquest.shared.model.Role

class AdminViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<CoffeeRepository>(relaxed = true)

    @Before
    fun emptyQueues() {
        coEvery { repository.pendingCafes() } returns listOf(cafe("c1"))
        coEvery { repository.flaggedCheckIns() } returns listOf(checkIn("ci1"))
        coEvery { repository.allUsers() } returns listOf(profile())
    }

    /** Each tab loads only its own queue - the other two are not paid for. */
    @Test
    fun `the cafes tab loads pending cafes and nothing else`() {
        val viewModel = AdminViewModel(repository)

        viewModel.load()

        assertEquals(1, viewModel.state.value.pendingCafes.dataOrNull?.size)
        coVerify(exactly = 0) { repository.flaggedCheckIns() }
        coVerify(exactly = 0) { repository.allUsers() }
    }

    @Test
    fun `switching to the users tab loads the users`() {
        val viewModel = AdminViewModel(repository)

        viewModel.onTabChange(AdminTab.USERS)

        assertEquals(AdminTab.USERS, viewModel.state.value.tab)
        assertEquals(1, viewModel.state.value.users.dataOrNull?.size)
    }

    @Test
    fun `approving a cafe reports it and reloads the queue`() {
        val viewModel = AdminViewModel(repository)
        viewModel.load()

        viewModel.moderateCafe("c1", approve = true)

        assertEquals("Kafic je odobren.", viewModel.state.value.message)
        assertEquals(false, viewModel.state.value.busy)
        coVerify { repository.moderateCafe("c1", true) }
        coVerify(exactly = 2) { repository.pendingCafes() }
    }

    @Test
    fun `rejecting a cafe says so instead`() {
        val viewModel = AdminViewModel(repository)

        viewModel.moderateCafe("c1", approve = false)

        assertEquals("Kafic je odbijen.", viewModel.state.value.message)
    }

    /** An invalidated check-in carries a reason, so the hunter can be told why. */
    @Test
    fun `invalidating a check-in sends a reason along`() {
        val viewModel = AdminViewModel(repository)
        viewModel.onTabChange(AdminTab.CHECK_INS)

        viewModel.moderateCheckIn("ci1", valid = false)

        coVerify { repository.invalidateCheckIn("ci1", "Sumnja na varanje.") }
        assertEquals("Check-in je ponisten.", viewModel.state.value.message)
    }

    @Test
    fun `approving a check-in awards the points`() {
        val viewModel = AdminViewModel(repository)

        viewModel.moderateCheckIn("ci1", valid = true)

        coVerify { repository.approveCheckIn("ci1") }
        assertEquals("Check-in je odobren, poeni su dodeljeni.", viewModel.state.value.message)
    }

    @Test
    fun `banning and unbanning differ in message and reason`() {
        val viewModel = AdminViewModel(repository)

        viewModel.setBanned("u1", banned = true)
        assertEquals("Korisnik je blokiran.", viewModel.state.value.message)
        coVerify { repository.banUser("u1", true, "Krsenje pravila.") }

        viewModel.setBanned("u1", banned = false)
        assertEquals("Blokada je uklonjena.", viewModel.state.value.message)
        coVerify { repository.banUser("u1", false, "") }
    }

    @Test
    fun `changing a role names the new role in the message`() {
        val viewModel = AdminViewModel(repository)

        viewModel.setRole("u1", Role.OWNER)

        coVerify { repository.changeRole("u1", Role.OWNER) }
        assertEquals("Rola je promenjena u OWNER.", viewModel.state.value.message)
    }

    @Test
    fun `a refused action reports the reason and stops being busy`() {
        coEvery { repository.moderateCafe(any(), any(), any()) } throws
            AppException("Nemate dozvolu za ovu akciju.")
        val viewModel = AdminViewModel(repository)

        viewModel.moderateCafe("c1", approve = true)

        assertEquals("Nemate dozvolu za ovu akciju.", viewModel.state.value.message)
        assertEquals(false, viewModel.state.value.busy)
    }

    @Test
    fun `a failing queue becomes an error in that tab only`() {
        coEvery { repository.pendingCafes() } throws AppException("Server nije dostupan.")
        val viewModel = AdminViewModel(repository)

        viewModel.load()

        assertEquals(UiState.Error("Server nije dostupan."), viewModel.state.value.pendingCafes)
        assertEquals(UiState.Loading, viewModel.state.value.users)
    }

    @Test
    fun `dismissing clears the message`() {
        val viewModel = AdminViewModel(repository)
        viewModel.moderateCafe("c1", approve = true)

        viewModel.dismissMessage()

        assertNull(viewModel.state.value.message)
    }
}
