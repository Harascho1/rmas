package rs.coffeeconquest.app.ui.cafe

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import rs.coffeeconquest.app.R
import rs.coffeeconquest.app.ui.common.Avatar
import rs.coffeeconquest.app.ui.common.Pill
import rs.coffeeconquest.app.ui.common.PhotoThumb
import rs.coffeeconquest.app.ui.common.SectionCard
import rs.coffeeconquest.app.ui.common.StarRating
import rs.coffeeconquest.app.ui.common.StateContent
import rs.coffeeconquest.app.ui.UiState
import rs.coffeeconquest.app.ui.common.rememberVisiblePhoto
import rs.coffeeconquest.app.ui.formatDistance
import rs.coffeeconquest.app.ui.formatRating
import rs.coffeeconquest.app.ui.relativeTime
import rs.coffeeconquest.shared.dto.Cafe
import rs.coffeeconquest.shared.dto.UserProfile
import rs.coffeeconquest.shared.model.CheckInStatus
import rs.coffeeconquest.shared.model.Role
import rs.coffeeconquest.shared.rules.Geo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafeDetailScreen(
    cafeId: String,
    profile: UserProfile,
    onBack: () -> Unit,
    onCheckIn: (String) -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: CafeDetailViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(cafeId) { viewModel.load(cafeId) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text((state.cafe as? UiState.Ready)?.data?.name ?: stringResource(R.string.cafedetail_title_fallback)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cafedetail_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (profile.role == Role.HUNTER || profile.role == Role.ADMIN) {
                ExtendedFloatingActionButton(
                    onClick = { onCheckIn(cafeId) },
                    icon = { Icon(Icons.Filled.LocalCafe, contentDescription = null) },
                    text = { Text(stringResource(R.string.cafedetail_button_checkin)) },
                )
            }
        },
    ) { padding ->
        StateContent(
            state = state.cafe,
            onRetry = { viewModel.load(cafeId) },
            modifier = Modifier.padding(padding),
        ) { cafe ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    CafeHeader(
                        cafe,
                        rememberVisiblePhoto(cafe.photoId, photos, viewModel::requestPhoto)
                    )
                }

                if (cafe.activeChallenges.isNotEmpty()) {
                    item {
                        SectionCard(title = stringResource(R.string.cafedetail_section_active_challenges)) {
                            cafe.activeChallenges.forEach { challenge ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(challenge.title, style = MaterialTheme.typography.titleMedium)
                                        challenge.description?.let {
                                            Text(it, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    Pill(
                                        stringResource(R.string.cafedetail_challenge_multiplier, challenge.multiplier),
                                        background = MaterialTheme.colorScheme.errorContainer,
                                        foreground = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SectionCard(title = stringResource(R.string.cafedetail_section_leave_rating)) {
                        StarRating(
                            rating = state.myRating,
                            size = 32,
                            onRatingChange = viewModel::onRatingChange,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = state.myComment,
                            onValueChange = viewModel::onCommentChange,
                            placeholder = { Text(stringResource(R.string.cafedetail_placeholder_comment)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = viewModel::submitReview,
                            enabled = !state.submittingReview,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (state.submittingReview) {
                                    stringResource(R.string.cafedetail_button_sending_review)
                                } else {
                                    stringResource(R.string.cafedetail_button_submit_review)
                                },
                            )
                        }
                    }
                }

                if (state.recentCheckIns.isNotEmpty()) {
                    item {
                        SectionCard(title = stringResource(R.string.cafedetail_section_recent_checkins)) {
                            state.recentCheckIns.take(8).forEach { checkIn ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onUserClick(checkIn.userId) }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Avatar(null, checkIn.username, size = 32)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(checkIn.username, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            relativeTime(checkIn.createdAtEpochMs),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (checkIn.status == CheckInStatus.VALID) {
                                        Pill(stringResource(R.string.cafedetail_points_awarded, checkIn.pointsAwarded))
                                    } else {
                                        Pill(
                                            stringResource(R.string.cafedetail_status_pending),
                                            background = MaterialTheme.colorScheme.errorContainer,
                                            foreground = MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        stringResource(R.string.cafedetail_reviews_count, state.reviews.size),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                items(state.reviews, key = { it.id }) { review ->
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(null, review.authorDisplayName, size = 32)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(review.authorDisplayName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    relativeTime(review.createdAtEpochMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            StarRating(review.rating)
                        }
                        review.comment?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                        review.ownerReply?.let { reply ->
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.cafedetail_owner_reply_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(reply, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun CafeHeader(cafe: Cafe, photo: ImageBitmap?) {
    Column {
        PhotoThumb(
            photo,
            Modifier
                .fillMaxWidth()
                .height(160.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(cafe.name, style = MaterialTheme.typography.headlineMedium)
        cafe.description?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(stringResource(R.string.cafedetail_rating_summary, formatRating(cafe.averageRating), cafe.reviewCount))
            Pill(stringResource(R.string.cafedetail_checkin_count, cafe.checkInCount))
            formatDistance(cafe.distanceMeters)?.let { Pill(it) }
        }
        Spacer(Modifier.height(10.dp))
        val tagHashFormat = stringResource(R.string.cafedetail_tag_hash)
        val tagSeparator = stringResource(R.string.cafedetail_tag_separator)
        listOfNotNull(
            cafe.address?.let { stringResource(R.string.cafedetail_address_line, it) },
            cafe.openingHours?.let { stringResource(R.string.cafedetail_hours_line, it) },
            stringResource(R.string.cafedetail_type_line, cafe.type.label),
            cafe.tags.takeIf { it.isNotEmpty() }
                ?.joinToString(tagSeparator) { String.format(tagHashFormat, it) },
        ).forEach {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        cafe.topVisitor?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.cafedetail_top_visitor, it.displayName, it.visits),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (cafe.distanceMeters != null && cafe.distanceMeters!! > Geo.MAX_CHECKIN_DISTANCE_M) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.cafedetail_checkin_distance_hint, Geo.MAX_CHECKIN_DISTANCE_M.toInt()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
