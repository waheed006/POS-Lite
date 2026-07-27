package com.gembyte.poslite.components.pref

import android.content.Context
import androidx.core.content.edit

class PrefManager private constructor(context: Context) {

    private val prefs =
        context.getSharedPreferences(
            "pos_prefs",
            Context.MODE_PRIVATE
        )

    companion object {

        @Volatile
        private var INSTANCE: PrefManager? = null

        fun getInstance(context: Context): PrefManager {

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: PrefManager(
                    context.applicationContext
                ).also {

                    INSTANCE = it
                }
            }
        }
    }

    fun savePin(pin: String) {

        prefs.edit {
            putString("pin_code", pin)
                .putBoolean("is_pin_set", true)
        }
    }

    fun getPin(): String {

        return prefs.getString(
            "pin_code",
            ""
        ) ?: ""
    }

    fun isPinSet(): Boolean {

        return prefs.getBoolean(
            "is_pin_set",
            false
        )
    }

    fun clearPin() {
        prefs.edit { clear() }
    }
}