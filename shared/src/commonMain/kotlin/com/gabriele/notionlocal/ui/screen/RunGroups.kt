package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.data.entity.BlockEntity

/**
 * La chiave del primo campo di un gruppo di righe: l'isola che gli sta sopra,
 * o l'inizio della pagina (vedi `groupIntoRenderItems`). Anche il ViewModel la
 * scrive per esteso, quando il campo col cursore deve tenerla anche se sopra
 * di lui nasce un altro campo (`PageEditorViewModel.keepVisibleTogether`).
 */
fun runGroupKey(islandAbove: BlockEntity?, firstLine: BlockEntity): String =
    "run:" + (islandAbove?.id ?: ("top:" + firstLine.pageId))
