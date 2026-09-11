package rs.coffeeconquest.app.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import rs.coffeeconquest.app.R
import rs.coffeeconquest.app.ui.common.Avatar
import rs.coffeeconquest.app.ui.common.EmptyBox
import rs.coffeeconquest.app.ui.common.Pill
import rs.coffeeconquest.app.ui.common.PhotoThumb
import rs.coffeeconquest.app.ui.common.StateContent
import rs.coffeeconquest.app.ui.common.rememberVisiblePhoto
import rs.coffeeconquest.app.ui.relativeTime
import rs.coffeeconquest.shared.dto.FeedItem
import rs.coffeeconquest.shared.model.FeedEventType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onCafeClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: FeedViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.feed_title)) }) }) { padding ->
        Column(Modifier
            .fillMaxSize()
            .padding(padding)) {
            PrimaryTabRow(selectedTabIndex = if (state.followingOnly) 1 else 0) {
                Tab(
                    selected = !state.followingOnly,
                    onClick = { viewModel.onScopeChange(false) },
                    text = { Text(stringResource(R.string.feed_tab_all)) },
                )
                Tab(
                    selected = state.followingOnly,
                    onClick = { viewModel.onScopeChange(true) },
                    text = { Text(stringResource(R.string.feed_tab_following)) },
                )
            }

            StateContent(state = state.items, onRetry = viewModel::load) { items ->
                if (items.isEmpty()) {
                    EmptyBox(
                        if (state.followingOnly) stringResource(R.string.feed_empty_following)
                        else stringResource(R.string.feed_empty_all),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(items, key = { it.id }) { item ->
                            FeedRow(
                                item = item,
                                photo = rememberVisiblePhoto(
                                    item.photoId,
                                    photos,
                                    viewModel::requestPhoto
                                ),
                                onCafeClick = onCafeClick,
                                onUserClick = onUserClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedRow(
    item: FeedItem,
    photo: ImageBitmap?,
    onCafeClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable { item.cafeId?.let(onCafeClick) ?: onUserClick(item.actorId) },
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(null, item.actorDisplayName, size = 36, modifier = Modifier.clickable { onUserClick(item.actorId) })
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.text, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        relativeTime(item.createdAtEpochMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item.points?.let { Pill(stringResource(R.string.feed_points_positive, it)) }
            }

            if (item.photoId != null) {
                Spacer(Modifier.height(10.dp))
                PhotoThumb(photo, Modifier
                    .fillMaxWidth()
                    .height(180.dp))
            }

            if (item.type == FeedEventType.CHALLENGE_CREATED) {
                Spacer(Modifier.height(8.dp))
                Pill(
                    stringResource(R.string.feed_badge_challenge),
                    background = MaterialTheme.colorScheme.errorContainer,
                    foreground = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}
