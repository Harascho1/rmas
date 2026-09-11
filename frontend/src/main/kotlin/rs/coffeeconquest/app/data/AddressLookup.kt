package rs.coffeeconquest.app.data

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class ResolvedAddress(val street: String? = null, val city: String? = null) {
    val isEmpty: Boolean get() = street == null && city == null
}

class AddressLookup(private val context: Context) {

    suspend fun reverse(point: LatLon): ResolvedAddress? = withContext(Dispatchers.IO) {
        fromDevice(point)?.takeIf { !it.isEmpty } ?: fromNominatim(point)?.takeIf { !it.isEmpty }
    }

    @Suppress("DEPRECATION")
    private fun fromDevice(point: LatLon): ResolvedAddress? {
        if (!Geocoder.isPresent()) return null
        val address = runCatching {
            Geocoder(context, SERBIAN).getFromLocation(point.latitude, point.longitude, 1)
        }.getOrNull()?.firstOrNull() ?: return null

        val street = listOfNotNull(address.thoroughfare, address.subThoroughfare)
            .joinToString(" ")
            .ifBlank { address.featureName.orEmpty() }
        return ResolvedAddress(
            street = street.takeIf { it.isNotBlank() },
            city = listOfNotNull(address.locality, address.subAdminArea, address.adminArea)
                .firstOrNull { it.isNotBlank() },
        )
    }

    private fun fromNominatim(point: LatLon): ResolvedAddress? {
        val url = URL(
            "https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=18" +
                "&addressdetails=1&accept-language=sr" +
                "&lat=${point.latitude}&lon=${point.longitude}",
        )
        val body = runCatching {
            (url.openConnection() as HttpURLConnection).run {
                setRequestProperty("User-Agent", USER_AGENT)
                connectTimeout = 8_000
                readTimeout = 8_000
                try {
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        null
                    } else {
                        inputStream.bufferedReader().use { it.readText() }
                    }
                } finally {
                    disconnect()
                }
            }
        }.getOrNull() ?: return null

        val address =
            runCatching { JSONObject(body).optJSONObject("address") }.getOrNull() ?: return null
        val street = listOfNotNull(
            address.string("road") ?: address.string("pedestrian") ?: address.string("footway"),
            address.string("house_number"),
        ).joinToString(" ").takeIf { it.isNotBlank() }
        val city = address.string("city")
            ?: address.string("town")
            ?: address.string("village")
            ?: address.string("municipality")
        return ResolvedAddress(street, city)
    }

    private fun JSONObject.string(key: String): String? = optString(key).takeIf { it.isNotBlank() }

    private companion object {
        val SERBIAN: Locale = Locale.forLanguageTag("sr-RS")
        const val USER_AGENT = "rs.coffeeconquest.app/0.0.0"
    }
}
