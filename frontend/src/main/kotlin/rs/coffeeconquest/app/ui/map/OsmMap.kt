package rs.coffeeconquest.app.ui.map

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import rs.coffeeconquest.app.data.LatLon
import rs.coffeeconquest.shared.dto.Cafe

/**
 * OpenStreetMap via osmdroid: no API key, no billing account, and custom pins are
 * plain Android drawables - which is exactly what a student project needs.
 */
@Composable
fun OsmMap(
    center: LatLon,
    cafes: List<Cafe>,
    showUserMarker: Boolean,
    onCafeClick: (Cafe) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val mapView = remember {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            // OSM's tile policy requires an identifying user agent.
            userAgentValue = "rs.coffeeconquest.app/0.0.0"
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.controller.setCenter(GeoPoint(center.latitude, center.longitude))
            view.overlays.clear()

            if (showUserMarker) {
                view.overlays.add(
                    Marker(view).apply {
                        position = GeoPoint(center.latitude, center.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = dotDrawable(view.context, Color.parseColor("#2E7D5B"))
                        title = "Vi ste ovde"
                    },
                )
            }

            cafes.forEach { cafe ->
                view.overlays.add(
                    Marker(view).apply {
                        position = GeoPoint(cafe.latitude, cafe.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = cafe.name
                        // A cafe the user already conquered gets the crema pin.
                        icon = pinDrawable(
                            context = view.context,
                            conquered = cafe.myCheckInCount > 0,
                            boosted = cafe.activeChallenges.isNotEmpty(),
                        )
                        setOnMarkerClickListener { _, _ ->
                            onCafeClick(cafe)
                            true
                        }
                    },
                )
            }
            view.invalidate()
        },
    )
}

private fun dotDrawable(context: Context, color: Int): Drawable {
    val size = (14 * context.resources.displayMetrics.density).toInt()
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.color = color
    canvas.drawCircle(size / 2f, size / 2f, size / 2.6f, paint)
    return BitmapDrawable(context.resources, bitmap)
}

private fun pinDrawable(context: Context, conquered: Boolean, boosted: Boolean): Drawable {
    val density = context.resources.displayMetrics.density
    val width = (26 * density).toInt()
    val height = (34 * density).toInt()
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val body = when {
        boosted -> Color.parseColor("#C62828")
        conquered -> Color.parseColor("#D7A86E")
        else -> Color.parseColor("#4E342E")
    }

    paint.color = Color.WHITE
    canvas.drawCircle(width / 2f, width / 2f, width / 2f, paint)

    paint.color = body
    canvas.drawCircle(width / 2f, width / 2f, width / 2f - 2 * density, paint)

    // The stem, drawn as a triangle down to the anchor point.
    val path = android.graphics.Path().apply {
        moveTo(width / 2f - 5 * density, width * 0.8f)
        lineTo(width / 2f + 5 * density, width * 0.8f)
        lineTo(width / 2f, height.toFloat())
        close()
    }
    canvas.drawPath(path, paint)

    paint.color = Color.WHITE
    paint.textSize = 13 * density
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("☕", width / 2f, width / 2f + 5 * density, paint)

    return BitmapDrawable(context.resources, bitmap)
}
