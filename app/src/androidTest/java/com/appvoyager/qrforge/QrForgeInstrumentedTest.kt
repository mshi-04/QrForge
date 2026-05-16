package com.appvoyager.qrforge

import android.graphics.Bitmap
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrForgeInstrumentedTest {
    @Test
    fun createPngBytesReturnsPngBytes() {
        val bytes = QrForge.createPngBytes("Hello QrForge")

        assertTrue(bytes.isNotEmpty())
        assertArrayEquals(PNG_HEADER, bytes.copyOf(PNG_HEADER.size))
    }

    @Test
    fun createBitmapReturnsAtLeastDefaultSizeBitmap() {
        val bitmap = QrForge.createBitmap("Hello QrForge")

        assertTrue(bitmap.width >= DEFAULT_SIZE)
        assertTrue(bitmap.height >= DEFAULT_SIZE)
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    }

    @Test(expected = QrForgeException.InvalidInput::class)
    fun createPngBytesThrowsInvalidInputForBlankText() {
        QrForge.createPngBytes("   ")
    }

    @Test(expected = QrForgeException.InvalidInput::class)
    fun createBitmapThrowsInvalidInputForBlankText() {
        QrForge.createBitmap("")
    }

    private companion object {
        private const val DEFAULT_SIZE = 512
        private val PNG_HEADER = byteArrayOf(
            0x89.toByte(),
            0x50,
            0x4E,
            0x47,
            0x0D,
            0x0A,
            0x1A,
            0x0A,
        )
    }
}
