package rs.coffeeconquest.app.ui.feed

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.coffeeconquest.app.data.AppContainer
import rs.coffeeconquest.app.data.CoffeeRepository
import rs.coffeeconquest.app.data.firebase.userMessage
import rs.coffeeconquest.app.ui.PhotoStore
import rs.coffeeconquest.app.ui.UiState
import rs.coffeeconquest.shared.dto.FeedItem

data class FeedUiState(
    val items: UiState<List<FeedItem>> = UiState.Loading,
    val followingOnly: Boolean = false,
)

class FeedViewModel(
    private val repository: CoffeeRepository = AppContainer.repository,
) : ViewModel() {

    private val _state = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    private val photoStore = PhotoStore(repository, viewModelScope)
    val photos: StateFlow<Map<String, ImageBitmap>> = photoStore.photos

    fun requestPhoto(photoId: String?) = photoStore.load(photoId)

    fun onScopeChange(followingOnly: Boolean) {
        _state.update { it.copy(followingOnly = followingOnly) }
        load()
    }

    private var feedJob: Job? = null

    fun load() {
        feedJob?.cancel()
        _state.update { it.copy(items = UiState.Loading) }
        feedJob = viewModelScope.launch {
            repository.feedStream(_state.value.followingOnly)
                .catch { error ->
                    _state.update { it.copy(items = UiState.Error(error.userMessage())) }
                }
                .collect { items -> _state.update { it.copy(items = UiState.Ready(items)) } }
        }
    }
}
