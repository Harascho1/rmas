package rs.coffeeconquest.app.ui.admin

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.coffeeconquest.app.data.AppContainer
import rs.coffeeconquest.app.data.firebase.userMessage
import rs.coffeeconquest.app.ui.PhotoStore
import rs.coffeeconquest.app.ui.UiState
import rs.coffeeconquest.shared.dto.Cafe
import rs.coffeeconquest.shared.dto.CheckIn
import rs.coffeeconquest.shared.dto.UserProfile
import rs.coffeeconquest.shared.model.Role

enum class AdminTab { CAFES, CHECK_INS, USERS }

data class AdminUiState(
    val tab: AdminTab = AdminTab.CAFES,
    val pendingCafes: UiState<List<Cafe>> = UiState.Loading,
    val flagged: UiState<List<CheckIn>> = UiState.Loading,
    val users: UiState<List<UserProfile>> = UiState.Loading,
    val busy: Boolean = false,
    val message: String? = null,
)

class AdminViewModel : ViewModel() {

    private val repository = AppContainer.repository

    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    private val photoStore = PhotoStore(repository, viewModelScope)
    val photos: StateFlow<Map<String, ImageBitmap>> = photoStore.photos

    /** The UI reports which photo scrolled into view; fetching it is this ViewModel's job. */
    fun requestPhoto(photoId: String?) = photoStore.load(photoId)

    fun onTabChange(tab: AdminTab) {
        _state.update { it.copy(tab = tab) }
        load()
    }

    fun load() {
        viewModelScope.launch {
            when (_state.value.tab) {
                AdminTab.CAFES -> {
                    _state.update { it.copy(pendingCafes = UiState.Loading) }
                    runCatching { repository.pendingCafes() }.fold(
                        onSuccess = { cafes -> _state.update { it.copy(pendingCafes = UiState.Ready(cafes)) } },
                        onFailure = { e -> _state.update { it.copy(pendingCafes = UiState.Error(e.userMessage())) } },
                    )
                }
                AdminTab.CHECK_INS -> {
                    _state.update { it.copy(flagged = UiState.Loading) }
                    runCatching { repository.flaggedCheckIns() }.fold(
                        onSuccess = { list -> _state.update { it.copy(flagged = UiState.Ready(list)) } },
                        onFailure = { e -> _state.update { it.copy(flagged = UiState.Error(e.userMessage())) } },
                    )
                }
                AdminTab.USERS -> {
                    _state.update { it.copy(users = UiState.Loading) }
                    runCatching { repository.allUsers() }.fold(
                        onSuccess = { list -> _state.update { it.copy(users = UiState.Ready(list)) } },
                        onFailure = { e -> _state.update { it.copy(users = UiState.Error(e.userMessage())) } },
                    )
                }
            }
        }
    }

    fun moderateCafe(cafeId: String, approve: Boolean) = act {
        repository.moderateCafe(cafeId, approve)
        if (approve) "Kafic je odobren." else "Kafic je odbijen."
    }

    fun moderateCheckIn(checkInId: String, valid: Boolean) = act {
        if (valid) {
            repository.approveCheckIn(checkInId)
            "Check-in je odobren, poeni su dodeljeni."
        } else {
            repository.invalidateCheckIn(checkInId, "Sumnja na varanje.")
            "Check-in je ponisten."
        }
    }

    fun setBanned(userId: String, banned: Boolean) = act {
        repository.banUser(userId, banned, if (banned) "Krsenje pravila." else "")
        if (banned) "Korisnik je blokiran." else "Blokada je uklonjena."
    }

    fun setRole(userId: String, role: Role) = act {
        repository.changeRole(userId, role)
        "Rola je promenjena u ${role.name}."
    }

    /** Runs a moderation action, then reloads the current tab. */
    private fun act(block: suspend () -> String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            runCatching { block() }.fold(
                onSuccess = { message ->
                    _state.update { it.copy(busy = false, message = message) }
                    load()
                },
                onFailure = { error -> _state.update { it.copy(busy = false, message = error.userMessage()) } },
            )
        }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }
}
