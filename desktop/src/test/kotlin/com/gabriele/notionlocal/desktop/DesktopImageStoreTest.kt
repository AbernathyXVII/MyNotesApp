package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.platform.DesktopContext
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

/**
 * **Le immagini delle pagine sul PC** (dal 04/11/2026): la stessa
 * `PageImageStore` del telefono, con la cartella dei dati del PC (qui una
 * temporanea) e Skia per leggere le immagini.
 *
 * La foto di prova è fatta qui: larga 40 e alta 20, rossa a sinistra e blu a
 * destra, con l'etichetta EXIF "girala di 90 gradi" che scrivono i telefoni
 * per le foto in verticale. Si guarda alta 40 e larga 20, col rosso in alto.
 */
class DesktopImageStoreTest {
    private val dir: File = Files.createTempDirectory("pageflow-images").toFile()
    private val store = PageImageStore(DesktopContext(dir))

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    /** Una JPEG rossa e blu con l'orientamento EXIF 6 ("ruota di 90 gradi in senso orario"). */
    private fun turnedPhoto(): ByteArray {
        val image = BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until 40) for (y in 0 until 20) image.setRGB(x, y, if (x < 20) Color.RED.rgb else Color.BLUE.rgb)
        val jpeg = ByteArrayOutputStream().also { ImageIO.write(image, "jpg", it) }.toByteArray()
        // Il segmento APP1 "Exif": intestazione TIFF (little endian) con una
        // sola voce, Orientation (0x0112) = 6.
        val tiff = byteArrayOf(
            0x49, 0x49, 0x2A, 0x00, 0x08, 0x00, 0x00, 0x00, // II*, primo IFD a 8
            0x01, 0x00, // una voce
            0x12, 0x01, 0x03, 0x00, 0x01, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, // Orientation SHORT 1 = 6
            0x00, 0x00, 0x00, 0x00 // nessun altro IFD
        )
        val payload = "Exif".toByteArray() + byteArrayOf(0, 0) + tiff
        val length = payload.size + 2
        val app1 = byteArrayOf(0xFF.toByte(), 0xE1.toByte(), (length shr 8).toByte(), (length and 0xFF).toByte()) + payload
        // Subito dopo l'inizio dell'immagine (FF D8).
        return jpeg.copyOfRange(0, 2) + app1 + jpeg.copyOfRange(2, jpeg.size)
    }

    private fun isReddish(argb: Int) = (argb shr 16 and 0xFF) > 180 && (argb and 0xFF) < 90
    private fun isBluish(argb: Int) = (argb and 0xFF) > 180 && (argb shr 16 and 0xFF) < 90

    @Test
    fun aTurnedPhotoIsMeasuredAndShownUpright() = runBlocking {
        val name = store.saveImageBytes(turnedPhoto())
        assertNotNull("la foto non è stata salvata", name)
        assertTrue(File(dir, "${PageImageStore.DIRECTORY}/$name").isFile)
        assertEquals(20 to 40, store.imageSize(name!!))

        val bitmap = store.load(name, targetWidthPx = 10)!!
        assertEquals(10, bitmap.width)
        assertEquals(20, bitmap.height)
        val seen = (0 until 20 step 2).map { y -> Integer.toHexString(bitmap.getColor(5, y)) }
        assertTrue("in alto dovrebbe esserci il rosso: $seen", isReddish(bitmap.getColor(5, 3)))
        assertTrue("in basso dovrebbe esserci il blu", isBluish(bitmap.getColor(5, 16)))
    }

    @Test
    fun whatIsNotAnImageDoesNotStay() = runBlocking {
        assertNull(store.saveImageBytes("non sono un'immagine".toByteArray()))
        val result = store.saveFromStream({ "neanche io".byteInputStream() }, mustBeImage = true, cannotOpen = "no")
        assertTrue(result.isFailure)
        assertEquals(0, File(dir, PageImageStore.DIRECTORY).listFiles()?.size ?: 0)
    }

    @Test
    fun aCopyHasItsOwnFileAndDeletingOneKeepsTheOther() = runBlocking {
        val name = store.saveImageBytes(turnedPhoto())!!
        val copy = store.copy(name)!!
        assertFalse(name == copy)
        store.delete(name)
        assertFalse(store.fileFor(name).exists())
        assertTrue(store.fileFor(copy).exists())
        assertEquals(20 to 40, store.imageSize(copy))
    }
}
