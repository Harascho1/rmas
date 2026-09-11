package rs.coffeeconquest.app.ui.cafe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.coffeeconquest.app.data.AddressLookup
import rs.coffeeconquest.app.data.AppContainer
import rs.coffeeconquest.app.data.CoffeeRepository
import rs.coffeeconquest.app.data.LatLon
import rs.coffeeconquest.app.data.LocationProvider
import rs.coffeeconquest.app.data.firebase.userMessage
import rs.coffeeconquest.shared.dto.CreateCafeRequest
import rs.coffeeconquest.shared.model.CafeType
import rs.coffeeconquest.shared.rules.Validation

data class AddCafeUiState(
    val name: String = "",
    val address: String = "",
    val city: String = "",
    val description: String = "",
    val openingHours: String = "",
    val type: CafeType = CafeType.KAFIC,
    val attributes: List<String> = emptyList(),
    val tags: String = "",
    val latitude: Double = LocationProvider.DEFAULT.latitude,
    val longitude: Double = LocationProvider.DEFAULT.longitude,
    val mapCenter: LatLon = LocationProvider.DEFAULT,
    val resolvingAddress: Boolean = false,
    val addressTouched: Boolean = false,
    val cityTouched: Boolean = false,
    val submitting: Boolean = false,
    val created: Boolean = false,
    val error: String? = null,
) {
    val pin: LatLon get() = LatLon(latitude, longitude)

    val canSubmit: Boolean get() = !submitting && Validation.cafeName(name) == null
}

class AddCafeViewModel(
    private val repository: CoffeeRepository = AppContainer.repository,
    private val location: LocationProvider = AppContainer.location,
    private val addresses: AddressLookup = AppContainer.addresses,
) : ViewModel() {

    private val _state = MutableStateFlow(AddCafeUiState())
    val state: StateFlow<AddCafeUiState> = _state.asStateFlow()

    fun onNameChange(value: String) = _state.update { it.copy(name = value, error = null) }
    fun onAddressChange(value: String) =
        _state.update { it.copy(address = value, addressTouched = true) }

    fun onCityChange(value: String) = _state.update { it.copy(city = value, cityTouched = true) }
    fun onDescriptionChange(value: String) = _state.update { it.copy(description = value) }
    fun onOpeningHoursChange(value: String) = _state.update { it.copy(openingHours = value) }
    fun onTagsChange(value: String) = _state.update { it.copy(tags = value) }
    fun onTypeChange(type: CafeType) = _state.update { it.copy(type = type) }

    fun toggleAttribute(tag: String) = _state.update { current ->
        val attributes =
            if (tag in current.attributes) current.attributes - tag else current.attributes + tag
        current.copy(attributes = attributes)
    }

    fun useCurrentLocation() {
        viewModelScope.launch {
            val fix = location.current()
            if (fix == null) {
                _state.update { it.copy(error = "Lokacija nije dostupna - proverite dozvolu i GPS.") }
            } else {
                _state.update {
                    it.copy(
                        latitude = fix.latitude,
                        longitude = fix.longitude,
                        mapCenter = fix.position,
                        error = null,
                    )
                }
                resolveAddress(fix.position)
            }
        }
    }

    fun onMapPick(point: LatLon) {
        _state.update {
            it.copy(
                latitude = point.latitude,
                longitude = point.longitude,
                error = null
            )
        }
        resolveAddress(point)
    }

    private var addressJob: Job? = null

    private fun resolveAddress(point: LatLon) {
        addressJob?.cancel()
        addressJob = viewModelScope.launch {
            _state.update { it.copy(resolvingAddress = true) }
            val resolved = runCatching { addresses.reverse(point) }.getOrNull()
            _state.update { current ->
                current.copy(
                    resolvingAddress = false,
                    address = if (current.addressTouched) {
                        current.address
                    } else {
                        resolved?.street ?: current.address
                    },
                    city = if (current.cityTouched) current.city else resolved?.city
                        ?: current.city,
                )
            }
        }
    }

    fun submit() {
        val current = _state.value
        val invalid = Validation.cafeName(current.name)
        if (invalid != null) {
            _state.update { it.copy(error = invalid) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            runCatching {
                repository.createCafe(
                    CreateCafeRequest(
                        name = current.name.trim(),
                        description = current.description.trim().takeIf { it.isNotEmpty() },
                        address = current.address.trim().takeIf { it.isNotEmpty() },
                        city = current.city.trim().takeIf { it.isNotEmpty() },
                        latitude = current.latitude,
                        longitude = current.longitude,
                        type = current.type,
                        openingHours = current.openingHours.trim().takeIf { it.isNotEmpty() },
                        // Chip choices and anything typed by hand end up in one list.
                        tags = (
                            current.attributes +
                                current.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            ).distinct(),
                    ),
                )
            }.fold(
                onSuccess = { _state.update { it.copy(submitting = false, created = true) } },
                onFailure = { error -> _state.update { it.copy(submitting = false, error = error.userMessage()) } },
            )
        }
    }
}
