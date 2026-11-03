package com.gabriele.notionlocal.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.gabriele.notionlocal.platform.PlatformContext

/** Sul telefono: le `SharedPreferences` di sempre, con lo stesso nome. */
actual fun openSettingsStore(context: PlatformContext, name: String): SettingsStore =
    SharedPreferencesStore(context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE))

private class SharedPreferencesStore(private val prefs: SharedPreferences) : SettingsStore {
    override fun getString(key: String, defValue: String?): String? = prefs.getString(key, defValue)
    override fun getBoolean(key: String, defValue: Boolean): Boolean = prefs.getBoolean(key, defValue)
    override fun getInt(key: String, defValue: Int): Int = prefs.getInt(key, defValue)
    override fun getLong(key: String, defValue: Long): Long = prefs.getLong(key, defValue)
    override fun edit(): SettingsStore.Editor = Editor(prefs.edit())

    private class Editor(private val editor: SharedPreferences.Editor) : SettingsStore.Editor {
        override fun putString(key: String, value: String?) = apply { editor.putString(key, value) }
        override fun putBoolean(key: String, value: Boolean) = apply { editor.putBoolean(key, value) }
        override fun putInt(key: String, value: Int) = apply { editor.putInt(key, value) }
        override fun putLong(key: String, value: Long) = apply { editor.putLong(key, value) }
        override fun apply() = editor.apply()
    }
}
