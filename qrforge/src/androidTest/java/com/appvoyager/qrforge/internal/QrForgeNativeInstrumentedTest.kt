package com.appvoyager.qrforge.internal

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class QrForgeNativeInstrumentedTest {
    @Test
    fun generateQrPngReturnsNonEmptyBytes() {
        // Arrange
        val text = "Hello QrForge"

        // Act
        val bytes = QrForgeNative.generateQrPng(
            text = text,
            size = DEFAULT_SIZE,
            margin = DEFAULT_MARGIN,
        )

        // Assert
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun generateQrPngReturnsPngHeader() {
        // Arrange
        val text = "Hello QrForge"

        // Act
        val bytes = QrForgeNative.generateQrPng(
            text = text,
            size = DEFAULT_SIZE,
            margin = DEFAULT_MARGIN,
        )

        // Assert
        assertArrayEquals(PNG_HEADER, bytes.copyOf(PNG_HEADER.size))
    }

    @Test
    fun generateQrPngWithOptionsReturnsNonEmptyBytes() {
        // Arrange
        val text = "Hello options"

        // Act
        val bytes = QrForgeNative.generateQrPng(
            text = text,
            size = CUSTOM_SIZE,
            margin = CUSTOM_MARGIN,
        )

        // Assert
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun generateQrPngWithOptionsReturnsPngHeader() {
        // Arrange
        val text = "Hello options"

        // Act
        val bytes = QrForgeNative.generateQrPng(
            text = text,
            size = CUSTOM_SIZE,
            margin = CUSTOM_MARGIN,
        )

        // Assert
        assertArrayEquals(PNG_HEADER, bytes.copyOf(PNG_HEADER.size))
    }

    @Test
    fun generateQrPngThrowsOnEmptyText() {
        // Arrange
        val text = ""

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            QrForgeNative.generateQrPng(text, DEFAULT_SIZE, DEFAULT_MARGIN)
        }
    }

    @Test
    fun generateQrPngThrowsOnBlankText() {
        // Arrange
        val text = "   "

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            QrForgeNative.generateQrPng(text, DEFAULT_SIZE, DEFAULT_MARGIN)
        }
    }

    @Test
    fun generateQrPngThrowsOnInvalidSize() {
        // Arrange
        val size = 0

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            QrForgeNative.generateQrPng("invalid size", size, DEFAULT_MARGIN)
        }
    }

    @Test
    fun generateQrPngThrowsOnInvalidMargin() {
        // Arrange
        val margin = 65

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            QrForgeNative.generateQrPng("invalid margin", DEFAULT_SIZE, margin)
        }
    }

    @Test
    fun generateQrPngThrowsGenerationFailedOnDataTooLarge() {
        // Arrange: QR capacity must reject clearly oversized input.
        val oversizedText = "A".repeat(10_000)

        // Act & Assert
        assertThrows(QrForgeNative.GenerationFailed::class.java) {
            QrForgeNative.generateQrPng(oversizedText, DEFAULT_SIZE, DEFAULT_MARGIN)
        }
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
