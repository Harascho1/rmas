package rs.coffeeconquest.app.ui.profile

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
import rs.coffeeconquest.shared.dto.CheckIn
import rs.coffeeconquest.shared.dto.UserStats
import rs.coffeeconquest.shared.model.Badges

data class ProfileUiState(
    val stats: UiState<UserStats> = UiState.Loading,
    val history: List<CheckIn> = emptyList(),
    val followBusy: Boolean = false,
    val message: String? = null,
) {
    val badgeBoard: List<BadgeSlot>
        get() {
            val earned = (stats as? UiState.Ready)?.data?.badges?.associateBy { it.code }.orEmpty()
            return Badges.all.map { definition ->
                BadgeSlot(
                    code = definition.code,
                    title = definition.title,
                    description = definition.description,
                    emoji = definition.emoji,
                    earnedAtEpochMs = earned[definition.code]?.earnedAtEpochMs,
                )
            }
        }
}

data class BadgeSlot(
    val code: String,
    val title: String,
    val description: String,
    val emoji: String,
    val earnedAtEpochMs: Long?,
) {
    val earned: Boolean get() = earnedAtEpochMs != null
}

class ProfileViewModel : ViewModel() {
    private val repository = AppContainer.repository

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val photoStore = PhotoStore(repository, viewModelScope)
    val photos: StateFlow<Map<String, ImageBitmap>> = photoStore.photos

    /** The UI reports which photo scrolled into view; fetching it is this ViewModel's job. */
    fun requestPhoto(photoId: String?) = photoStore.load(photoId)

    private var userId: String = ""

    fun load(id: String, ownProfile: Boolean) {
        userId = id
        viewModelScope.launch {
            _state.update { it.copy(stats = UiState.Loading) }
            runCatching { repository.userStats(id) }.fold(
                onSuccess = { stats -> _state.update { it.copy(stats = UiState.Ready(stats)) } },
                onFailure = { error -> _state.update { it.copy(stats = UiState.Error(error.userMessage())) } },
            )
            runCatching {
                if (ownProfile) repository.myCheckIns() else repository.userCheckIns(id)
            }.onSuccess { history -> _state.update { it.copy(history = history) } }
        }
    }

    fun toggleFollow() {
        val current = (_state.value.stats as? UiState.Ready)?.data ?: return
        viewModelScope.launch {
            _state.update { it.copy(followBusy = true) }
            runCatching { repository.follow(userId, !current.profile.isFollowedByMe) }.fold(
                onSuccess = {
                    _state.update { it.copy(followBusy = false) }
                    load(userId, ownProfile = false)
                },
                onFailure = { error ->
                    _state.update { it.copy(followBusy = false, message = error.userMessage()) }
                },
            )
        }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }
}
