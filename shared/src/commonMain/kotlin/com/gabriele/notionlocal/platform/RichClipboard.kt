package com.gabriele.notionlocal.platform

/**
 * **Testo con la sua formattazione negli appunti** (dal 04/11/2026): il testo
 * semplice e, accanto, lo stesso in HTML, così incollato altrove arriva
 * formattato. Vero se è andato.
 */
expect fun copyRichText(context: PlatformContext, plain: String, html: String): Boolean

/** Se il sistema dice da sé "copiato" (Android dal 13): allora l'app non lo ripete. */
expect val systemAnnouncesCopies: Boolean
