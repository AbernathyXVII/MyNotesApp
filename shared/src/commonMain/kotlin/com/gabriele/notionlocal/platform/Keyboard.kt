package com.gabriele.notionlocal.platform

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable

/**
 * **La tastiera sullo schermo è aperta** (dal 04/11/2026, per le schermate
 * nel codice comune): sul telefono `WindowInsets.isImeVisible`; sul PC la
 * tastiera sullo schermo non c'è, e la risposta è sempre no.
 */
@Composable
expect fun isKeyboardVisible(): Boolean

/**
 * **Dove arriverà la tastiera** mentre sale o scende: sul telefono
 * `WindowInsets.imeAnimationTarget` (in basso è più di zero se sta salendo o
 * è su); sul PC niente.
 */
@Composable
expect fun keyboardTargetInsets(): WindowInsets
