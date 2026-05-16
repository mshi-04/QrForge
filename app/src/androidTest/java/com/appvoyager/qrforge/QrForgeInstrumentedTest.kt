package com.appvoyager.qrforge

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
    fun createBitmapReturnsDefaultSizeBitmap() {
        val bitmap = QrForge.createBitmap("Hello QrForge")

        assertEquals(DEFAULT_SIZE, bitmap.width)
        assertEquals(DEFAULT_SIZE, bitmap.height)
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
