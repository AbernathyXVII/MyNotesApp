package com.gabriele.notionlocal.data.webclip


/**
 * "What do you want to capture?", come in OneNote Web Clipper (dal 20/10/2026).
 * `TEXT_AND_IMAGES` (chiesto dopo) non è una foto: il testo e le immagini
 * dell'articolo diventano righe di notE.
 */
enum class WebClipMode { FULL_PAGE, REGION, BOOKMARK, ARTICLE, TEXT_AND_IMAGES }
