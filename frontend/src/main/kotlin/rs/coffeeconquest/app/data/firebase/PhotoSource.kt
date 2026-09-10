package rs.coffeeconquest.app.data.firebase

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import rs.coffeeconquest.shared.rules.Time
import java.io.ByteArrayOutputStream

/**
 * Photos, stored as documents in Firestore rather than in Cloud Storage.
 *
 * Firebase now puts Cloud Storage behind a billing account, so photos live in a
 * `photos/{id}` collection as base64 instead. A Firestore document caps out at
 * 1 MiB and base64 costs a third on top, so every image is downscaled below
 * [MAX_IMAGE_BYTES] before it is written - which is well within what a phone
 * camera shot compresses to anyway.
 *
 * The trade is real: images cost document reads instead of bandwidth, and they
 * cannot be huge. Keeping them in their own collection is what makes it work -
 * a cafe or check-in document carries only the photo *id*, so listing a
 * screenful of cafes never drags their images along.
 */
class PhotoSource {

    /** Decoded images kept in memory, so scrolling a list re-reads nothing. */
    private val cache = object : LruCache<String, ByteArray>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

    /** Stores an image and returns the id to put on a cafe, check-in or profile. */
    suspend fun upload(bytes: ByteArray, fileName: String): String {
        Fire.requireUid()
        if (bytes.isEmpty()) throw AppException("Fajl je prazan.")

        val payload = withContext(Dispatchers.Default) { shrink(bytes) }
        val encoded = Base64.encodeToString(payload, Base64.NO_WRAP)

        val document = Fire.photos().add(
            mapOf(
                "base64" to encoded,
                "contentType" to "image/jpeg",
                "fileName" to fileName,
                "sizeBytes" to payload.size,
                "uploaderId" to Fire.requireUid(),
                "createdAt" to Time.now(),
            ),
        ).await()

        cache.put(document.id, payload)
        return document.id
    }

    /** Raw JPEG bytes for [photoId], or null when it is missing. */
    suspend fun load(photoId: String): ByteArray? {
        cache.get(photoId)?.let { return it }

        val document = Fire.photos().document(photoId).fetch()
        val encoded = document.str("base64") ?: return null
        val bytes = withContext(Dispatchers.Default) {
            runCatching { Base64.decode(encoded, Base64.NO_WRAP) }.getOrNull()
        } ?: return null

        cache.put(photoId, bytes)
        return bytes
    }

    /**
     * Compresses until the image fits [MAX_IMAGE_BYTES]: quality first, then
     * halving the dimensions once quality alone stops helping.
     */
    private fun shrink(input: ByteArray): ByteArray {
        if (input.size <= MAX_IMAGE_BYTES) return input

        var bitmap = BitmapFactory.decodeByteArray(input, 0, input.size)
            ?: throw AppException("Slika nije citljiva.")
        var quality = 80

        repeat(8) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            val candidate = stream.toByteArray()
            if (candidate.size <= MAX_IMAGE_BYTES) return candidate

            if (quality > 45) {
                quality -= 15
            } else {
                bitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width / 2).coerceAtLeast(1),
                    (bitmap.height / 2).coerceAtLeast(1),
                    true,
                )
                quality = 80
            }
        }
        throw AppException("Slika je prevelika, probajte manju rezoluciju.")
    }

    companion object {
        /**
         * Firestore allows 1 MiB per document and base64 adds about a third, so
         * this leaves comfortable room for the other fields.
         */
        const val MAX_IMAGE_BYTES = 500_000

        private const val CACHE_BYTES = 8 * 1024 * 1024
    }
}
