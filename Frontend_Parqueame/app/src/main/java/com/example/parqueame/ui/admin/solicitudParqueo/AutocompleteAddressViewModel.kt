package com.example.parqueame.ui.admin.solicitudParqueo

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
//import com.google.android.libraries.places.api.model.FetchPlaceRequest
//import com.google.android.libraries.places.api.model.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class AddressSuggestion(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String?
)

class AutocompleteAddressViewModel(app: Application): AndroidViewModel(app) {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _suggestions = MutableStateFlow<List<AddressSuggestion>>(emptyList())
    val suggestions: StateFlow<List<AddressSuggestion>> = _suggestions

    private val _selectedLat = MutableStateFlow<Double?>(null)
    private val _selectedLng = MutableStateFlow<Double?>(null)
    private val _selectedAddress = MutableStateFlow<String?>(null)

    val selectedLat: StateFlow<Double?> = _selectedLat
    val selectedLng: StateFlow<Double?> = _selectedLng
    val selectedAddress: StateFlow<String?> = _selectedAddress

    private var debounceJob: Job? = null
    private var placesClient: PlacesClient? = null
    private var sessionToken: AutocompleteSessionToken? = null

    private fun ensurePlaces(context: Context) {
        if (!Places.isInitialized()) {
            val key = try {
                // Lee la misma API key que pusiste en el manifest (opcional: puedes pasar null)
                val ai = context.packageManager.getApplicationInfo(context.packageName, 128)
                val bundle = ai.metaData
                bundle.getString("com.google.android.geo.API_KEY")
            } catch (_: Exception) { null }

            Places.initialize(context, key ?: "", Locale.getDefault())
        }
        if (placesClient == null) placesClient = Places.createClient(context)
        if (sessionToken == null) sessionToken = AutocompleteSessionToken.newInstance()
    }

    fun updateQuery(context: Context, value: String) {
        _query.value = value
        _selectedAddress.value = null
        _selectedLat.value = null
        _selectedLng.value = null

        if (value.length < 3) {
            _suggestions.value = emptyList()
            return
        }

        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(300) // debounce
            fetchPredictions(context, value)
        }
    }

    private fun fetchPredictions(context: Context, value: String) {
        ensurePlaces(context)
        val client = placesClient ?: return

        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(sessionToken)
            .setQuery(value)
            // Opcional: sesgar por país (ej: República Dominicana = "DO")
            .setCountries(listOf("DO"))
            .build()

        client.findAutocompletePredictions(request)
            .addOnSuccessListener { resp ->
                _suggestions.value = resp.autocompletePredictions.map {
                    AddressSuggestion(
                        placeId = it.placeId,
                        primaryText = it.getPrimaryText(null).toString(),
                        secondaryText = it.getSecondaryText(null)?.toString()
                    )
                }
            }
            .addOnFailureListener {
                _suggestions.value = emptyList()
            }
    }

    fun selectSuggestion(context: Context, suggestion: AddressSuggestion) {
        ensurePlaces(context)
        val client = placesClient ?: return

        val request = FetchPlaceRequest.builder(
            suggestion.placeId,
            listOf(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        ).setSessionToken(sessionToken).build()

        client.fetchPlace(request)
            .addOnSuccessListener { resp ->
                val place = resp.place
                _selectedAddress.value = place.address
                _selectedLat.value = place.latLng?.latitude
                _selectedLng.value = place.latLng?.longitude

                // Si quieres cerrar sesión para facturación clara:
                sessionToken = AutocompleteSessionToken.newInstance()
            }
            .addOnFailureListener {
                // no cambies el estado si falla
            }
    }
}
