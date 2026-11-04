package com.gabriele.notionlocal.platform

import java.io.InputStream

/**
 * **Un file di testo che viaggia con l'app** ("emoji/emoji.txt", per
 * esempio; dal 04/11/2026): sul telefono gli asset dell'APK, sul PC gli stessi
 * file, messi fra le risorse del programma (`desktop/build.gradle.kts`).
 * Null se non c'è o non si legge.
 */
expect fun readAppAsset(context: PlatformContext, path: String): String?

/** Lo stesso file, da leggere a pezzi (un dizionario del correttore); null se non c'è. */
expect fun openAppAsset(context: PlatformContext, path: String): InputStream?

/**
 * **Se il sistema sa disegnare un'emoji in un carattere solo** (una bandiera
 * nuova su un telefono vecchio non si sa disegnare, e diventerebbe due
 * lettere): sul telefono lo chiede ad Android, sul PC per ora sì.
 */
expect fun canDrawEmoji(emoji: String): Boolean
