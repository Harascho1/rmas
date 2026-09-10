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

/**
 * Photo loading for one screen, owned by that screen's ViewModel.
 *
 * Photos live in Firestore rather than behind a URL, so something has to fetch
 * and decode them. That something is not the UI: a composable reports which
 * photo is on screen and receives the finished [ImageBitmap] as a parameter,
 * so a row that is never scrolled to is never read.
 */
class PhotoStore(
    private val repository: CoffeeRepository,
    private val scope: CoroutineScope,
) {

    private val _photos = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    val photos: StateFlow<Map<String, ImageBitmap>> = _photos.asStateFlow()

    /** Ids already loaded or in flight, so reloading a list never re-reads a photo. */
    private val claimed = mutableSetOf<String>()

    /** Called when a photo scrolls into view; already-claimed ids are ignored. */
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
                // A failed read is not cached, so the next reload may try again.
                claimed.remove(photoId)
                return@launch
            }
            _photos.update { it + (photoId to image) }
        }
    }
}

/** Reads a loaded photo out of screen state: `photos.of(cafe.photoId)`. */
fun Map<String, ImageBitmap>.of(photoId: String?): ImageBitmap? = photoId?.let(::get)
