package com.gabriele.notionlocal.data.ink

/** Come cancella la gomma, come su OneNote. */
enum class EraserMode {
    /** Tutto il tratto toccato. */
    STROKE,

    /** Solo quello che sta sotto la gomma: il tratto si spezza. */
    POINT
}
