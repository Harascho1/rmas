package rs.coffeeconquest.app.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.coffeeconquest.app.data.AppContainer
import rs.coffeeconquest.app.data.firebase.userMessage
import rs.coffeeconquest.app.ui.UiState
import rs.coffeeconquest.shared.dto.FeedItem

data class FeedUiState(
    val items: UiState<List<FeedItem>> = UiState.Loading,
    val followingOnly: Boolean = false,
)

class FeedViewModel : ViewModel() {

    private val repository = AppContainer.repository

    private val _state = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    fun onScopeChange(followingOnly: Boolean) {
        _state.update { it.copy(followingOnly = followingOnly) }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(items = UiState.Loading) }
            runCatching { repository.feed(_state.value.followingOnly) }.fold(
                onSuccess = { items -> _state.update { it.copy(items = UiState.Ready(items)) } },
                onFailure = { error -> _state.update { it.copy(items = UiState.Error(error.userMessage())) } },
            )
        }
    }
}
