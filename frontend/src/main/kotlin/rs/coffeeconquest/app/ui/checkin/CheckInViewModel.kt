package rs.coffeeconquest.app.ui.checkin

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.coffeeconquest.app.data.AppContainer
import rs.coffeeconquest.app.data.CoffeeRepository
import rs.coffeeconquest.app.data.LocationFix
import rs.coffeeconquest.app.data.LocationProvider
import rs.coffeeconquest.app.data.firebase.userMessage
import rs.coffeeconquest.app.ui.UiState
import rs.coffeeconquest.shared.dto.Cafe
import rs.coffeeconquest.shared.dto.CheckInResult
import rs.coffeeconquest.shared.dto.CreateCheckInRequest
import rs.coffeeconquest.shared.model.CheckInMethod
import rs.coffeeconquest.shared.rules.Geo
import rs.coffeeconquest.shared.rules.ScoreRules
import java.io.ByteArrayOutputStream

data class CheckInUiState(
    val cafe: UiState<Cafe> = UiState.Loading,
    val method: CheckInMethod = CheckInMethod.GPS,
    val fix: LocationFix? = null,
    val photo: Bitmap? = null,
    val qrToken: String? = null,
    val note: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val submitting: Boolean = false,
    val result: CheckInResult? = null,
    val error: String? = null,
) {
    val distanceMeters: Double?
        get() {
            val cafe = (cafe as? UiState.Ready)?.data ?: return null
            val here = fix ?: return null
            return Geo.distanceMeters(here.latitude, here.longitude, cafe.latitude, cafe.longitude)
        }

    val inRange: Boolean get() = (distanceMeters ?: Double.MAX_VALUE) <= Geo.MAX_CHECKIN_DISTANCE_M

    /** Live preview using the same rules the server will apply. */
    val preview: ScoreRules.ScoreBreakdown
        get() {
            val cafe = (cafe as? UiState.Ready)?.data
            return ScoreRules.checkInScore(
                method = method,
                withPhoto = photo != null,
                firstVisitToCafe = (cafe?.myCheckInCount ?: 0) == 0,
                streakDays = 1,
                challengeMultiplier = cafe?.activeChallenges?.maxOfOrNull { it.multiplier } ?: 1.0,
            )
        }

    val canSubmit: Boolean
        get() = !submitting && when (method) {
            CheckInMethod.GPS -> inRange
            CheckInMethod.QR -> qrToken != null
            CheckInMethod.HONOR -> true
        }
}

class CheckInViewModel(
    private val repository: CoffeeRepository = AppContainer.repository,
    private val location: LocationProvider = AppContainer.location,
) : ViewModel() {

    private val _state = MutableStateFlow(CheckInUiState())
    val state: StateFlow<CheckInUiState> = _state.asStateFlow()

    private var cafeId: String = ""

    fun load(id: String) {
        cafeId = id
        viewModelScope.launch {
            val fix = location.current()
            _state.update { it.copy(fix = fix, cafe = UiState.Loading) }
            runCatching { repository.cafe(id, fix?.latitude, fix?.longitude) }.fold(
                onSuccess = { cafe -> _state.update { it.copy(cafe = UiState.Ready(cafe)) } },
                onFailure = { error -> _state.update { it.copy(cafe = UiState.Error(error.userMessage())) } },
            )
        }
    }

    fun onMethodChange(method: CheckInMethod) = _state.update { it.copy(method = method, error = null) }
    fun onPhotoTaken(bitmap: Bitmap?) = _state.update { it.copy(photo = bitmap) }
    fun onNoteChange(value: String) = _state.update { it.copy(note = value) }
    fun onRatingChange(value: Int) = _state.update { it.copy(rating = value) }
    fun onCommentChange(value: String) = _state.update { it.copy(comment = value) }

    fun onQrScanned(token: String?) {
        if (token.isNullOrBlank()) {
            _state.update { it.copy(error = "QR kod nije procitan.") }
        } else {
            _state.update { it.copy(qrToken = token, method = CheckInMethod.QR, error = null) }
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            val fix = location.current()
            _state.update { it.copy(fix = fix, error = if (fix == null) "Lokacija nije dostupna." else null) }
        }
    }

    fun submit() {
        val current = _state.value
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }

            // The photo is uploaded first; the check-in then references it by id.
            val photoId = current.photo?.let { bitmap ->
                runCatching { repository.uploadPhoto(bitmap.toJpegBytes()) }
                    .getOrElse { error ->
                        _state.update { it.copy(submitting = false, error = error.userMessage()) }
                        return@launch
                    }
            }

            runCatching {
                repository.checkIn(
                    CreateCheckInRequest(
                        cafeId = cafeId,
                        method = current.method,
                        latitude = current.fix?.latitude,
                        longitude = current.fix?.longitude,
                        qrToken = current.qrToken,
                        photoId = photoId,
                        note = current.note.takeIf { it.isNotBlank() },
                        rating = current.rating.takeIf { it > 0 },
                        comment = current.comment.takeIf { it.isNotBlank() },
                    ),
                )
            }.fold(
                onSuccess = { result -> _state.update { it.copy(submitting = false, result = result) } },
                onFailure = { error -> _state.update { it.copy(submitting = false, error = error.userMessage()) } },
            )
        }
    }
}

private fun Bitmap.toJpegBytes(quality: Int = 82): ByteArray =
    ByteArrayOutputStream().use { stream ->
        compress(Bitmap.CompressFormat.JPEG, quality, stream)
        stream.toByteArray()
    }
