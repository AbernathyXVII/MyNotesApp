package com.gabriele.notionlocal.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * **"---" fa un divisore** (14/10/2026, come su Notion): solo il terzo
 * trattino appena scritto, in una riga che non ha nient'altro.
 */
class DividerShortcutTest {

    @Test
    fun theThirdDashOnItsOwnLineMakesADivider() {
        assertEquals(DividerShortcut(lineIndex = 0, lineStart = 0), dividerShortcutJustTyped("--", "---", 3))
        // Nella seconda riga di un testo: "Uno\n---".
        assertEquals(DividerShortcut(lineIndex = 1, lineStart = 4), dividerShortcutJustTyped("Uno\n--", "Uno\n---", 7))
        // In mezzo, con altre righe sotto.
        assertEquals(DividerShortcut(lineIndex = 1, lineStart = 4), dividerShortcutJustTyped("Uno\n--\nTre", "Uno\n---\nTre", 7))
    }

    @Test
    fun aKeyboardThatTurnedTheFirstTwoIntoADashStillCounts() {
        assertEquals(DividerShortcut(0, 0), dividerShortcutJustTyped("—", "—-", 2))
        assertEquals(DividerShortcut(0, 0), dividerShortcutJustTyped("–", "–-", 2))
    }

    @Test
    fun anythingElseStaysWritten() {
        // Due trattini, quattro, o del testo nella riga.
        assertNull(dividerShortcutJustTyped("-", "--", 2))
        assertNull(dividerShortcutJustTyped("---", "----", 4))
        assertNull(dividerShortcutJustTyped("a--", "a---", 4))
        assertNull(dividerShortcutJustTyped("--a", "---a", 3))
        // Il trattino scritto in un'altra riga, o un'altra lettera.
        assertNull(dividerShortcutJustTyped("---\nab", "---\nabc", 7))
        // Incollato tutto insieme, non scritto.
        assertNull(dividerShortcutJustTyped("", "---", 3))
    }
}
