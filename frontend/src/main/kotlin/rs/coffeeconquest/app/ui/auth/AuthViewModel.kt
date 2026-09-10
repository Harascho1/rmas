package rs.coffeeconquest.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.coffeeconquest.app.data.AppContainer
import rs.coffeeconquest.app.data.firebase.userMessage
import rs.coffeeconquest.shared.dto.RegisterRequest
import rs.coffeeconquest.shared.dto.UserProfile
import rs.coffeeconquest.shared.model.Role
import rs.coffeeconquest.shared.rules.Validation

data class AuthUiState(
    val register: Boolean = false,
    val usernameOrEmail: String = "",
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val city: String = "",
    val role: Role = Role.HUNTER,
    val submitting: Boolean = false,
    val error: String? = null,
) {
    /** Client-side mirror of the server rules, so the button disables before a round trip. */
    val validationError: String?
        get() = if (!register) {
            when {
                usernameOrEmail.isBlank() -> "Unesite korisnicko ime ili email."
                password.isBlank() -> "Unesite lozinku."
                else -> null
            }
        } else {
            Validation.username(usernameOrEmail)
                ?: Validation.email(email)
                ?: Validation.password(password)
                ?: Validation.displayName(displayName)
        }

    val canSubmit: Boolean get() = !submitting && validationError == null
}

class AuthViewModel : ViewModel() {

    private val repository = AppContainer.repository

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun toggleMode() = _state.update { it.copy(register = !it.register, error = null) }

    fun onUsernameChange(value: String) = _state.update { it.copy(usernameOrEmail = value, error = null) }
    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onDisplayNameChange(value: String) = _state.update { it.copy(displayName = value, error = null) }
    fun onCityChange(value: String) = _state.update { it.copy(city = value, error = null) }
    fun onRoleChange(role: Role) = _state.update { it.copy(role = role, error = null) }

    fun submit(onSuccess: (UserProfile) -> Unit) {
        val current = _state.value
        val validation = current.validationError
        if (validation != null) {
            _state.update { it.copy(error = validation) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            val result = runCatching {
                if (current.register) {
                    repository.register(
                        RegisterRequest(
                            username = current.usernameOrEmail.trim(),
                            email = current.email.trim(),
                            password = current.password,
                            displayName = current.displayName.trim(),
                            city = current.city.trim().takeIf { it.isNotEmpty() },
                            role = current.role,
                        ),
                    )
                } else {
                    repository.login(current.usernameOrEmail.trim(), current.password)
                }
            }
            result.fold(
                onSuccess = { profile ->
                    _state.update { it.copy(submitting = false) }
                    onSuccess(profile)
                },
                onFailure = { error ->
                    _state.update { it.copy(submitting = false, error = error.userMessage()) }
                },
            )
        }
    }
}
