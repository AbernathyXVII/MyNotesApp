package com.gabriele.notionlocal.data.patchnotes

import com.gabriele.notionlocal.platform.PlatformContext

/** Sul PC: le risorse dell'app (vedi `desktop/build.gradle.kts`). */
actual fun readPatchNotesFile(context: PlatformContext, name: String): String? =
    PlatformContext::class.java.getResourceAsStream("/$name")?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
