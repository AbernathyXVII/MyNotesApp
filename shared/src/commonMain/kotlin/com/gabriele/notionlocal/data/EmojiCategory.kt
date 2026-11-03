package com.gabriele.notionlocal.data

/**
 * Le categorie delle emoji, nell'ordine e coi nomi del selettore di Notion
 * (foto dell'utente del 07/10/2026). `key` è il nome nel file
 * `assets/emoji/emoji.txt`.
 */
enum class EmojiCategory(val key: String) {
    PEOPLE("people"),
    NATURE("nature"),
    FOOD("food"),
    ACTIVITY("activity"),
    TRAVEL("travel"),
    OBJECTS("objects"),
    SYMBOLS("symbols"),
    FLAGS("flags")
}

/** Quanti colori della pelle ci sono (i modificatori di Fitzpatrick): vedi `withSkinTone`. */
const val SKIN_TONE_COUNT = 5
