package rs.coffeeconquest.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import rs.coffeeconquest.app.ui.common.Avatar
import rs.coffeeconquest.app.ui.common.LevelBar
import rs.coffeeconquest.app.ui.common.Pill
import rs.coffeeconquest.app.ui.common.RoleChip
import rs.coffeeconquest.app.ui.common.SectionCard
import rs.coffeeconquest.app.ui.common.StateContent
import rs.coffeeconquest.app.ui.common.rememberVisiblePhoto
import rs.coffeeconquest.app.ui.days
import rs.coffeeconquest.app.ui.formatDay
import rs.coffeeconquest.app.ui.points
import rs.coffeeconquest.app.ui.relativeTime
import rs.coffeeconquest.shared.dto.UserProfile
import rs.coffeeconquest.shared.dto.UserStats
import rs.coffeeconquest.shared.model.CheckInStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: UserProfile,
    onSignOut: () -> Unit,
    onCafeClick: (String) -> Unit,
    onRefresh: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()

    LaunchedEffect(profile.id, profile.points) {
        viewModel.load(profile.id, ownProfile = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Moj profil") },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Odjava")
                    }
                },
            )
        },
    ) { padding ->
        StateContent(
            state = state.stats,
            onRetry = {
                onRefresh()
                viewModel.load(profile.id, ownProfile = true)
            },
            modifier = Modifier.padding(padding),
        ) { stats ->
            ProfileBody(
                stats = stats,
                state = state,
                avatar = rememberVisiblePhoto(
                    stats.profile.avatarPhotoId,
                    photos,
                    viewModel::requestPhoto,
                ),
                showFollowButton = false,
                onFollowToggle = {},
                onCafeClick = onCafeClick,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onCafeClick: (String) -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(userId) { viewModel.load(userId, ownProfile = false) }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        StateContent(
            state = state.stats,
            onRetry = { viewModel.load(userId, ownProfile = false) },
            modifier = Modifier.padding(padding),
        ) { stats ->
            ProfileBody(
                stats = stats,
                state = state,
                avatar = rememberVisiblePhoto(
                    stats.profile.avatarPhotoId,
                    photos,
                    viewModel::requestPhoto,
                ),
                showFollowButton = true,
                onFollowToggle = viewModel::toggleFollow,
                onCafeClick = onCafeClick,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun ProfileBody(
    stats: UserStats,
    state: ProfileUiState,
    avatar: ImageBitmap?,
    showFollowButton: Boolean,
    onFollowToggle: () -> Unit,
    onCafeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = stats.profile

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(avatar, profile.displayName, size = 64)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.displayName, style = MaterialTheme.typography.titleLarge)
                        if (profile.isCityChampion) {
                            Spacer(Modifier.width(6.dp))
                            Text("👑", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Text(
                        "@${profile.username}${profile.city?.let { " · $it" } ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    RoleChip(profile.role)
                }
            }
        }

        if (profile.isBanned) {
            item {
                Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        "Nalog je blokiran: ${profile.banReason ?: "bez obrazlozenja"}",
                        Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        if (showFollowButton) {
            item {
                if (profile.isFollowedByMe) {
                    OutlinedButton(
                        onClick = onFollowToggle,
                        enabled = !state.followBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Prestani da pratis") }
                } else {
                    Button(
                        onClick = onFollowToggle,
                        enabled = !state.followBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Zaprati") }
                }
            }
        }

        item {
            SectionCard {
                LevelBar(profile.points)
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatCell(points(profile.points), "poeni")
                    StatCell("${profile.checkInCount}", "check-inovi")
                    StatCell("${profile.conqueredCafes}", "kafica")
                    StatCell(days(profile.currentStreakDays), "streak")
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatCell("${profile.followerCount}", "prati vas")
                    StatCell("${profile.followingCount}", "pratite")
                    StatCell("${profile.reviewCount}", "recenzije")
                    StatCell(days(profile.longestStreakDays), "najduzi")
                }
            }
        }

        item {
            SectionCard(title = "Bedzevi (${stats.badges.size}/${state.badgeBoard.size})") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.badgeBoard, key = { it.code }) { badge ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(76.dp)
                                .alpha(if (badge.earned) 1f else 0.32f),
                        ) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(badge.emoji, style = MaterialTheme.typography.titleLarge)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                badge.title,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        if (stats.conquered.isNotEmpty()) {
            item {
                SectionCard(title = "Osvojeni kafici (${stats.conquered.size})") {
                    stats.conquered.take(10).forEach { cafe ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onCafeClick(cafe.cafeId) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(cafe.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${cafe.visits}x · poslednji put ${formatDay(cafe.lastVisitEpochMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (cafe.isTopVisitor) Pill("👑 osvajac")
                        }
                    }
                }
            }
        }

        if (state.history.isNotEmpty()) {
            item {
                Text(
                    "Istorija check-inova",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            items(state.history, key = { it.id }) { checkIn ->
                Card(Modifier
                    .fillMaxWidth()
                    .clickable { onCafeClick(checkIn.cafeId) }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(checkIn.cafeName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${relativeTime(checkIn.createdAtEpochMs)} · ${checkIn.method}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        when (checkIn.status) {
                            CheckInStatus.VALID -> Pill("+${checkIn.pointsAwarded}")
                            CheckInStatus.FLAGGED -> Pill(
                                "na proveri",
                                background = MaterialTheme.colorScheme.errorContainer,
                                foreground = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            CheckInStatus.INVALIDATED -> Pill(
                                "ponisten",
                                background = MaterialTheme.colorScheme.errorContainer,
                                foreground = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
