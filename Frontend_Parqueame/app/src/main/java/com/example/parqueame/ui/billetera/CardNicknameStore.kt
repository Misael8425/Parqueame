// CardNicknameStore.kt
package com.example.parqueame.ui.billetera

import android.content.Context
import androidx.core.content.edit
import java.util.Locale

object CardNicknameStore {
    private const val PREFS_NAME = "wallet_nicknames"

    private fun pmKey(pmId: String) = "card_nickname_$pmId"
    private fun fallbackKey(brand: String, last4: String) =
        "card_nickname_fbk_${brand.lowercase(Locale.ROOT)}_${last4.trim()}"

    fun get(context: Context, pmId: String?): String? {
        if (pmId.isNullOrBlank()) return null
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getString(pmKey(pmId), null)
    }

    fun getByBrandLast4(context: Context, brand: String?, last4: String?): String? {
        if (brand.isNullOrBlank() || last4.isNullOrBlank()) return null
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getString(fallbackKey(brand, last4), null)
    }

    fun set(context: Context, pmId: String?, nickname: String?) {
        if (pmId.isNullOrBlank()) return
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit {
            if (nickname.isNullOrBlank()) remove(pmKey(pmId))
            else putString(pmKey(pmId), nickname)
        }
    }

    fun setByBrandLast4(context: Context, brand: String?, last4: String?, nickname: String?) {
        if (brand.isNullOrBlank() || last4.isNullOrBlank()) return
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val k = fallbackKey(brand, last4)
        sp.edit {
            if (nickname.isNullOrBlank()) remove(k)
            else putString(k, nickname)
        }
    }
}