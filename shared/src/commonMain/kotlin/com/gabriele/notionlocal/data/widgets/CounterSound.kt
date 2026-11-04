package com.gabriele.notionlocal.data.widgets


/**
 * **I suoni del contatore** (dal 18/10/2026, chiesto dall'utente): un click
 * su +, uno più lieve su −, e qualche alternativa da scegliere. Non vengono
 * da file: l'app li disegna da sé, onda per onda (`samples`), e li suona come
 * i suoni dei tasti — col telefono in silenzioso non si sentono.
 */
enum class CounterSound {
    CLICK, SOFT_CLICK, TICK, POP, BUBBLE, WOOD, BEEP, COIN;

    companion object {
        fun of(name: String?): CounterSound = entries.find { it.name == name } ?: CLICK
    }
}
