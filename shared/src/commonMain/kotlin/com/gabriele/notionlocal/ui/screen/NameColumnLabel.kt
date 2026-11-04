package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.gabriele.notionlocal.ui.i18n.DbStrings

/**
 * **Il nome della proprietà principale** del database aperto (dal
 * 18/10/2026): "Name", o quello che l'utente le ha dato (vedi
 * `PageEntity.nameColumnTitle`). Passa da qui per la stessa ragione di
 * `LocalDatabaseLocked`: lo usano l'intestazione, la finestra della riga,
 * l'ordinamento, i calcoli e il grafico, in fondo a catene lunghe.
 */
val LocalNameColumnTitle = compositionLocalOf<String?> { null }

/** "Name", o il nome dato alla proprietà principale del database aperto. */
@Composable
fun nameColumnLabel(): String = DbStrings.nameColumn(LocalNameColumnTitle.current)
