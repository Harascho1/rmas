package rs.coffeeconquest.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rs.coffeeconquest.app.data.AppContainer
import rs.coffeeconquest.shared.dto.UserProfile

/**
 * Who is signed in. Held above the nav host so every screen can branch on the
 * role without fetching the profile again.
 */
class SessionViewModel : ViewModel() {

    private val repository = AppContainer.repository

    private val _state = MutableStateFlow<SessionState>(SessionState.Checking)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        restore()
    }

    /** Silently resumes a stored session, or falls back to the auth screen. */
    fun restore() {
        viewModelScope.launch {
            _state.value = SessionState.Checking
            if (!repository.isSignedIn()) {
                _state.value = SessionState.SignedOut
                return@launch
            }
            _state.value = runCatching { repository.me() }
                .fold(
                    onSuccess = { SessionState.SignedIn(it) },
                    onFailure = {
                        repository.logout()
                        SessionState.SignedOut
                    },
                )
        }
    }

    fun onSignedIn(profile: UserProfile) {
        _state.value = SessionState.SignedIn(profile)
    }

    /** Called after actions that change points or role, so the header stays honest. */
    fun refreshProfile() {
        viewModelScope.launch {
            runCatching { repository.me() }.onSuccess { _state.value = SessionState.SignedIn(it) }
        }
    }

    fun signOut() {
        repository.logout()
        _state.value = SessionState.SignedOut
    }
}

sealed interface SessionState {
    data object Checking : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val profile: UserProfile) : SessionState
}
