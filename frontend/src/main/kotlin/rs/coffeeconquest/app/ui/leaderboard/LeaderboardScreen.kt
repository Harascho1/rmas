package rs.coffeeconquest.app.ui.leaderboard

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import rs.coffeeconquest.app.ui.common.Avatar
import rs.coffeeconquest.app.ui.common.EmptyBox
import rs.coffeeconquest.app.ui.common.Pill
import rs.coffeeconquest.app.ui.common.StateContent
import rs.coffeeconquest.app.ui.points
import rs.coffeeconquest.shared.dto.LeaderboardEntry
import rs.coffeeconquest.shared.dto.UserProfile
import rs.coffeeconquest.shared.model.LeaderboardScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    profile: UserProfile,
    onUserClick: (String) -> Unit,
    viewModel: LeaderboardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(profile.id) { viewModel.start(profile.city) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Rang lista") }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            val scopes = listOf(
                LeaderboardScope.GLOBAL to "Globalno",
                LeaderboardScope.CITY to (profile.city ?: "Grad"),
                LeaderboardScope.FRIENDS to "Prijatelji",
            )
            PrimaryTabRow(selectedTabIndex = scopes.indexOfFirst { it.first == state.scope }) {
                scopes.forEach { (scope, label) ->
                    Tab(
                        selected = state.scope == scope,
                        onClick = { viewModel.onScopeChange(scope) },
                        text = { Text(label, style = MaterialTheme.typography.labelLarge) },
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !state.weekly,
                    onClick = { viewModel.onPeriodChange(false) },
                    label = { Text("Sve vreme") },
                )
                FilterChip(
                    selected = state.weekly,
                    onClick = { viewModel.onPeriodChange(true) },
                    label = { Text("Ova nedelja") },
                )
            }

            state.champion?.user?.let { champion ->
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("👑", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Sampion grada ${state.champion?.city}",
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(champion.displayName, style = MaterialTheme.typography.titleMedium)
                        }
                        Text(points(champion.points), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            StateContent(state = state.board, onRetry = viewModel::load) { board ->
                if (board.entries.isEmpty()) {
                    EmptyBox(
                        when (state.scope) {
                            LeaderboardScope.FRIENDS -> "Jos ne pratite nikoga. Otvorite tudji profil i pritisnite Zaprati."
                            LeaderboardScope.CITY -> "Za ovaj grad jos nema rezultata."
                            LeaderboardScope.GLOBAL -> "Jos niko nije skupio poene."
                        },
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(board.entries, key = { it.userId }) { entry ->
                            LeaderboardRow(entry, onClick = { onUserClick(entry.userId) })
                        }

                        // The signed-in user always sees their own row, even outside the top 50.
                        val me = board.me
                        if (me != null && board.entries.none { it.isMe }) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                LeaderboardRow(me.copy(isMe = true), onClick = { onUserClick(me.userId) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isMe) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(medalColor(entry.rank)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${entry.rank}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Avatar(entry.avatarPhotoId, entry.displayName, size = 36)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "@${entry.username} · ${entry.checkInCount} check-inova",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(points(entry.points), style = MaterialTheme.typography.titleMedium)
                Pill("Nivo ${entry.level}")
            }
        }
    }
}

@Composable
private fun medalColor(rank: Int) = when (rank) {
    1 -> MaterialTheme.colorScheme.secondary
    2 -> MaterialTheme.colorScheme.surfaceVariant
    3 -> MaterialTheme.colorScheme.tertiaryContainer
    else -> MaterialTheme.colorScheme.surfaceVariant
}
