package rs.coffeeconquest.app.ui.leaderboard

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
import rs.coffeeconquest.shared.dto.CityChampion
import rs.coffeeconquest.shared.dto.LeaderboardResponse
import rs.coffeeconquest.shared.model.LeaderboardScope

data class LeaderboardUiState(
    val board: UiState<LeaderboardResponse> = UiState.Loading,
    val scope: LeaderboardScope = LeaderboardScope.GLOBAL,
    val weekly: Boolean = false,
    val city: String? = null,
    val champion: CityChampion? = null,
)

class LeaderboardViewModel : ViewModel() {

    private val repository = AppContainer.repository

    private val _state = MutableStateFlow(LeaderboardUiState())
    val state: StateFlow<LeaderboardUiState> = _state.asStateFlow()

    private val photoStore = PhotoStore(repository, viewModelScope)
    val photos: StateFlow<Map<String, ImageBitmap>> = photoStore.photos

    /** The UI reports which photo scrolled into view; fetching it is this ViewModel's job. */
    fun requestPhoto(photoId: String?) = photoStore.load(photoId)

    fun start(city: String?) {
        if (_state.value.city == null) _state.update { it.copy(city = city) }
        load()
    }

    fun onScopeChange(scope: LeaderboardScope) {
        _state.update { it.copy(scope = scope) }
        load()
    }

    fun onPeriodChange(weekly: Boolean) {
        _state.update { it.copy(weekly = weekly) }
        load()
    }

    fun load() {
        val current = _state.value
        viewModelScope.launch {
            _state.update { it.copy(board = UiState.Loading) }
            runCatching {
                repository.leaderboard(
                    scope = current.scope,
                    city = current.city.takeIf { current.scope == LeaderboardScope.CITY },
                    weekly = current.weekly,
                )
            }.fold(
                onSuccess = { board -> _state.update { it.copy(board = UiState.Ready(board)) } },
                onFailure = { error -> _state.update { it.copy(board = UiState.Error(error.userMessage())) } },
            )

            // The crown is the reward for the weekly city race, so it is worth showing up front.
            current.city?.let { city ->
                runCatching { repository.cityChampion(city) }
                    .onSuccess { champion -> _state.update { it.copy(champion = champion) } }
            }
        }
    }
}
