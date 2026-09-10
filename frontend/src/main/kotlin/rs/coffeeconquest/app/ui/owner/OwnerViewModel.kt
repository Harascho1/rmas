package rs.coffeeconquest.app.ui.owner

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
import rs.coffeeconquest.shared.dto.Cafe

data class OwnerUiState(
    val cafes: UiState<List<Cafe>> = UiState.Loading,
)

class OwnerViewModel : ViewModel() {

    private val repository = AppContainer.repository

    private val _state = MutableStateFlow(OwnerUiState())
    val state: StateFlow<OwnerUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(cafes = UiState.Loading) }
            runCatching { repository.myCafes() }.fold(
                onSuccess = { cafes -> _state.update { it.copy(cafes = UiState.Ready(cafes)) } },
                onFailure = { error -> _state.update { it.copy(cafes = UiState.Error(error.userMessage())) } },
            )
        }
    }
}
