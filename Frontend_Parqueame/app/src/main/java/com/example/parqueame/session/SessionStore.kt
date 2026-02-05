    // app/src/main/java/com/example/parqueame/session/SessionStore.kt
    package com.example.parqueame.session

    import android.content.Context
    import androidx.datastore.preferences.core.edit
    import androidx.datastore.preferences.core.stringPreferencesKey
    import androidx.datastore.preferences.preferencesDataStore
    import kotlinx.coroutines.flow.map
    import kotlinx.coroutines.flow.Flow

    val Context.sessionDataStore by preferencesDataStore("session")

    object SessionKeys {
        val USER_ID  = stringPreferencesKey("user_id")
        val USER_DOC = stringPreferencesKey("user_doc")
        val USER_TIPO = stringPreferencesKey("user_tipo") // "CEDULA" o "RNC"
    }

    class SessionStore(private val context: Context) {
        val userId: Flow<String?> = context.sessionDataStore.data.map { it[SessionKeys.USER_ID] }
        val userDoc: Flow<String?> = context.sessionDataStore.data.map { it[SessionKeys.USER_DOC] }
        val userTipo: Flow<String?> = context.sessionDataStore.data.map { it[SessionKeys.USER_TIPO] }

        suspend fun save(userId: String, documento: String, tipo: String) {
            context.sessionDataStore.edit {
                it[SessionKeys.USER_ID] = userId
                it[SessionKeys.USER_DOC] = documento
                it[SessionKeys.USER_TIPO] = tipo.uppercase().replace("É", "E")
            }
        }

        suspend fun clear() {
            context.sessionDataStore.edit { it.clear() }
        }
    }
