package rs.coffeeconquest.app.ui.staff

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.coffeeconquest.app.data.AppContainer
import rs.coffeeconquest.app.data.firebase.userMessage
import rs.coffeeconquest.app.ui.UiState
import rs.coffeeconquest.shared.dto.Cafe
import rs.coffeeconquest.shared.dto.QrTokenResponse

data class StaffQrUiState(
    val cafes: UiState<List<Cafe>> = UiState.Loading,
    val selectedCafeId: String? = null,
    val token: QrTokenResponse? = null,
    val qrBitmap: Bitmap? = null,
    val secondsLeft: Long = 0,
    val busy: Boolean = false,
    val error: String? = null,
)

class StaffQrViewModel : ViewModel() {

    private val repository = AppContainer.repository

    private val _state = MutableStateFlow(StaffQrUiState())
    val state: StateFlow<StaffQrUiState> = _state.asStateFlow()

    init {
        tickCountdown()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(cafes = UiState.Loading) }
            runCatching { repository.myCafes() }.fold(
                onSuccess = { cafes ->
                    _state.update {
                        it.copy(
                            cafes = UiState.Ready(cafes),
                            selectedCafeId = it.selectedCafeId ?: cafes.firstOrNull()?.id,
                        )
                    }
                },
                onFailure = { error -> _state.update { it.copy(cafes = UiState.Error(error.userMessage())) } },
            )
        }
    }

    fun select(cafeId: String) {
        _state.update { it.copy(selectedCafeId = cafeId, token = null, qrBitmap = null) }
    }

    fun refreshToken() {
        val cafeId = _state.value.selectedCafeId ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            runCatching { repository.qrToken(cafeId) }.fold(
                onSuccess = { token ->
                    _state.update {
                        it.copy(busy = false, token = token, qrBitmap = renderQr(token.token))
                    }
                },
                onFailure = { error -> _state.update { it.copy(busy = false, error = error.userMessage()) } },
            )
        }
    }

    /** Keeps the "expires in" label honest and clears the code once it is stale. */
    private fun tickCountdown() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val token = _state.value.token ?: continue
                val left = (token.expiresAtEpochMs - System.currentTimeMillis()) / 1000
                _state.update {
                    if (left <= 0) it.copy(secondsLeft = 0, qrBitmap = null, token = null)
                    else it.copy(secondsLeft = left)
                }
            }
        }
    }

    private fun renderQr(content: String, size: Int = 640): Bitmap {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = createBitmap(size, size)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap[x, y] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        return bitmap
    }
}
