package rs.coffeeconquest.app.ui.owner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import rs.coffeeconquest.app.ui.common.EmptyBox
import rs.coffeeconquest.app.ui.common.Pill
import rs.coffeeconquest.app.ui.common.StateContent
import rs.coffeeconquest.app.ui.formatRating
import rs.coffeeconquest.shared.model.CafeStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerScreen(
    onCafeClick: (String) -> Unit,
    onAddCafe: () -> Unit,
    viewModel: OwnerViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Moji kafici") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCafe) {
                Icon(Icons.Filled.Add, contentDescription = "Dodaj kafic")
            }
        },
    ) { padding ->
        StateContent(
            state = state.cafes,
            onRetry = viewModel::load,
            modifier = Modifier.padding(padding),
        ) { cafes ->
            if (cafes.isEmpty()) {
                EmptyBox("Jos nemate nijedan kafic. Dodajte ga preko + dugmeta - administrator ga zatim odobrava.")
            } else {
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(cafes, key = { it.id }) { cafe ->
                        Card(Modifier.fillMaxWidth().clickable { onCafeClick(cafe.id) }) {
                            Column(Modifier.padding(14.dp)) {
                                Text(cafe.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    cafe.address ?: cafe.city.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    when (cafe.status) {
                                        CafeStatus.APPROVED -> Pill("na mapi")
                                        CafeStatus.PENDING -> Pill(
                                            "ceka odobrenje",
                                            background = MaterialTheme.colorScheme.errorContainer,
                                            foreground = MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                        CafeStatus.REJECTED -> Pill(
                                            "odbijen",
                                            background = MaterialTheme.colorScheme.errorContainer,
                                            foreground = MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                    }
                                    Pill("★ ${formatRating(cafe.averageRating)}")
                                    Pill("${cafe.checkInCount} poseta")
                                    if (cafe.activeChallenges.isNotEmpty()) Pill("izazov aktivan")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
