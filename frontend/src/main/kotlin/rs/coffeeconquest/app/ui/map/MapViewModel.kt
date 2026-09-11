package rs.coffeeconquest.app.ui.map

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.coffeeconquest.app.data.AppContainer
import rs.coffeeconquest.app.data.LatLon
import rs.coffeeconquest.app.data.LocationFix
import rs.coffeeconquest.app.data.LocationProvider
import rs.coffeeconquest.app.data.firebase.userMessage
import rs.coffeeconquest.app.ui.PhotoStore
import rs.coffeeconquest.app.ui.UiState
import rs.coffeeconquest.shared.dto.Cafe
import rs.coffeeconquest.shared.dto.CafeFilter
import rs.coffeeconquest.shared.model.CafeType

data class MapUiState(
    val cafes: UiState<List<Cafe>> = UiState.Loading,
    val center: LatLon = LocationProvider.DEFAULT,
    val hasFix: Boolean = false,
    val fixSource: LocationFix.Source? = null,
    val fixAccuracyMeters: Float? = null,
    val filter: CafeFilter = CafeFilter(),
    val radiusMeters: Double = 5_000.0,
    val filterSheetOpen: Boolean = false,
    val selectedCafe: Cafe? = null,
    val listMode: Boolean = false,
) {
    val radiusKm: Double get() = radiusMeters / 1000.0
}

class MapViewModel : ViewModel() {

    private val repository = AppContainer.repository
    private val location = AppContainer.location

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    private val photoStore = PhotoStore(repository, viewModelScope)
    val photos: StateFlow<Map<String, ImageBitmap>> = photoStore.photos

    /** The UI reports which photo scrolled into view; fetching it is this ViewModel's job. */
    fun requestPhoto(photoId: String?) = photoStore.load(photoId)

    init {
        locate()
    }

    /** Centres on the user when a fix is available, then loads what is around them. */
    fun locate() {
        viewModelScope.launch {
            val fix = location.current()
            if (fix != null) {
                _state.update {
                    it.copy(
                        center = fix.position,
                        hasFix = true,
                        fixSource = fix.source,
                        fixAccuracyMeters = fix.accuracyMeters,
                    )
                }
            }
            load()
        }
    }

    private var cafesJob: Job? = null

    fun load() {
        cafesJob?.cancel()
        _state.update { it.copy(cafes = UiState.Loading) }
        val current = _state.value
        cafesJob = viewModelScope.launch {
            repository.cafesStream(
                latitude = current.center.latitude,
                longitude = current.center.longitude,
                radiusMeters = current.radiusMeters,
                filter = current.filter,
            )
                .catch { error ->
                    _state.update { it.copy(cafes = UiState.Error(error.userMessage())) }
                }
                .collect { cafes -> _state.update { it.copy(cafes = UiState.Ready(cafes)) } }
        }
    }

    // ---------------------------------------------------------------- search

    fun onQueryChange(value: String) = updateFilter { it.copy(query = value) }

    // --------------------------------------------------------------- filters

    fun openFilters() = _state.update { it.copy(filterSheetOpen = true) }

    /** Closing the sheet is what applies the filter, so one edit is one query. */
    fun closeFilters(apply: Boolean = true) {
        _state.update { it.copy(filterSheetOpen = false) }
        if (apply) load()
    }

    fun onTypeChange(type: CafeType?) = updateFilter { it.copy(type = type) }

    fun toggleAttribute(tag: String) = updateFilter { filter ->
        val attributes = if (tag in filter.attributes) filter.attributes - tag else filter.attributes + tag
        filter.copy(attributes = attributes)
    }

    fun onAuthorChange(username: String) = updateFilter {
        it.copy(authorUsername = username.takeIf { name -> name.isNotBlank() }, onlyMine = false)
    }

    fun onOnlyMineChange(value: Boolean) = updateFilter {
        it.copy(onlyMine = value, authorUsername = null)
    }

    fun onAddedWithinChange(days: Int?) = updateFilter { it.copy(addedWithinDays = days) }

    fun onMinRatingChange(rating: Double?) = updateFilter { it.copy(minRating = rating) }

    /** Clears every narrowing choice but keeps the text the user typed. */
    fun resetFilters() = _state.update { it.copy(filter = CafeFilter(query = it.filter.query)) }

    private fun updateFilter(transform: (CafeFilter) -> CafeFilter) =
        _state.update { it.copy(filter = transform(it.filter)) }

    // ---------------------------------------------------------------- radius

    fun onRadiusChange(meters: Double) = _state.update { it.copy(radiusMeters = meters) }

    fun select(cafe: Cafe?) = _state.update { it.copy(selectedCafe = cafe) }

    fun toggleListMode() = _state.update { it.copy(listMode = !it.listMode) }

    companion object {
        /** The radius slider runs between these, in metres. */
        const val MIN_RADIUS_M = 500.0
        const val MAX_RADIUS_M = 25_000.0
    }
}
