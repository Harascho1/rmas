package rs.coffeeconquest.app.ui.checkin

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import rs.coffeeconquest.app.R
import rs.coffeeconquest.app.data.LocationFix
import rs.coffeeconquest.app.ui.common.Pill
import rs.coffeeconquest.app.ui.common.SectionCard
import rs.coffeeconquest.app.ui.common.StarRating
import rs.coffeeconquest.app.ui.common.StateContent
import rs.coffeeconquest.app.ui.formatDistance
import rs.coffeeconquest.app.ui.points
import rs.coffeeconquest.shared.model.CheckInMethod
import rs.coffeeconquest.shared.rules.Geo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    cafeId: String,
    onBack: () -> Unit,
    onFinished: () -> Unit,
    viewModel: CheckInViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(cafeId) { viewModel.load(cafeId) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { viewModel.onPhotoTaken(it) }

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) cameraLauncher.launch(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.checkin_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.checkin_back))
                    }
                },
            )
        },
    ) { padding ->
        val result = state.result
        if (result != null) {
            CheckInResultView(
                result = result,
                onDone = onFinished,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            return@Scaffold
        }

        StateContent(
            state = state.cafe,
            onRetry = { viewModel.load(cafeId) },
            modifier = Modifier.padding(padding),
        ) { cafe ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(cafe.name, style = MaterialTheme.typography.headlineMedium)
                Text(
                    cafe.address ?: cafe.city.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SectionCard(title = stringResource(R.string.checkin_method_section_title)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.method == CheckInMethod.GPS,
                            onClick = { viewModel.onMethodChange(CheckInMethod.GPS) },
                            label = { Text(stringResource(R.string.checkin_method_gps)) },
                        )
                        FilterChip(
                            selected = state.method == CheckInMethod.HONOR,
                            onClick = { viewModel.onMethodChange(CheckInMethod.HONOR) },
                            label = { Text(stringResource(R.string.checkin_method_honor)) },
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    when (state.method) {
                        CheckInMethod.GPS -> {
                            val distance = formatDistance(state.distanceMeters)
                            Text(
                                when {
                                    state.fix == null -> stringResource(R.string.checkin_location_not_read)
                                    state.inRange -> stringResource(R.string.checkin_distance_in_range, distance.orEmpty())
                                    else -> stringResource(
                                        R.string.checkin_distance_out_of_range,
                                        distance.orEmpty(),
                                        Geo.MAX_CHECKIN_DISTANCE_M.toInt(),
                                    )
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (state.inRange) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                            )
                            state.fix?.let { fix ->
                                val accuracy = fix.accuracyMeters?.let {
                                    stringResource(R.string.checkin_location_accuracy_suffix, it.toInt())
                                }.orEmpty()
                                Text(
                                    if (fix.source == LocationFix.Source.NETWORK) {
                                        stringResource(R.string.checkin_location_source_network, fix.source.label, accuracy)
                                    } else {
                                        stringResource(R.string.checkin_location_source_other, fix.source.label, accuracy)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = viewModel::refreshLocation) {
                                Text(stringResource(R.string.checkin_button_refresh_location))
                            }
                        }

                        CheckInMethod.QR -> Unit

                        CheckInMethod.HONOR -> Text(
                            stringResource(R.string.checkin_honor_warning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                SectionCard(title = stringResource(R.string.checkin_photo_section_title, ScoreBonus.PHOTO)) {
                    val photo = state.photo
                    if (photo != null) {
                        Image(
                            bitmap = photo.asImageBitmap(),
                            contentDescription = stringResource(R.string.checkin_photo_content_description),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedButton(
                        onClick = { cameraPermission.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                        Text(
                            if (photo == null) stringResource(R.string.checkin_button_take_photo)
                            else stringResource(R.string.checkin_button_retake_photo),
                        )
                    }
                }

                SectionCard(title = stringResource(R.string.checkin_rating_section_title)) {
                    StarRating(state.rating, size = 30, onRatingChange = viewModel::onRatingChange)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.comment,
                        onValueChange = viewModel::onCommentChange,
                        placeholder = { Text(stringResource(R.string.checkin_comment_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                }

                SectionCard(title = stringResource(R.string.checkin_score_section_title)) {
                    val preview = state.preview
                    ScoreRow(stringResource(R.string.checkin_score_base), preview.base)
                    if (preview.firstVisitBonus > 0) {
                        ScoreRow(stringResource(R.string.checkin_score_first_visit), preview.firstVisitBonus)
                    }
                    if (preview.photoBonus > 0) ScoreRow(stringResource(R.string.checkin_score_photo), preview.photoBonus)
                    if (preview.streakBonus > 0) ScoreRow(stringResource(R.string.checkin_score_streak), preview.streakBonus)
                    if (preview.honorPenalty != 0) {
                        ScoreRow(stringResource(R.string.checkin_method_honor), preview.honorPenalty)
                    }
                    if (preview.challengeMultiplier > 1.0) {
                        ScoreRow(
                            stringResource(R.string.checkin_score_challenge, preview.challengeMultiplier),
                            0,
                            suffix = true,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.checkin_total_points, points(preview.total)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Button(
                    onClick = viewModel::submit,
                    enabled = state.canSubmit,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(
                        if (state.submitting) stringResource(R.string.checkin_button_submit_sending)
                        else stringResource(R.string.checkin_button_submit),
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ScoreRow(label: String, value: Int, suffix: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        if (!suffix) {
            Text(
                if (value >= 0) stringResource(R.string.checkin_points_positive, value) else "$value",
                style = MaterialTheme.typography.bodyMedium,
                color = if (value >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun CheckInResultView(
    result: rs.coffeeconquest.shared.dto.CheckInResult,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.checkin_result_emoji), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            if (result.checkIn.pointsAwarded > 0) {
                stringResource(R.string.checkin_points_positive, result.checkIn.pointsAwarded)
            } else {
                stringResource(R.string.checkin_result_points_pending)
            },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(result.checkIn.cafeName, style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(stringResource(R.string.checkin_result_total_points, result.newTotalPoints))
            Pill(stringResource(R.string.checkin_result_level, result.level))
            Pill(stringResource(R.string.checkin_result_streak, result.streakDays))
        }

        val flagReason = result.checkIn.flagReason
        if (flagReason != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.checkin_result_flag_reason, flagReason),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (result.newBadges.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.checkin_result_new_badges_title), style = MaterialTheme.typography.titleMedium)
            result.newBadges.forEach {
                Text(
                    stringResource(R.string.checkin_result_badge_item, it.emoji, it.title),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.checkin_button_done))
        }
    }
}

private object ScoreBonus {
    const val PHOTO = rs.coffeeconquest.shared.rules.ScoreRules.PHOTO_BONUS
}
