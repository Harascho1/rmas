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
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
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

    val qrLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        viewModel.onQrScanned(result.contents)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check-in") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
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

                SectionCard(title = "Kako potvrdjujete posetu?") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.method == CheckInMethod.GPS,
                            onClick = { viewModel.onMethodChange(CheckInMethod.GPS) },
                            label = { Text("GPS") },
                        )
                        FilterChip(
                            selected = state.method == CheckInMethod.QR,
                            onClick = { viewModel.onMethodChange(CheckInMethod.QR) },
                            label = { Text("QR kod") },
                        )
                        FilterChip(
                            selected = state.method == CheckInMethod.HONOR,
                            onClick = { viewModel.onMethodChange(CheckInMethod.HONOR) },
                            label = { Text("Bez dokaza") },
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    when (state.method) {
                        CheckInMethod.GPS -> {
                            val distance = formatDistance(state.distanceMeters)
                            Text(
                                when {
                                    state.fix == null -> "Lokacija jos nije ocitana."
                                    state.inRange -> "Udaljenost: $distance — mozete da potvrdite posetu."
                                    else -> "Udaljenost: $distance — priblizite se na ${Geo.MAX_CHECKIN_DISTANCE_M.toInt()} m."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (state.inRange) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                            )
                            // A network fix can be a kilometre out, which fails the
                            // 150 m rule through no fault of the user - so say so.
                            state.fix?.let { fix ->
                                val accuracy = fix.accuracyMeters?.let { " (~${it.toInt()} m)" }.orEmpty()
                                Text(
                                    if (fix.source == LocationFix.Source.NETWORK) {
                                        "Izvor: ${fix.source.label}$accuracy — ukljucite GPS za precizniju lokaciju."
                                    } else {
                                        "Izvor: ${fix.source.label}$accuracy"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = viewModel::refreshLocation) {
                                Text("Osvezi lokaciju")
                            }
                        }

                        CheckInMethod.QR -> {
                            Text(
                                if (state.qrToken == null) {
                                    "Zamolite osoblje da prikaze QR kod, pa ga skenirajte. Nosi +${ScoreBonus.QR} poena."
                                } else {
                                    "QR kod je uspesno skeniran."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    qrLauncher.launch(
                                        ScanOptions()
                                            .setPrompt("Skenirajte QR kod kafica")
                                            .setBeepEnabled(false)
                                            .setOrientationLocked(false),
                                    )
                                },
                            ) {
                                Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                                Text("  Skeniraj QR")
                            }
                        }

                        CheckInMethod.HONOR -> Text(
                            "Bez dokaza vredi upola manje i ide administratoru na proveru pre nego sto poeni budu dodeljeni.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                SectionCard(title = "Slika kafe (+${ScoreBonus.PHOTO} poena)") {
                    val photo = state.photo
                    if (photo != null) {
                        Image(
                            bitmap = photo.asImageBitmap(),
                            contentDescription = "Slika kafe",
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
                        Text(if (photo == null) "  Slikaj kafu" else "  Slikaj ponovo")
                    }
                }

                SectionCard(title = "Ocena (opciono)") {
                    StarRating(state.rating, size = 30, onRatingChange = viewModel::onRatingChange)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.comment,
                        onValueChange = viewModel::onCommentChange,
                        placeholder = { Text("Komentar") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                }

                SectionCard(title = "Procena poena") {
                    val preview = state.preview
                    ScoreRow("Osnovni check-in", preview.base)
                    if (preview.firstVisitBonus > 0) ScoreRow("Prvi put ovde", preview.firstVisitBonus)
                    if (preview.photoBonus > 0) ScoreRow("Slika", preview.photoBonus)
                    if (preview.qrBonus > 0) ScoreRow("QR potvrda", preview.qrBonus)
                    if (preview.streakBonus > 0) ScoreRow("Streak", preview.streakBonus)
                    if (preview.honorPenalty != 0) ScoreRow("Bez dokaza", preview.honorPenalty)
                    if (preview.challengeMultiplier > 1.0) {
                        ScoreRow("Izazov x${preview.challengeMultiplier}", 0, suffix = true)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Ukupno: ${points(preview.total)}",
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
                    Text(if (state.submitting) "Salje se..." else "Popio sam kafu ☕")
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
                if (value >= 0) "+$value" else "$value",
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
        Text("☕", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            if (result.checkIn.pointsAwarded > 0) "+${result.checkIn.pointsAwarded}" else "Na proveri",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(result.checkIn.cafeName, style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Ukupno ${result.newTotalPoints}")
            Pill("Nivo ${result.level}")
            Pill("Streak ${result.streakDays}")
        }

        if (result.checkIn.flagReason != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Check-in ceka odobrenje: ${result.checkIn.flagReason}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (result.newBadges.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("Novi bedzevi", style = MaterialTheme.typography.titleMedium)
            result.newBadges.forEach {
                Text("${it.emoji} ${it.title}", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Gotovo")
        }
    }
}

/** Mirrors the shared scoring constants for use in the copy above. */
private object ScoreBonus {
    const val QR = rs.coffeeconquest.shared.rules.ScoreRules.QR_BONUS
    const val PHOTO = rs.coffeeconquest.shared.rules.ScoreRules.PHOTO_BONUS
}
