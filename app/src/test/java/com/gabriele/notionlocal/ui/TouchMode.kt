package com.gabriele.notionlocal.ui

import android.app.Activity
import android.view.View

/**
 * **Lo schermo come lo usa un dito** (dal 03/11/2026, Compose 1.11).
 *
 * Sul telefono, appena lo si tocca, Android è "in modalità tocco": tolto il
 * cursore da un campo, non lo dà a nessun altro. Senza, come con una tastiera
 * vera attaccata, lo ridà da solo al primo campo dello schermo. Robolectric
 * non ci entra mai (i tocchi dei test arrivano dritti a Compose), e da
 * Compose 1.11 si vede: tolto il cursore, tornava subito nella prima casella
 * della tabella, o nella riga della nota veloce. Qui ci si entra come quando
 * arriva il primo tocco.
 */
fun Activity.enterTouchMode() {
    val root = View::class.java.getMethod("getViewRootImpl").invoke(window.decorView) ?: return
    root.javaClass.getDeclaredMethod("ensureTouchModeLocally", Boolean::class.javaPrimitiveType).apply {
        isAccessible = true
        invoke(root, true)
    }
}
