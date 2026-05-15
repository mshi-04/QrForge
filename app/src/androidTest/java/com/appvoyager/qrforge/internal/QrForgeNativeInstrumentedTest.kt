package com.appvoyager.qrforge.internal

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrForgeNativeInstrumentedTest {
    @Test
    fun generateQrPngReturnsPngBytes() {
        val bytes = QrForgeNative.generateQrPng("Hello QrForge")

        assertTrue(bytes.isNotEmpty())
        assertArrayEquals(PNG_HEADER, bytes.copyOf(PNG_HEADER.size))
    }

    @Test(expected = IllegalArgumentException::class)
    fun generateQrPngThrowsOnEmptyText() {
        QrForgeNative.generateQrPng("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun generateQrPngThrowsOnBlankText() {
        QrForgeNative.generateQrPng("   ")
    }

    private companion object {
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
