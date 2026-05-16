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

    @Test
    fun createPngBytesWithOptionsReturnsPngBytes() {
        val bytes = QrForge.createPngBytes(
            text = "Hello options",
            options = QrOptions(size = CUSTOM_SIZE, margin = CUSTOM_MARGIN),
        )

        assertTrue(bytes.isNotEmpty())
        assertArrayEquals(PNG_HEADER, bytes.copyOf(PNG_HEADER.size))
    }

    @Test
    fun createBitmapWithOptionsReturnsAtLeastCustomSizeBitmap() {
        val bitmap = QrForge.createBitmap(
            text = "Hello custom bitmap",
            options = QrOptions(size = CUSTOM_SIZE, margin = CUSTOM_MARGIN),
        )

        assertTrue(bitmap.width >= CUSTOM_SIZE)
        assertTrue(bitmap.height >= CUSTOM_SIZE)
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    }

    @Test(expected = IllegalArgumentException::class)
    fun createPngBytesThrowsIllegalArgumentForBlankText() {
        QrForge.createPngBytes("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun createBitmapThrowsIllegalArgumentForBlankText() {
        QrForge.createBitmap("")
    }

    private companion object {
        private const val DEFAULT_SIZE = 512
        private const val CUSTOM_SIZE = 768
        private const val CUSTOM_MARGIN = 6
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
