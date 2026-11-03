package com.gabriele.notionlocal.platform

import java.io.File

actual abstract class PlatformContext

/**
 * **Sul PC, la cartella dei dati di PageFlow**: su Windows
 * `%APPDATA%\PageFlow`, sul Mac `~/Library/Application Support/PageFlow`.
 */
class DesktopContext(val dataDir: File = defaultDataDir()) : PlatformContext() {
    init {
        dataDir.mkdirs()
    }

    companion object {
        fun defaultDataDir(): File {
            val os = System.getProperty("os.name").orEmpty().lowercase()
            val home = System.getProperty("user.home")
            return when {
                os.contains("win") -> File(System.getenv("APPDATA") ?: "$home\\AppData\\Roaming", "PageFlow")
                os.contains("mac") -> File(home, "Library/Application Support/PageFlow")
                else -> File(home, ".pageflow")
            }
        }
    }
}
