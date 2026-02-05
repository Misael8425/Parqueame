package com.example.parqueame.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PreferencesManager(private val context: Context) {

    companion object {
        private val Context.dataStore by preferencesDataStore(name = "usuario_prefs")

        private val CORREO_KEY = stringPreferencesKey("correo_usuario")
        // Nuevo: rol del usuario (CONDUCTOR, EMPRESA, etc.)
        private val ROL_KEY    = stringPreferencesKey("rol_usuario")
        // Legacy: por si antes guardaste el tipo con otra clave
        private val TIPO_KEY   = stringPreferencesKey("tipo_usuario")
    }

    // Guardar correo
    suspend fun saveCorreo(correo: String) {
        context.dataStore.edit { prefs ->
            prefs[CORREO_KEY] = correo
        }
    }

    // Leer correo
    suspend fun getCorreo(): String {
        return context.dataStore.data
            .map { prefs -> prefs[CORREO_KEY] ?: "" }
            .first()
    }

    // Eliminar solo el correo
    suspend fun clearCorreo() {
        context.dataStore.edit { prefs ->
            prefs.remove(CORREO_KEY)
        }
    }

    // ✅ Cerrar sesión: borrar claves conocidas (sin usar clear())
    suspend fun cerrarSesion() {
        context.dataStore.edit { prefs ->
            prefs.remove(CORREO_KEY)
            prefs.remove(ROL_KEY)
            prefs.remove(TIPO_KEY)
        }
    }

    // ---------------------------
    // ✅ Mejoras añadidas
    // ---------------------------

    /** Guardar rol (CONDUCTOR / EMPRESA / ADMINISTRADOR, etc.) */
    suspend fun saveRol(rol: String) {
        context.dataStore.edit { prefs ->
            prefs[ROL_KEY] = rol
            // Limpia clave legacy para evitar ambigüedades en el futuro
            prefs.remove(TIPO_KEY)
        }
    }

    /** Leer rol con fallback a la clave legacy "tipo_usuario" */
    suspend fun getRol(): String {
        return context.dataStore.data
            .map { prefs -> prefs[ROL_KEY] ?: prefs[TIPO_KEY] ?: "" }
            .first()
    }

    /** Flow del correo para observar cambios reactivos (opcional) */
    fun correoFlow(): Flow<String> =
        context.dataStore.data.map { prefs -> prefs[CORREO_KEY] ?: "" }

    /** Flow del rol con fallback a "tipo_usuario" (opcional) */
    fun rolFlow(): Flow<String> =
        context.dataStore.data.map { prefs -> prefs[ROL_KEY] ?: prefs[TIPO_KEY] ?: "" }

    /** Indica si hay sesión activa (correo y rol no vacíos) */
    suspend fun isLoggedIn(): Boolean =
        getCorreo().isNotBlank() && getRol().isNotBlank()
}
