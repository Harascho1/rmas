package rs.coffeeconquest.app.ui

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rs.coffeeconquest.app.data.CoffeeRepository

class PhotoStore(
    private val repository: CoffeeRepository,
    private val scope: CoroutineScope,
) {

    private val _photos = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    val photos: StateFlow<Map<String, ImageBitmap>> = _photos.asStateFlow()

    private val claimed = mutableSetOf<String>()

    fun load(photoId: String?) {
        if (photoId == null || !claimed.add(photoId)) return
        scope.launch {
            val bytes = runCatching { repository.photo(photoId) }.getOrNull()
            val image = bytes?.let {
                withContext(Dispatchers.Default) {
                    BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
                }
            }
            if (image == null) {
                claimed.remove(photoId)
                return@launch
            }
            _photos.update { it + (photoId to image) }
        }
    }
}

fun Map<String, ImageBitmap>.of(photoId: String?): ImageBitmap? = photoId?.let(::get)
