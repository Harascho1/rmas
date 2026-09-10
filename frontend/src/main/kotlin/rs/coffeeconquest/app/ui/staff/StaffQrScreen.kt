package rs.coffeeconquest.app.ui.staff

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import rs.coffeeconquest.app.ui.common.EmptyBox
import rs.coffeeconquest.app.ui.common.StateContent

/**
 * The staff screen: one big QR code that a guest scans to prove they are really
 * in the cafe. The code rotates, so a screenshot is worthless a few minutes later.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffQrScreen(viewModel: StaffQrViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(topBar = { TopAppBar(title = { Text("QR potvrda") }) }) { padding ->
        StateContent(
            state = state.cafes,
            onRetry = viewModel::load,
            modifier = Modifier.padding(padding),
        ) { cafes ->
            if (cafes.isEmpty()) {
                EmptyBox("Niste dodeljeni nijednom kaficu. Zamolite vlasnika da vas doda kao osoblje.")
            } else {
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (cafes.size > 1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            cafes.forEach { cafe ->
                                FilterChip(
                                    selected = state.selectedCafeId == cafe.id,
                                    onClick = { viewModel.select(cafe.id) },
                                    label = { Text(cafe.name) },
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    val bitmap = state.qrBitmap
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR kod za check-in",
                            modifier = Modifier.size(280.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            state.token?.cafeName.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            "Kod istice ${state.secondsLeft}s - gost ga skenira pri check-inu.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Text(
                            "Pritisnite dugme da generisete kod.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = viewModel::refreshToken,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) {
                        Text(if (state.qrBitmap == null) "Generisi QR kod" else "Novi kod")
                    }

                    state.error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
