package rs.coffeeconquest.app.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import rs.coffeeconquest.app.ui.common.Pill
import rs.coffeeconquest.app.ui.common.PhotoThumb
import rs.coffeeconquest.app.ui.common.StateContent
import rs.coffeeconquest.app.ui.common.rememberVisiblePhoto
import rs.coffeeconquest.app.ui.dataOrNull
import rs.coffeeconquest.app.ui.formatDistance
import rs.coffeeconquest.app.ui.formatRating
import rs.coffeeconquest.shared.dto.Cafe
import rs.coffeeconquest.shared.dto.CafeFilter
import rs.coffeeconquest.shared.dto.UserProfile
import rs.coffeeconquest.shared.model.CafeAttributes
import rs.coffeeconquest.shared.model.CafeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    profile: UserProfile,
    onCafeClick: (String) -> Unit,
    onAddCafe: () -> Unit,
    viewModel: MapViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.locate() }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
        )
    }

    Box(Modifier.fillMaxSize()) {
        val cafes = state.cafes.dataOrNull.orEmpty()

        if (state.listMode) {
            CafeList(
                cafes = cafes,
                photos = photos,
                onPhotoVisible = viewModel::requestPhoto,
                onCafeClick = onCafeClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 88.dp),
            )
        } else {
            OsmMap(
                center = state.center,
                cafes = cafes,
                showUserMarker = state.hasFix,
                onCafeClick = viewModel::select,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Search bar and the filter button float above the map.
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.filter.query.orEmpty(),
                    onValueChange = viewModel::onQueryChange,
                    placeholder = { Text("Pretrazi kafice...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = viewModel::openFilters) {
                    BadgedBox(
                        badge = {
                            if (state.filter.activeCount > 0) Badge { Text("${state.filter.activeCount}") }
                        },
                    ) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filteri")
                    }
                }
            }
        }

        // Which provider answered - GPS is metres, the network is a neighbourhood.
        state.fixSource?.let { source ->
            val accuracy = state.fixAccuracyMeters?.let { " (~${it.toInt()} m)" }.orEmpty()
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 84.dp),
            ) {
                Text(
                    "Lokacija: ${source.label}$accuracy",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SmallFloatingActionButton(onClick = viewModel::toggleListMode) {
                Icon(
                    if (state.listMode) Icons.Filled.Map else Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = "Promeni prikaz",
                )
            }
            SmallFloatingActionButton(onClick = viewModel::locate) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Moja lokacija")
            }
            FloatingActionButton(onClick = onAddCafe) {
                Icon(Icons.Filled.Add, contentDescription = "Predlozi kafic")
            }
        }

        StateContent(state = state.cafes, onRetry = viewModel::load) { list ->
            if (list.isEmpty()) {
                Card(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp, start = 24.dp, end = 90.dp),
                ) {
                    Text(
                        buildString {
                            append("Nema kafica u krugu od ${state.radiusKm.toInt()} km")
                            if (state.filter.isActive) append(" za izabrane filtere")
                            append(". Predlozite novi preko + dugmeta.")
                        },
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        if (state.filterSheetOpen) {
            FilterSheet(
                filter = state.filter,
                radiusMeters = state.radiusMeters,
                onType = viewModel::onTypeChange,
                onAttribute = viewModel::toggleAttribute,
                onAuthor = viewModel::onAuthorChange,
                onOnlyMine = viewModel::onOnlyMineChange,
                onAddedWithin = viewModel::onAddedWithinChange,
                onMinRating = viewModel::onMinRatingChange,
                onRadius = viewModel::onRadiusChange,
                onReset = viewModel::resetFilters,
                onDismiss = { viewModel.closeFilters() },
            )
        }

        val selected = state.selectedCafe
        if (selected != null) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { viewModel.select(null) },
                sheetState = sheetState,
            ) {
                CafePreview(
                    cafe = selected,
                    onOpen = {
                        viewModel.select(null)
                        onCafeClick(selected.id)
                    },
                )
            }
        }
    }

    LaunchedEffect(profile.id) { viewModel.load() }
}

@Composable
private fun CafeList(
    cafes: List<Cafe>,
    photos: Map<String, ImageBitmap>,
    onPhotoVisible: (String?) -> Unit,
    onCafeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(cafes, key = { it.id }) { cafe ->
            CafeRow(
                cafe = cafe,
                photo = rememberVisiblePhoto(cafe.photoId, photos, onPhotoVisible),
                onClick = { onCafeClick(cafe.id) },
            )
        }
    }
}

