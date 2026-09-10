package rs.coffeeconquest.app.ui.common

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rs.coffeeconquest.app.data.AppContainer
import rs.coffeeconquest.app.ui.UiState
import rs.coffeeconquest.shared.model.Role
import rs.coffeeconquest.shared.rules.ScoreRules

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            if (onRetry != null) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRetry) { Text("Pokusaj ponovo") }
            }
        }
    }
}

@Composable
fun EmptyBox(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Renders the three UI states so screens only describe the happy path. */
@Composable
fun <T> StateContent(
    state: UiState<T>,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        is UiState.Loading -> LoadingBox(modifier)
        is UiState.Error -> ErrorBox(state.message, onRetry, modifier)
        is UiState.Ready -> content(state.data)
    }
}

@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(contentPadding)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.secondaryContainer,
    foreground: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = foreground,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun RoleChip(role: Role, modifier: Modifier = Modifier) {
    val (label, color) = when (role) {
        Role.HUNTER -> "Hunter" to MaterialTheme.colorScheme.secondaryContainer
        Role.OWNER -> "Vlasnik" to MaterialTheme.colorScheme.primaryContainer
        Role.STAFF -> "Osoblje" to MaterialTheme.colorScheme.tertiaryContainer
        Role.ADMIN -> "Admin" to MaterialTheme.colorScheme.errorContainer
    }
    Pill(label, modifier, background = color, foreground = MaterialTheme.colorScheme.onSurface)
}

@Composable
fun StarRating(
    rating: Int,
    modifier: Modifier = Modifier,
    size: Int = 18,
    onRatingChange: ((Int) -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        (1..5).forEach { star ->
            val filled = star <= rating
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "Ocena $star",
                tint = if (filled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(size.dp)
                    .then(
                        if (onRatingChange != null) {
                            Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onRatingChange(star) },
                            )
                        } else Modifier,
                    ),
            )
            if (star < 5) Spacer(Modifier.width(2.dp))
        }
    }
}

@Composable
fun LevelBar(pointsTotal: Int, modifier: Modifier = Modifier) {
    val progress = ScoreRules.levelProgress(pointsTotal)
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Nivo ${progress.level}", style = MaterialTheme.typography.labelLarge)
            Text(
                "${progress.into} / ${progress.needed}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress.fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
        )
    }
}

/**
 * Loads a stored photo and decodes it off the main thread.
 *
 * Photos live in Firestore rather than behind a URL, so there is nothing for an
 * image library to fetch - the bytes are read once, cached by [PhotoSource] and
 * decoded here.
 */
@Composable
fun rememberPhoto(photoId: String?): ImageBitmap? {
    var image by remember(photoId) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(photoId) {
        image = photoId?.let { id ->
            val bytes = runCatching { AppContainer.repository.photo(id) }.getOrNull()
            bytes?.let {
                withContext(Dispatchers.Default) {
                    BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
                }
            }
        }
    }
    return image
}

@Composable
fun PhotoThumb(photoId: String?, modifier: Modifier = Modifier, corner: Int = 12) {
    val image = rememberPhoto(photoId)
    if (image == null) {
        Box(
            modifier
                .clip(RoundedCornerShape(corner.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text("☕", style = MaterialTheme.typography.headlineMedium)
        }
    } else {
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(corner.dp)),
        )
    }
}

@Composable
fun Avatar(photoId: String?, displayName: String, size: Int = 40, modifier: Modifier = Modifier) {
    val image = rememberPhoto(photoId)
    Box(
        modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = displayName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
