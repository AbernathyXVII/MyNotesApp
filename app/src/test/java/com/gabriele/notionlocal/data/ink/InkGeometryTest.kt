package com.gabriele.notionlocal.data.ink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** I conti dei disegni a mano, senza Android: salvataggio dei punti, le due gomme, gli ancoraggi. */
class InkGeometryTest {

    private fun line(fromX: Float, toX: Float, y: Float = 0f, step: Float = 1f): List<InkPoint> {
        val points = mutableListOf<InkPoint>()
        var x = fromX
        while (x <= toX + 0.001f) {
            points += InkPoint(x, y)
            x += step
        }
        return points
    }

    @Test
    fun pointsSurviveSaving() {
        val points = listOf(InkPoint(12.34f, -5.06f, 0.73f), InkPoint(0f, 100f, 1f), InkPoint(-0.3f, 7.5f))
        val decoded = decodeInkPoints(encodeInkPoints(points))
        assertEquals(3, decoded.size)
        assertEquals(12.3f, decoded[0].x, 0.001f)
        assertEquals(-5.1f, decoded[0].y, 0.001f)
        assertEquals(0.73f, decoded[0].p, 0.001f)
        assertEquals(1f, decoded[1].p, 0.001f)
        assertEquals(-0.3f, decoded[2].x, 0.001f)
        assertEquals(NEUTRAL_PRESSURE, decoded[2].p, 0.001f)
        assertTrue(decodeInkPoints("").isEmpty())
    }

    @Test
    fun theStrokeEraserTouchesOnlyWhatItReaches() {
        val stroke = line(0f, 100f)
        assertTrue(strokeTouched(stroke, halfWidth = 1f, cx = 50f, cy = 5f, radius = 4f))
        assertFalse(strokeTouched(stroke, halfWidth = 1f, cx = 50f, cy = 10f, radius = 4f))
        // Due punti lontani: la gomma in mezzo al segmento lo tocca lo stesso.
        assertTrue(strokeTouched(listOf(InkPoint(0f, 0f), InkPoint(100f, 0f)), 1f, 50f, 0f, 2f))
    }

    @Test
    fun thePointEraserSplitsTheStroke() {
        val pieces = eraseFromStroke(line(0f, 100f), halfWidth = 1f, cx = 50f, cy = 0f, radius = 5f)!!
        assertEquals(2, pieces.size)
        assertTrue(pieces[0].last().x < 45.01f)
        assertTrue(pieces[1].first().x > 54.99f)
        assertEquals(0f, pieces[0].first().x, 0.001f)
        assertEquals(100f, pieces[1].last().x, 0.001f)
    }

    @Test
    fun thePointEraserCutsEvenBetweenFarPoints() {
        // Col dito i punti sono radi: la gomma passa fra due punti e taglia lo stesso.
        val pieces = eraseFromStroke(listOf(InkPoint(0f, 0f), InkPoint(100f, 0f)), 1f, 50f, 0f, 5f)!!
        assertEquals(2, pieces.size)
    }

    @Test
    fun untouchedAndFullyErasedStrokes() {
        assertNull(eraseFromStroke(line(0f, 10f), 1f, 50f, 50f, 5f))
        assertTrue(eraseFromStroke(line(0f, 4f), 1f, 2f, 0f, 10f)!!.isEmpty())
        // Il puntino di un tocco solo si cancella tutto.
        assertTrue(eraseFromStroke(listOf(InkPoint(3f, 3f)), 1f, 3f, 3f, 2f)!!.isEmpty())
    }

    @Test
    fun theDrawingFollowsTheFirstBlockThatIsStillThere() {
        val items = mapOf("__header__" to 0, "b2" to 3, "b3" to 3, "db" to 4)
        assertEquals(3, resolveAnchor("b1,b2,b3", items))
        assertEquals(4, resolveAnchor("db", items))
        assertEquals(0, resolveAnchor("__header__", items))
        // Cancellato quello su cui si era disegnato: il disegno non si vede (ma resta).
        assertNull(resolveAnchor("gone", items))
    }

    @Test
    fun aCopyOfThePageKeepsItsDrawingsOnTheCopiedBlocks() {
        assertEquals("n1,n2,b9", remapAnchor("b1,b2,b9", mapOf("b1" to "n1", "b2" to "n2")))
        assertEquals("__header__", remapAnchor("__header__", mapOf("b1" to "n1")))
    }

    @Test
    fun thicknessSteps() {
        assertEquals(2, nearestPreset(listOf(1f, 2f, 3f, 5f, 8f), 3.2f))
        assertEquals(2.5f, thicker(2f), 0.001f)
        assertEquals(MIN_INK_WIDTH, thinner(0.5f), 0.001f)
        assertEquals(MAX_INK_WIDTH, thicker(59f), 0.001f)
    }
}
