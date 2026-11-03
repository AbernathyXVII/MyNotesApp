package com.gabriele.notionlocal.data.settings

import com.gabriele.notionlocal.platform.PlatformContext

/**
 * **Dove si salvano le impostazioni** (dal 04/11/2026, verso Windows e Mac).
 *
 * Le stesse chiamate delle `SharedPreferences` di Android, così `AppSettings`
 * non è cambiato: sul telefono sono proprio quelle (lo stesso file di
 * sempre), sul PC un file `.properties` nella cartella dei dati.
 */
interface SettingsStore {
    fun getString(key: String, defValue: String?): String?
    fun getBoolean(key: String, defValue: Boolean): Boolean
    fun getInt(key: String, defValue: Int): Int
    fun getLong(key: String, defValue: Long): Long
    fun edit(): Editor

    interface Editor {
        fun putString(key: String, value: String?): Editor
        fun putBoolean(key: String, value: Boolean): Editor
        fun putInt(key: String, value: Int): Editor
        fun putLong(key: String, value: Long): Editor
        fun apply()
    }
}

/** Le impostazioni chiamate [name] ("settings", per esempio). */
expect fun openSettingsStore(context: PlatformContext, name: String): SettingsStore
