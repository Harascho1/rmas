package rs.coffeeconquest.app.ui.map

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import rs.coffeeconquest.app.R
import rs.coffeeconquest.app.data.LatLon
import rs.coffeeconquest.shared.dto.Cafe

@Composable
fun OsmMap(
    center: LatLon,
    cafes: List<Cafe>,
    showUserMarker: Boolean,
    onCafeClick: (Cafe) -> Unit,
    modifier: Modifier = Modifier,
    pickedPoint: LatLon? = null,
    onMapTap: ((LatLon) -> Unit)? = null,
) {
    val context = LocalContext.current

    val youAreHereLabel = stringResource(R.string.map_marker_you_are_here)
    val pickLocationLabel = stringResource(R.string.map_marker_pick_location)
    val cafeEmoji = stringResource(R.string.map_marker_cafe_emoji)

    val centred = remember { MapAnchor() }

    val mapView = remember {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = "rs.coffeeconquest.app/0.0.0"
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN ->
                        view.parent?.requestDisallowInterceptTouchEvent(true)

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        view.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
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
            if (centred.point != center) {
                view.controller.setCenter(GeoPoint(center.latitude, center.longitude))
                centred.point = center
            }
            view.overlays.clear()

            if (onMapTap != null) {
                view.overlays.add(MapEventsOverlay(tapReceiver(onMapTap)))
            }

            if (showUserMarker) {
                view.overlays.add(
                    Marker(view).apply {
                        position = GeoPoint(center.latitude, center.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = dotDrawable(view.context, Color.parseColor("#2E7D5B"))
                        title = youAreHereLabel
                    },
                )
            }

            cafes.forEach { cafe ->
                view.overlays.add(
                    Marker(view).apply {
                        position = GeoPoint(cafe.latitude, cafe.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = cafe.name
                        icon = pinDrawable(
                            context = view.context,
                            conquered = cafe.myCheckInCount > 0,
                            boosted = cafe.activeChallenges.isNotEmpty(),
                            emoji = cafeEmoji,
                        )
                        setOnMarkerClickListener { _, _ ->
                            onCafeClick(cafe)
                            true
                        }
                    },
                )
            }

            if (pickedPoint != null) {
                view.overlays.add(
                    Marker(view).apply {
                        position = GeoPoint(pickedPoint.latitude, pickedPoint.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = targetPinDrawable(view.context)
                        title = pickLocationLabel
                    },
                )
            }
            view.invalidate()
        },
    )
}

private class MapAnchor(var point: LatLon? = null)

private fun tapReceiver(onTap: (LatLon) -> Unit) = object : MapEventsReceiver {
    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        val point = p ?: return false
        onTap(LatLon(point.latitude, point.longitude))
        return true
    }

    override fun longPressHelper(p: GeoPoint?): Boolean = false
}

private fun targetPinDrawable(context: Context): Drawable {
    val density = context.resources.displayMetrics.density
    val width = (26 * density).toInt()
    val height = (34 * density).toInt()
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val body = Color.parseColor("#2E7D5B")

    paint.color = Color.WHITE
    canvas.drawCircle(width / 2f, width / 2f, width / 2f, paint)

    paint.color = body
    val path = android.graphics.Path().apply {
        moveTo(width / 2f - 5 * density, width * 0.8f)
        lineTo(width / 2f + 5 * density, width * 0.8f)
        lineTo(width / 2f, height.toFloat())
        close()
    }
    canvas.drawPath(path, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 3 * density
    canvas.drawCircle(width / 2f, width / 2f, width / 2f - 2 * density, paint)
    canvas.drawCircle(width / 2f, width / 2f, 2.5f * density, paint)

    return BitmapDrawable(context.resources, bitmap)
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

private fun pinDrawable(context: Context, conquered: Boolean, boosted: Boolean, emoji: String): Drawable {
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
    canvas.drawText(emoji, width / 2f, width / 2f + 5 * density, paint)

    return BitmapDrawable(context.resources, bitmap)
}
