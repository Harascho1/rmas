package rs.coffeeconquest.app.ui.cafe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import rs.coffeeconquest.app.ui.map.OsmMap
import rs.coffeeconquest.shared.dto.UserProfile
import rs.coffeeconquest.shared.model.CafeAttributes
import rs.coffeeconquest.shared.model.CafeType
import rs.coffeeconquest.shared.model.Role

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCafeScreen(
    profile: UserProfile,
    onBack: () -> Unit,
    onCreated: () -> Unit,
    viewModel: AddCafeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.useCurrentLocation() }
    LaunchedEffect(state.created) { if (state.created) onCreated() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (profile.role == Role.HUNTER) "Predlozi kafic" else "Novi kafic") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                when (profile.role) {
                    Role.HUNTER -> "Predlog ide administratoru na odobrenje i pojavice se na mapi kada bude prihvacen."
                    Role.OWNER -> "Kafic koji napravite postaje vas, ali ceka odobrenje administratora."
                    else -> "Kao administrator, kafic odmah ide na mapu."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Naziv kafica") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.address,
                onValueChange = viewModel::onAddressChange,
                label = { Text("Adresa") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.city,
                onValueChange = viewModel::onCityChange,
                label = { Text("Grad") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Opis") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            OutlinedTextField(
                value = state.openingHours,
                onValueChange = viewModel::onOpeningHoursChange,
                label = { Text("Radno vreme (npr. 08-23)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Tip", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CafeType.entries.forEach { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { viewModel.onTypeChange(type) },
                        label = { Text(type.label) },
                    )
                }
            }

            Text("Atributi", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CafeAttributes.all.forEach { tag ->
                    FilterChip(
                        selected = tag in state.attributes,
                        onClick = { viewModel.toggleAttribute(tag) },
                        label = { Text("#$tag") },
                    )
                }
            }

            OutlinedTextField(
                value = state.tags,
                onValueChange = viewModel::onTagsChange,
                label = { Text("Jos tagova, odvojeni zarezom") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Lokacija", style = MaterialTheme.typography.titleSmall)
            OsmMap(
                center = state.mapCenter,
                cafes = emptyList(),
                showUserMarker = false,
                onCafeClick = {},
                pickedPoint = state.pin,
                onMapTap = viewModel::onMapPick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = viewModel::useCurrentLocation) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Moja lokacija")
                }
                Text(
                    "${state.latitude.format()} , ${state.longitude.format()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                if (state.resolvingAddress) {
                    "Trazim adresu za izabranu tacku..."
                } else {
                    "Dodirnite mapu da postavite pin - adresa i grad se popunjavaju sami, " +
                        "a sve sto sami upisete ostaje netaknuto."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = viewModel::submit,
                enabled = state.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Text(if (state.submitting) "Salje se..." else "Posalji")
            }
        }
    }
}

private fun Double.format(): String = String.format(java.util.Locale.US, "%.5f", this)
