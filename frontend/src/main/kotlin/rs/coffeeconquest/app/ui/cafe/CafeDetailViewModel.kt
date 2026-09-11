package rs.coffeeconquest.app.ui.cafe

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.coffeeconquest.app.data.AppContainer
import rs.coffeeconquest.app.data.CoffeeRepository
import rs.coffeeconquest.app.data.LocationProvider
import rs.coffeeconquest.app.data.firebase.userMessage
import rs.coffeeconquest.app.ui.PhotoStore
import rs.coffeeconquest.app.ui.UiState
import rs.coffeeconquest.shared.dto.Cafe
import rs.coffeeconquest.shared.dto.CheckIn
import rs.coffeeconquest.shared.dto.Review

data class CafeDetailUiState(
    val cafe: UiState<Cafe> = UiState.Loading,
    val reviews: List<Review> = emptyList(),
    val recentCheckIns: List<CheckIn> = emptyList(),
    val myRating: Int = 0,
    val myComment: String = "",
    val submittingReview: Boolean = false,
    val message: String? = null,
)

class CafeDetailViewModel(
    private val repository: CoffeeRepository = AppContainer.repository,
    private val location: LocationProvider = AppContainer.location,
) : ViewModel() {

    private val _state = MutableStateFlow(CafeDetailUiState())
    val state: StateFlow<CafeDetailUiState> = _state.asStateFlow()

    private val photoStore = PhotoStore(repository, viewModelScope)
    val photos: StateFlow<Map<String, ImageBitmap>> = photoStore.photos

    fun requestPhoto(photoId: String?) = photoStore.load(photoId)

    private var cafeId: String = ""

    fun load(id: String) {
        cafeId = id
        viewModelScope.launch {
            _state.update { it.copy(cafe = UiState.Loading) }
            val fix = location.current()
            val result = runCatching {
                repository.cafe(id, fix?.latitude, fix?.longitude)
            }
            result.fold(
                onSuccess = { cafe -> _state.update { it.copy(cafe = UiState.Ready(cafe)) } },
                onFailure = { error -> _state.update { it.copy(cafe = UiState.Error(error.userMessage())) } },
            )
            runCatching { repository.reviews(id) }
                .onSuccess { reviews -> _state.update { it.copy(reviews = reviews) } }
            runCatching { repository.cafeCheckIns(id) }
                .onSuccess { checkIns -> _state.update { it.copy(recentCheckIns = checkIns) } }
        }
    }

    fun onRatingChange(rating: Int) = _state.update { it.copy(myRating = rating) }
    fun onCommentChange(comment: String) = _state.update { it.copy(myComment = comment) }
    fun dismissMessage() = _state.update { it.copy(message = null) }

    fun submitReview() {
        val current = _state.value
        if (current.myRating == 0) {
            _state.update { it.copy(message = "Izaberite ocenu od 1 do 5.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(submittingReview = true) }
            runCatching {
                repository.postReview(cafeId, current.myRating, current.myComment.takeIf { it.isNotBlank() })
            }.fold(
                onSuccess = {
                    _state.update {
                        it.copy(submittingReview = false, myComment = "", message = "Hvala na oceni!")
                    }
                    load(cafeId)
                },
                onFailure = { error ->
                    _state.update { it.copy(submittingReview = false, message = error.userMessage()) }
                },
            )
        }
    }
}
