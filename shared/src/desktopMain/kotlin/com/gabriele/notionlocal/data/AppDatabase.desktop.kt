package com.gabriele.notionlocal.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.PlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import java.io.File

/**
 * **Sul PC**: `notion_local.db` nella cartella dei dati di PageFlow (come sul
 * telefono, così un backup del telefono è lo stesso file), con l'SQLite
 * messo dentro l'app: su Windows non ce n'è uno di sistema da usare.
 */
actual fun buildAppDatabase(context: PlatformContext): AppDatabase =
    Room.databaseBuilder<AppDatabase>(name = File((context as DesktopContext).dataDir, "notion_local.db").absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
