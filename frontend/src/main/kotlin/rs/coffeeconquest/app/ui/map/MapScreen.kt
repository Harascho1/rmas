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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import rs.coffeeconquest.app.R
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
                    placeholder = { Text(stringResource(R.string.map_search_placeholder)) },
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
                        Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.map_content_desc_filters))
                    }
                }
            }
        }

        state.fixSource?.let { source ->
            val accuracy = state.fixAccuracyMeters?.let {
                stringResource(R.string.map_location_accuracy, it.toInt())
            }.orEmpty()
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 84.dp),
            ) {
                Text(
                    stringResource(R.string.map_location_label, source.label, accuracy),
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
                    contentDescription = stringResource(R.string.map_content_desc_toggle_view),
                )
            }
            SmallFloatingActionButton(onClick = viewModel::locate) {
                Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.map_content_desc_my_location))
            }
            FloatingActionButton(onClick = onAddCafe) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.map_content_desc_add_cafe))
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
                        if (state.filter.isActive) {
                            stringResource(R.string.map_empty_cafes_filtered, state.radiusKm.toInt())
                        } else {
                            stringResource(R.string.map_empty_cafes, state.radiusKm.toInt())
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
                    Pill(stringResource(R.string.map_rating_star, formatRating(cafe.averageRating)))
                    Pill(stringResource(R.string.map_visits_count, cafe.checkInCount))
                    if (cafe.activeChallenges.isNotEmpty()) {
                        Pill(
                            stringResource(R.string.map_multiplier_badge, cafe.activeChallenges.maxOf { it.multiplier }),
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
            Pill(stringResource(R.string.map_rating_with_reviews, formatRating(cafe.averageRating), cafe.reviewCount))
            Pill(stringResource(R.string.map_checkins_count, cafe.checkInCount))
            if (cafe.myCheckInCount > 0) Pill(stringResource(R.string.map_conquered_count, cafe.myCheckInCount))
        }
        cafe.topVisitor?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.map_top_visitor, it.displayName, it.visits),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.map_button_open_cafe))
        }
        Spacer(Modifier.height(12.dp))
    }
}

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
                Text(stringResource(R.string.map_filters_title), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onReset, enabled = filter.isActive) { Text(stringResource(R.string.map_button_reset)) }
            }

            FilterLabel(stringResource(R.string.map_filter_radius_label, "%.1f".format(radiusMeters / 1000)))
            Slider(
                value = radiusMeters.toFloat(),
                onValueChange = { onRadius(it.toDouble()) },
                valueRange = MapViewModel.MIN_RADIUS_M.toFloat()..MapViewModel.MAX_RADIUS_M.toFloat(),
                steps = 48,
            )

            HorizontalDivider()

            FilterLabel(stringResource(R.string.map_filter_type_label))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter.type == null,
                    onClick = { onType(null) },
                    label = { Text(stringResource(R.string.map_filter_all)) },
                )
                CafeType.entries.forEach { type ->
                    FilterChip(
                        selected = filter.type == type,
                        onClick = { onType(if (filter.type == type) null else type) },
                        label = { Text(type.label) },
                    )
                }
            }

            FilterLabel(stringResource(R.string.map_filter_attributes_label))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CafeAttributes.all.forEach { tag ->
                    FilterChip(
                        selected = tag in filter.attributes,
                        onClick = { onAttribute(tag) },
                        label = { Text(stringResource(R.string.map_filter_tag_hash, tag)) },
                    )
                }
            }

            FilterLabel(stringResource(R.string.map_filter_min_rating_label))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter.minRating == null,
                    onClick = { onMinRating(null) },
                    label = { Text(stringResource(R.string.map_filter_any_rating)) },
                )
                listOf(3.0, 4.0, 4.5).forEach { rating ->
                    FilterChip(
                        selected = filter.minRating == rating,
                        onClick = { onMinRating(if (filter.minRating == rating) null else rating) },
                        label = { Text(stringResource(R.string.map_filter_rating_plus, rating)) },
                    )
                }
            }

            HorizontalDivider()

            FilterLabel(stringResource(R.string.map_filter_author_label))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.map_filter_only_mine), Modifier.weight(1f))
                Switch(checked = filter.onlyMine, onCheckedChange = onOnlyMine)
            }
            OutlinedTextField(
                value = filter.authorUsername.orEmpty(),
                onValueChange = onAuthor,
                enabled = !filter.onlyMine,
                singleLine = true,
                label = { Text(stringResource(R.string.map_filter_author_username_label)) },
                modifier = Modifier.fillMaxWidth(),
            )

            FilterLabel(stringResource(R.string.map_filter_added_label))
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
                Text(stringResource(R.string.map_button_show_results))
            }
        }
    }
}

@Composable
private fun FilterLabel(text: String) {
    Spacer(Modifier.height(6.dp))
    Text(text, style = MaterialTheme.typography.titleSmall)
}
