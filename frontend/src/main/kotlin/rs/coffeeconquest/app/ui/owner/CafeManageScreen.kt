package rs.coffeeconquest.app.ui.owner

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import rs.coffeeconquest.app.ui.common.Pill
import rs.coffeeconquest.app.ui.common.SectionCard
import rs.coffeeconquest.app.ui.common.StarRating
import rs.coffeeconquest.app.ui.common.StateContent
import rs.coffeeconquest.app.ui.formatDay
import rs.coffeeconquest.app.ui.formatRating
import rs.coffeeconquest.app.ui.relativeTime
import rs.coffeeconquest.shared.dto.DailyCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafeManageScreen(
    cafeId: String,
    onBack: () -> Unit,
    viewModel: CafeManageViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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
                title = { Text("Upravljanje kaficem") },
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
            state = state.cafe,
            onRetry = { viewModel.load(cafeId) },
            modifier = Modifier.padding(padding),
        ) { cafe ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(cafe.name, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        cafe.address ?: cafe.city.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                state.stats?.let { stats ->
                    item {
                        SectionCard(title = "Statistika") {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                Metric("${stats.totalCheckIns}", "ukupno")
                                Metric("${stats.checkInsLast7Days}", "7 dana")
                                Metric("${stats.checkInsLast30Days}", "30 dana")
                                Metric("${stats.uniqueVisitors}", "gostiju")
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                Metric(formatRating(stats.averageRating), "prosek")
                                Metric("${stats.reviewCount}", "recenzija")
                                Metric("${stats.unansweredReviews}", "bez odgovora")
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Check-inovi po danu", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(8.dp))
                            DailyChart(stats.dailyCheckIns)
                        }
                    }

                    if (stats.topVisitors.isNotEmpty()) {
                        item {
                            SectionCard(title = "Najverniji gosti") {
                                stats.topVisitors.forEachIndexed { index, visitor ->
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("${index + 1}.", style = MaterialTheme.typography.titleMedium)
                                        Spacer(Modifier.width(10.dp))
                                        Text(visitor.displayName, Modifier.weight(1f))
                                        Pill("${visitor.visits} poseta")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SectionCard(title = "Bonus izazov") {
                        Text(
                            "Podignite broj poena da privucete goste. Vazi samo za ovaj kafic.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = state.challengeTitle,
                            onValueChange = viewModel::onChallengeTitleChange,
                            label = { Text("Naziv, npr. Dupli poeni danas") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1.5, 2.0, 3.0).forEach { multiplier ->
                                FilterChip(
                                    selected = state.challengeMultiplier == multiplier,
                                    onClick = { viewModel.onChallengeMultiplierChange(multiplier) },
                                    label = { Text("x$multiplier") },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 3, 7).forEach { days ->
                                FilterChip(
                                    selected = state.challengeDays == days,
                                    onClick = { viewModel.onChallengeDaysChange(days) },
                                    label = { Text("$days d") },
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = viewModel::createChallenge,
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Pokreni izazov") }

                        state.challenges.forEach { challenge ->
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(challenge.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "x${challenge.multiplier} do ${formatDay(challenge.endsAtEpochMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(onClick = { viewModel.deleteChallenge(challenge.id) }) {
                                    Text("Ukini")
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Recenzije (${state.reviews.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                items(state.reviews, key = { it.id }) { review ->
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                            Spacer(Modifier.height(6.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }

                        if (review.ownerReply != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Vas odgovor: ${review.ownerReply}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.replyDraft[review.id].orEmpty(),
                                onValueChange = { viewModel.onReplyDraftChange(review.id, it) },
                                placeholder = { Text("Odgovorite gostu") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(6.dp))
                            Button(
                                onClick = { viewModel.sendReply(review.id) },
                                enabled = !state.busy,
                            ) { Text("Posalji odgovor") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Metric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A bar chart small enough to draw with plain boxes - no chart library needed. */
@Composable
private fun DailyChart(days: List<DailyCount>) {
    val max = (days.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(90.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { day ->
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text("${day.count}", style = MaterialTheme.typography.labelSmall)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((60 * day.count / max).dp.coerceAtLeast(3.dp))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}
