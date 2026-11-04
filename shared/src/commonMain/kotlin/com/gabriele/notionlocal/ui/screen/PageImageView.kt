package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.toImageBitmap
import com.gabriele.notionlocal.ui.theme.DarkSurface

/**
 * Un'immagine di pagina già rimpicciolita a quanto serve davvero.
 *
 * La misura da raggiungere la dà il riquadro stesso: caricare una foto
 * da dodici megapixel per mostrarla larga un centimetro sono
 * quarantotto megabyte di memoria per niente, e l'app si chiude.
 */
@Composable
fun PageImage(
    fileName: String,
    store: PageImageStore,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    /** Crop per icone e copertine; Fit per la proprietà "Image", che si vede intera. */
    contentScale: ContentScale = ContentScale.Crop,
    /** Il riquadro scuro finché l'immagine si carica. */
    placeholder: Boolean = true
) {
    var widthPx by remember(fileName) { mutableStateOf(0) }
    var bitmap by remember(fileName) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(fileName, widthPx) {
        if (widthPx <= 0) return@LaunchedEffect
        bitmap = store.load(fileName, widthPx)?.toImageBitmap()
    }

    Box(modifier = modifier.onSizeChanged { widthPx = it.width }) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.matchParentSize()
            )
        } else if (placeholder) {
            // Finché l'immagine si carica, il posto resta suo: senza,
            // il titolo salterebbe su e giù ad ogni apertura.
            Box(modifier = Modifier.matchParentSize().background(DarkSurface))
        }
    }
}

/**
 * Cosa viene disegnato davanti a una riga del testo unito: gli spazi del
 * rientro e poi il marcatore (• per gli elenchi puntati, il numero per
 * quelli numerati, niente per tutto il resto). I due pezzi restano
 * distinti perché la zona toccabile deve coprire il solo marcatore, non
 * anche il rientro.
 */
data class LinePrefix(
    val indent: String,
    val marker: String,
    /**
     * Il marcatore c'è ma non si vede: la riga tiene il posto di un elenco
     * (vedi `BlockAttrs.listIndent`), e scritto trasparente occupa proprio
     * lo spazio del numero o del pallino, così il testo resta allineato.
     */
    val hidden: Boolean = false,
    /**
     * L'elenco di cui è il marcatore (anche quello invisibile del posto di
     * un elenco), per lo spazio dopo di lui (vedi `ParagraphLook.gapOf`);
     * null per le righe senza marcatore.
     */
    val listType: BlockType? = null
) {
    val text: String get() = indent + marker
}
