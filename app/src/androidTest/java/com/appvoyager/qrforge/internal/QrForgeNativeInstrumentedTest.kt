package com.appvoyager.qrforge.internal

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrForgeNativeInstrumentedTest {
    @Test
    fun generateQrPngReturnsPngBytes() {
        val bytes = QrForgeNative.generateQrPng(
            text = "Hello QrForge",
            size = DEFAULT_SIZE,
            margin = DEFAULT_MARGIN,
        )

        assertTrue(bytes.isNotEmpty())
        assertArrayEquals(PNG_HEADER, bytes.copyOf(PNG_HEADER.size))
    }

    @Test
    fun generateQrPngReturnsPngBytesWithOptions() {
        val bytes = QrForgeNative.generateQrPng(
            text = "Hello options",
            size = CUSTOM_SIZE,
            margin = CUSTOM_MARGIN,
        )

        assertTrue(bytes.isNotEmpty())
        assertArrayEquals(PNG_HEADER, bytes.copyOf(PNG_HEADER.size))
    }

    @Test(expected = IllegalArgumentException::class)
    fun generateQrPngThrowsOnEmptyText() {
        QrForgeNative.generateQrPng("", DEFAULT_SIZE, DEFAULT_MARGIN)
    }

    @Test(expected = IllegalArgumentException::class)
    fun generateQrPngThrowsOnBlankText() {
        QrForgeNative.generateQrPng("   ", DEFAULT_SIZE, DEFAULT_MARGIN)
    }

    @Test(expected = IllegalArgumentException::class)
    fun generateQrPngThrowsOnInvalidSize() {
        QrForgeNative.generateQrPng("invalid size", 0, DEFAULT_MARGIN)
    }

    private companion object {
        private const val DEFAULT_SIZE = 512
        private const val DEFAULT_MARGIN = 4
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
