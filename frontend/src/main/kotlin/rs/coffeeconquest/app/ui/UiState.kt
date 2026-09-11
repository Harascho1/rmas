package rs.coffeeconquest.app.ui

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
    data class Ready<T>(val data: T) : UiState<T>
}

inline fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> = when (this) {
    is UiState.Loading -> UiState.Loading
    is UiState.Error -> this
    is UiState.Ready -> UiState.Ready(transform(data))
}

val <T> UiState<T>.dataOrNull: T? get() = (this as? UiState.Ready)?.data
