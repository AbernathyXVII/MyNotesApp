package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.PageImageStore

/**
 * **"Image"** (dal 29/09/2026, come la colonna delle figure del Pokédex di
 * Notion): una cella quadrata con dentro un'immagine, scelta come l'icona di
 * una pagina ("Upload" dalla galleria, "Remove") ma **intera**: si vede
 * tutta, piccola, senza essere ritagliata. Il valore salvato è il nome del
 * file, nella stessa cartella delle icone e delle copertine.
 */
@Composable
internal fun ImageCell(
    value: String,
    onValueChange: (String) -> Unit,
    /** Il nome della proprietà: il titolo della finestra per sceglierla. */
    title: String,
    /** Nella tabella la cella è un quadrato alto quanto la riga; nella scheda della riga un riquadro da 44. */
    inTable: Boolean
) {
    val context = LocalContext.current
    val store = remember(context) { PageImageStore(context) }
    val locked = LocalDatabaseLocked.current
    var picking by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = IMAGE_CELL_SIZE)
            .clickable(enabled = !locked) { picking = true }
            .padding(IMAGE_CELL_PADDING),
        contentAlignment = if (inTable) LocalCellAlignment.current.box else Alignment.CenterStart
    ) {
        if (value.isNotEmpty()) {
            PageImage(
                fileName = value,
                store = store,
                contentDescription = title,
                contentScale = ContentScale.Fit,
                placeholder = false,
                modifier = Modifier.size(IMAGE_CELL_SIZE - IMAGE_CELL_PADDING * 2)
            )
        }
    }
    if (picking) {
        PageImageSheet(
            target = PageImageTarget.ICON,
            title = title,
            hasImage = value.isNotEmpty(),
            store = store,
            onPicked = { picked ->
                val previous = value.takeIf { it.isNotEmpty() && it != picked }
                onValueChange(picked.orEmpty())
                // Il file vecchio se ne va: solo questa cella lo usava
                // (copiando righe e database il file si copia).
                previous
            },
            onReposition = null,
            onDismiss = { picking = false }
        )
    }
}

/** Il quadrato della cella: alto quanto una riga della tabella, e l'immagine dentro coi suoi margini. */
internal val IMAGE_CELL_SIZE = 44.dp
private val IMAGE_CELL_PADDING = 4.dp
