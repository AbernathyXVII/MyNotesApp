package com.gabriele.notionlocal.data.patchnotes

import com.gabriele.notionlocal.platform.PlatformContext

/** Sul telefono: gli asset dell'app (vedi `app/build.gradle.kts`). */
actual fun readPatchNotesFile(context: PlatformContext, name: String): String? =
    context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
