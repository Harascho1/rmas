package rs.coffeeconquest.app.ui.auth

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import rs.coffeeconquest.app.data.CoffeeRepository
import rs.coffeeconquest.app.data.firebase.AppException
import rs.coffeeconquest.app.ui.MainDispatcherRule
import rs.coffeeconquest.shared.dto.RegisterRequest
import rs.coffeeconquest.shared.dto.UserProfile
import rs.coffeeconquest.shared.model.Role

class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<CoffeeRepository>()

    private val luka = UserProfile(
        id = "u1",
        username = "luka",
        displayName = "Luka",
        role = Role.HUNTER,
    )

    @Test
    fun `login hands the profile to the caller`() {
        coEvery { repository.login("luka", "lozinka123") } returns luka
        val viewModel = AuthViewModel(repository)
        viewModel.onUsernameChange("luka")
        viewModel.onPasswordChange("lozinka123")

        var signedIn: UserProfile? = null
        viewModel.submit { signedIn = it }

        assertEquals(luka, signedIn)
        assertNull(viewModel.state.value.error)
        assertEquals(false, viewModel.state.value.submitting)
    }

    @Test
    fun `a rejected login shows the message and calls nobody back`() {
        coEvery { repository.login(any(), any()) } throws
            AppException("Pogresno korisnicko ime ili lozinka.")
        val viewModel = AuthViewModel(repository)
        viewModel.onUsernameChange("luka")
        viewModel.onPasswordChange("pogresna1")

        var called = false
        viewModel.submit { called = true }

        assertEquals(false, called)
        assertEquals("Pogresno korisnicko ime ili lozinka.", viewModel.state.value.error)
        assertEquals(false, viewModel.state.value.submitting)
    }

    @Test
    fun `an invalid form is never sent`() {
        val viewModel = AuthViewModel(repository)
        viewModel.toggleMode()
        viewModel.onUsernameChange("luka")
        viewModel.onEmailChange("luka@example.com")
        viewModel.onPasswordChange("kratka")
        viewModel.onDisplayNameChange("Luka")

        viewModel.submit { }

        assertEquals("Lozinka mora imati bar 8 karaktera.", viewModel.state.value.error)
        coVerify(exactly = 0) { repository.register(any()) }
    }

    @Test
    fun `registration trims what the user typed and keeps the chosen role`() {
        coEvery { repository.register(any()) } returns luka
        val viewModel = AuthViewModel(repository)
        viewModel.toggleMode()
        viewModel.onUsernameChange("luka")
        viewModel.onEmailChange("luka@example.com")
        viewModel.onPasswordChange("lozinka123")
        viewModel.onDisplayNameChange("  Luka  ")
        viewModel.onCityChange("  Nis  ")
        viewModel.onRoleChange(Role.OWNER)

        viewModel.submit { }

        coVerify {
            repository.register(
                RegisterRequest(
                    username = "luka",
                    email = "luka@example.com",
                    password = "lozinka123",
                    displayName = "Luka",
                    city = "Nis",
                    role = Role.OWNER,
                ),
            )
        }
    }

    @Test
    fun `an empty city is sent as null rather than an empty string`() {
        coEvery { repository.register(any()) } returns luka
        val viewModel = AuthViewModel(repository)
        viewModel.toggleMode()
        viewModel.onUsernameChange("luka")
        viewModel.onEmailChange("luka@example.com")
        viewModel.onPasswordChange("lozinka123")
        viewModel.onDisplayNameChange("Luka")

        viewModel.submit { }

        coVerify { repository.register(match { it.city == null }) }
    }

    @Test
    fun `switching to registration clears a stale error`() {
        coEvery { repository.login(any(), any()) } throws AppException("Nalog je blokiran.")
        val viewModel = AuthViewModel(repository)
        viewModel.onUsernameChange("luka")
        viewModel.onPasswordChange("lozinka123")
        viewModel.submit { }
        assertTrue(viewModel.state.value.error != null)

        viewModel.toggleMode()

        assertNull(viewModel.state.value.error)
        assertTrue(viewModel.state.value.register)
    }
}