@Composable
fun CafeRow(cafe: Cafe, photo: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PhotoThumb(photo, Modifier.size(56.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(cafe.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOfNotNull(cafe.address, formatDistance(cafe.distanceMeters)).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(cafe.type.label)
                    Pill("★ ${formatRating(cafe.averageRating)}")
                    Pill("${cafe.checkInCount} poseta")
                    if (cafe.activeChallenges.isNotEmpty()) {
                        Pill(
                            "x${cafe.activeChallenges.maxOf { it.multiplier }}",
                            background = MaterialTheme.colorScheme.errorContainer,
                            foreground = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CafePreview(cafe: Cafe, onOpen: () -> Unit) {
    Column(Modifier
        .fillMaxWidth()
        .padding(20.dp)) {
        Text(cafe.name, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            listOfNotNull(cafe.address, cafe.city, formatDistance(cafe.distanceMeters)).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("★ ${formatRating(cafe.averageRating)} (${cafe.reviewCount})")
            Pill("${cafe.checkInCount} check-inova")
            if (cafe.myCheckInCount > 0) Pill("osvojen ${cafe.myCheckInCount}x")
        }
        cafe.topVisitor?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                "👑 Osvajac: ${it.displayName} (${it.visits} poseta)",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
            Text("Otvori kafic")
        }
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * Every way the POI list can be narrowed, in one sheet: tip, atributi, autor,
 * datum dodavanja, ocena and the search radius.
 *
 * Choices apply when the sheet is dismissed rather than on every tap, so
 * adjusting four things costs one query instead of four.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    filter: CafeFilter,
    radiusMeters: Double,
    onType: (CafeType?) -> Unit,
    onAttribute: (String) -> Unit,
    onAuthor: (String) -> Unit,
    onOnlyMine: (Boolean) -> Unit,
    onAddedWithin: (Int?) -> Unit,
    onMinRating: (Double?) -> Unit,
    onRadius: (Double) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Filteri", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onReset, enabled = filter.isActive) { Text("Ponisti") }
            }

            // ------------------------------------------------------------ radius
            FilterLabel("Radijus pretrage: ${"%.1f".format(radiusMeters / 1000)} km")
            Slider(
                value = radiusMeters.toFloat(),
                onValueChange = { onRadius(it.toDouble()) },
                valueRange = MapViewModel.MIN_RADIUS_M.toFloat()..MapViewModel.MAX_RADIUS_M.toFloat(),
                steps = 48,
            )

            HorizontalDivider()

            // --------------------------------------------------------------- tip
            FilterLabel("Tip")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter.type == null,
                    onClick = { onType(null) },
                    label = { Text("Svi") },
                )
                CafeType.entries.forEach { type ->
                    FilterChip(
                        selected = filter.type == type,
                        onClick = { onType(if (filter.type == type) null else type) },
                        label = { Text(type.label) },
                    )
                }
            }

            // ---------------------------------------------------------- atributi
            FilterLabel("Atributi")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CafeAttributes.all.forEach { tag ->
                    FilterChip(
                        selected = tag in filter.attributes,
                        onClick = { onAttribute(tag) },
                        label = { Text("#$tag") },
                    )
                }
            }

            // ------------------------------------------------------------- ocena
            FilterLabel("Najmanja ocena")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter.minRating == null,
                    onClick = { onMinRating(null) },
                    label = { Text("Bilo koja") },
                )
                listOf(3.0, 4.0, 4.5).forEach { rating ->
                    FilterChip(
                        selected = filter.minRating == rating,
                        onClick = { onMinRating(if (filter.minRating == rating) null else rating) },
                        label = { Text("${rating}+") },
                    )
                }
            }

            HorizontalDivider()

            // -------------------------------------------------------------- autor
            FilterLabel("Autor")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Samo moji predlozi", Modifier.weight(1f))
                Switch(checked = filter.onlyMine, onCheckedChange = onOnlyMine)
            }
            OutlinedTextField(
                value = filter.authorUsername.orEmpty(),
                onValueChange = onAuthor,
                enabled = !filter.onlyMine,
                singleLine = true,
                label = { Text("Korisnicko ime autora") },
                modifier = Modifier.fillMaxWidth(),
            )

            // ------------------------------------------------------------ datumi
            FilterLabel("Dodato")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CafeFilter.DATE_PRESETS.forEach { (label, days) ->
                    FilterChip(
                        selected = filter.addedWithinDays == days,
                        onClick = { onAddedWithin(days) },
                        label = { Text(label) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Prikazi rezultate")
            }
        }
    }
}

@Composable
private fun FilterLabel(text: String) {
    Spacer(Modifier.height(6.dp))
    Text(text, style = MaterialTheme.typography.titleSmall)
}
