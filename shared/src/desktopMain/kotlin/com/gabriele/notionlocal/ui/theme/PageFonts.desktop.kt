package com.gabriele.notionlocal.ui.theme

import androidx.compose.ui.text.font.FontFamily
import com.gabriele.notionlocal.data.entity.PageFont

actual fun PageFont.fontFamily(): FontFamily = when (this) {
    PageFont.GARAMOND, PageFont.GEORGIA, PageFont.TIMES_NEW_ROMAN, PageFont.CAMBRIA,
    PageFont.SONGTI, PageFont.SIMSUN, PageFont.MINCHO, PageFont.KAISEI -> FontFamily.Serif
    PageFont.CONSOLAS -> FontFamily.Monospace
    PageFont.KAITI -> FontFamily.Cursive
    else -> FontFamily.SansSerif
}
