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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import rs.coffeeconquest.app.R
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
                title = { Text(stringResource(R.string.cafemanage_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cafemanage_back))
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
                        SectionCard(title = stringResource(R.string.cafemanage_section_stats)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                Metric("${stats.totalCheckIns}", stringResource(R.string.cafemanage_metric_total))
                                Metric("${stats.checkInsLast7Days}", stringResource(R.string.cafemanage_metric_7_days))
                                Metric("${stats.checkInsLast30Days}", stringResource(R.string.cafemanage_metric_30_days))
                                Metric("${stats.uniqueVisitors}", stringResource(R.string.cafemanage_metric_guests))
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                Metric(formatRating(stats.averageRating), stringResource(R.string.cafemanage_metric_average))
                                Metric("${stats.reviewCount}", stringResource(R.string.cafemanage_metric_reviews))
                                Metric("${stats.unansweredReviews}", stringResource(R.string.cafemanage_metric_unanswered))
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.cafemanage_checkins_per_day), style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(8.dp))
                            DailyChart(stats.dailyCheckIns)
                        }
                    }

                    if (stats.topVisitors.isNotEmpty()) {
                        item {
                            SectionCard(title = stringResource(R.string.cafemanage_section_top_visitors)) {
                                stats.topVisitors.forEachIndexed { index, visitor ->
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            stringResource(R.string.cafemanage_rank_format, index + 1),
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(visitor.displayName, Modifier.weight(1f))
                                        Pill(stringResource(R.string.cafemanage_visits_count, visitor.visits))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SectionCard(title = stringResource(R.string.cafemanage_section_bonus_challenge)) {
                        Text(
                            stringResource(R.string.cafemanage_challenge_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = state.challengeTitle,
                            onValueChange = viewModel::onChallengeTitleChange,
                            label = { Text(stringResource(R.string.cafemanage_challenge_title_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1.5, 2.0, 3.0).forEach { multiplier ->
                                FilterChip(
                                    selected = state.challengeMultiplier == multiplier,
                                    onClick = { viewModel.onChallengeMultiplierChange(multiplier) },
                                    label = { Text(stringResource(R.string.cafemanage_multiplier_format, multiplier)) },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 3, 7).forEach { days ->
                                FilterChip(
                                    selected = state.challengeDays == days,
                                    onClick = { viewModel.onChallengeDaysChange(days) },
                                    label = { Text(stringResource(R.string.cafemanage_days_format, days)) },
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = viewModel::createChallenge,
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.cafemanage_start_challenge)) }

                        state.challenges.forEach { challenge ->
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(challenge.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        stringResource(
                                            R.string.cafemanage_challenge_summary,
                                            challenge.multiplier,
                                            formatDay(challenge.endsAtEpochMs),
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(onClick = { viewModel.deleteChallenge(challenge.id) }) {
                                    Text(stringResource(R.string.cafemanage_cancel_challenge))
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        stringResource(R.string.cafemanage_reviews_count, state.reviews.size),
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

                        val ownerReply = review.ownerReply
                        if (ownerReply != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.cafemanage_owner_reply, ownerReply),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.replyDraft[review.id].orEmpty(),
                                onValueChange = { viewModel.onReplyDraftChange(review.id, it) },
                                placeholder = { Text(stringResource(R.string.cafemanage_reply_placeholder)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(6.dp))
                            Button(
                                onClick = { viewModel.sendReply(review.id) },
                                enabled = !state.busy,
                            ) { Text(stringResource(R.string.cafemanage_send_reply)) }
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
