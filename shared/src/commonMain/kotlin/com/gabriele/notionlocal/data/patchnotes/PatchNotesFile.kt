package com.gabriele.notionlocal.data.patchnotes

import com.gabriele.notionlocal.platform.PlatformContext

/**
 * Il testo del file delle Patch Notes [name] messo dentro l'app dal build:
 * fra gli asset sul telefono, fra le risorse sul PC. Null se non c'è.
 */
expect fun readPatchNotesFile(context: PlatformContext, name: String): String?
