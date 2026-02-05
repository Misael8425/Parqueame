// RecentSearchStore.kt
package com.example.parqueame.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("recents_store")

data class LastPlace(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val distanceText: String
)

class RecentSearchStore(private val context: Context) {

    private val KEY_NAME = stringPreferencesKey("last_place_name")
    private val KEY_ADDRESS = stringPreferencesKey("last_place_address")
    private val KEY_LAT = doublePreferencesKey("last_place_lat")
    private val KEY_LNG = doublePreferencesKey("last_place_lng")
    private val KEY_DISTANCE = stringPreferencesKey("last_place_distance")

    suspend fun saveLastPlace(place: LastPlace) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NAME] = place.name
            prefs[KEY_ADDRESS] = place.address
            prefs[KEY_LAT] = place.lat
            prefs[KEY_LNG] = place.lng
            prefs[KEY_DISTANCE] = place.distanceText
        }
    }

    fun getLastPlace(): Flow<LastPlace?> {
        return context.dataStore.data.map { prefs ->
            val name = prefs[KEY_NAME] ?: return@map null
            val address = prefs[KEY_ADDRESS] ?: ""
            val lat = prefs[KEY_LAT] ?: return@map null
            val lng = prefs[KEY_LNG] ?: return@map null
            val dist = prefs[KEY_DISTANCE] ?: ""
            LastPlace(name, address, lat, lng, dist)
        }
    }
}