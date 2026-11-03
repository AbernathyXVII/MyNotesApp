package com.gabriele.notionlocal.data.settings

import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.PlatformContext
import java.io.File
import java.util.Properties

/** Sul PC: un file `<name>.properties` nella cartella dei dati di PageFlow. */
actual fun openSettingsStore(context: PlatformContext, name: String): SettingsStore =
    PropertiesStore(File((context as DesktopContext).dataDir, "$name.properties"))

private class PropertiesStore(private val file: File) : SettingsStore {
    private val values = Properties().apply {
        if (file.isFile) file.inputStream().use { load(it) }
    }

    override fun getString(key: String, defValue: String?): String? = synchronized(values) { values.getProperty(key) } ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = getString(key, null)?.toBooleanStrictOrNull() ?: defValue
    override fun getInt(key: String, defValue: Int): Int = getString(key, null)?.toIntOrNull() ?: defValue
    override fun getLong(key: String, defValue: Long): Long = getString(key, null)?.toLongOrNull() ?: defValue

    override fun edit(): SettingsStore.Editor = object : SettingsStore.Editor {
        private val changes = mutableMapOf<String, String?>()
        override fun putString(key: String, value: String?) = apply { changes[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { changes[key] = value.toString() }
        override fun putInt(key: String, value: Int) = apply { changes[key] = value.toString() }
        override fun putLong(key: String, value: Long) = apply { changes[key] = value.toString() }
        override fun apply() {
            synchronized(values) {
                for ((key, value) in changes) if (value == null) values.remove(key) else values.setProperty(key, value)
                file.parentFile?.mkdirs()
                file.outputStream().use { values.store(it, "PageFlow") }
            }
        }
    }
}
