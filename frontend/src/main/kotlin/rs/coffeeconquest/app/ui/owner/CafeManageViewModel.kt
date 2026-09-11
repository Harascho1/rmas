package rs.coffeeconquest.app.ui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.coffeeconquest.app.data.AppContainer
import rs.coffeeconquest.app.data.CoffeeRepository
import rs.coffeeconquest.app.data.firebase.userMessage
import rs.coffeeconquest.app.ui.UiState
import rs.coffeeconquest.shared.dto.Cafe
import rs.coffeeconquest.shared.dto.CafeStats
import rs.coffeeconquest.shared.dto.Challenge
import rs.coffeeconquest.shared.dto.CreateChallengeRequest
import rs.coffeeconquest.shared.dto.Review
import rs.coffeeconquest.shared.model.ChallengeScope

data class CafeManageUiState(
    val cafe: UiState<Cafe> = UiState.Loading,
    val stats: CafeStats? = null,
    val reviews: List<Review> = emptyList(),
    val challenges: List<Challenge> = emptyList(),
    val replyDraft: Map<String, String> = emptyMap(),
    val challengeTitle: String = "",
    val challengeMultiplier: Double = 2.0,
    val challengeDays: Int = 1,
    val busy: Boolean = false,
    val message: String? = null,
)

/** Everything an owner does with one cafe: numbers, replies and bonus challenges. */
class CafeManageViewModel(
    private val repository: CoffeeRepository = AppContainer.repository,
) : ViewModel() {

    private val _state = MutableStateFlow(CafeManageUiState())
    val state: StateFlow<CafeManageUiState> = _state.asStateFlow()

    private var cafeId: String = ""

    fun load(id: String) {
        cafeId = id
        viewModelScope.launch {
            _state.update { it.copy(cafe = UiState.Loading) }
            runCatching { repository.cafe(id) }.fold(
                onSuccess = { cafe -> _state.update { it.copy(cafe = UiState.Ready(cafe)) } },
                onFailure = { error -> _state.update { it.copy(cafe = UiState.Error(error.userMessage())) } },
            )
            runCatching { repository.cafeStats(id) }.onSuccess { stats -> _state.update { it.copy(stats = stats) } }
            runCatching { repository.reviews(id) }.onSuccess { reviews -> _state.update { it.copy(reviews = reviews) } }
            runCatching { repository.challenges(cafeId = id) }
                .onSuccess { challenges -> _state.update { it.copy(challenges = challenges) } }
        }
    }

    fun onReplyDraftChange(reviewId: String, value: String) =
        _state.update { it.copy(replyDraft = it.replyDraft + (reviewId to value)) }

    fun onChallengeTitleChange(value: String) = _state.update { it.copy(challengeTitle = value) }
    fun onChallengeMultiplierChange(value: Double) = _state.update { it.copy(challengeMultiplier = value) }
    fun onChallengeDaysChange(value: Int) = _state.update { it.copy(challengeDays = value.coerceIn(1, 14)) }
    fun dismissMessage() = _state.update { it.copy(message = null) }

    fun sendReply(reviewId: String) {
        val text = _state.value.replyDraft[reviewId]?.trim()
        if (text.isNullOrEmpty()) {
            _state.update { it.copy(message = "Odgovor ne moze biti prazan.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            runCatching { repository.replyToReview(cafeId, reviewId, text) }.fold(
                onSuccess = {
                    _state.update {
                        it.copy(busy = false, replyDraft = it.replyDraft - reviewId, message = "Odgovor je objavljen.")
                    }
                    load(cafeId)
                },
                onFailure = { error -> _state.update { it.copy(busy = false, message = error.userMessage()) } },
            )
        }
    }

    fun createChallenge() {
        val current = _state.value
        if (current.challengeTitle.isBlank()) {
            _state.update { it.copy(message = "Unesite naziv izazova.") }
            return
        }
        val now = System.currentTimeMillis()

        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            runCatching {
                repository.createChallenge(
                    CreateChallengeRequest(
                        scope = ChallengeScope.CAFE,
                        cafeId = cafeId,
                        title = current.challengeTitle.trim(),
                        multiplier = current.challengeMultiplier,
                        startsAtEpochMs = now,
                        endsAtEpochMs = now + current.challengeDays * 24L * 60 * 60 * 1000,
                    ),
                )
            }.fold(
                onSuccess = {
                    _state.update { it.copy(busy = false, challengeTitle = "", message = "Izazov je aktivan.") }
                    load(cafeId)
                },
                onFailure = { error -> _state.update { it.copy(busy = false, message = error.userMessage()) } },
            )
        }
    }

    fun deleteChallenge(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteChallenge(id) }.fold(
                onSuccess = { load(cafeId) },
                onFailure = { error -> _state.update { it.copy(message = error.userMessage()) } },
            )
        }
    }
}
