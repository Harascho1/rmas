package rs.coffeeconquest.app.ui

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import rs.coffeeconquest.app.data.CoffeeRepository
import rs.coffeeconquest.app.data.firebase.AppException

class SessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<CoffeeRepository>(relaxed = true)

    @Test
    fun `no stored session means the auth screen`() {
        every { repository.isSignedIn() } returns false

        val viewModel = SessionViewModel(repository)

        assertEquals(SessionState.SignedOut, viewModel.state.value)
    }

    @Test
    fun `a stored session is resumed without asking for a password`() {
        val luka = profile()
        every { repository.isSignedIn() } returns true
        coEvery { repository.me() } returns luka

        val viewModel = SessionViewModel(repository)

        assertEquals(SessionState.SignedIn(luka), viewModel.state.value)
    }

    /** A stored session whose profile is gone is worse than none: sign the user out. */
    @Test
    fun `a session that cannot be resumed is thrown away`() {
        every { repository.isSignedIn() } returns true
        coEvery { repository.me() } throws AppException("Korisnik ne postoji.")

        val viewModel = SessionViewModel(repository)

        assertEquals(SessionState.SignedOut, viewModel.state.value)
        verify { repository.logout() }
    }

    @Test
    fun `signing in from the auth screen needs no round trip`() {
        every { repository.isSignedIn() } returns false
        val viewModel = SessionViewModel(repository)
        val luka = profile()

        viewModel.onSignedIn(luka)

        assertEquals(SessionState.SignedIn(luka), viewModel.state.value)
    }

    @Test
    fun `refreshProfile replaces the profile after points change`() {
        every { repository.isSignedIn() } returns true
        coEvery { repository.me() } returns profile(username = "luka")
        val viewModel = SessionViewModel(repository)

        val richer = profile(username = "luka").copy(points = 340)
        coEvery { repository.me() } returns richer
        viewModel.refreshProfile()

        assertEquals(SessionState.SignedIn(richer), viewModel.state.value)
    }

    /** A failed refresh must not log the user out mid-session. */
    @Test
    fun `a failed refresh leaves the session alone`() {
        val luka = profile()
        every { repository.isSignedIn() } returns true
        coEvery { repository.me() } returns luka
        val viewModel = SessionViewModel(repository)

        coEvery { repository.me() } throws AppException("Server nije dostupan.")
        viewModel.refreshProfile()

        assertEquals(SessionState.SignedIn(luka), viewModel.state.value)
    }

    @Test
    fun `signing out clears the session and tells the repository`() {
        every { repository.isSignedIn() } returns true
        coEvery { repository.me() } returns profile()
        val viewModel = SessionViewModel(repository)

        viewModel.signOut()

        assertEquals(SessionState.SignedOut, viewModel.state.value)
        verify { repository.logout() }
    }
}
