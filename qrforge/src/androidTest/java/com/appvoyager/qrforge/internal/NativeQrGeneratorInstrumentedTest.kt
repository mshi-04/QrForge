package com.appvoyager.qrforge.internal

import com.appvoyager.qrforge.QrOptions
import com.appvoyager.qrforge.QrTestFixtures.CUSTOM_MARGIN
import com.appvoyager.qrforge.QrTestFixtures.CUSTOM_SIZE
import com.appvoyager.qrforge.QrTestFixtures.PNG_HEADER
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NativeQrGeneratorInstrumentedTest {
    @Test
    fun generateQrPngReturnsPngHeader() {
        // Arrange
        val text = "Hello QR"

        // Act
        val bytes = NativeQrGenerator.generateQrPng(
            text = text,
            size = VALID_SIZE,
            margin = VALID_MARGIN,
        )

        // Assert
        assertArrayEquals(PNG_HEADER, bytes.copyOf(PNG_HEADER.size))
    }

    @Test
    fun generateQrPngWithOptionsReturnsPngHeader() {
        // Arrange
        val text = "Hello options"

        // Act
        val bytes = NativeQrGenerator.generateQrPng(
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
            NativeQrGenerator.generateQrPng(text, VALID_SIZE, VALID_MARGIN)
        }
    }

    @Test
    fun generateQrPngThrowsOnBlankText() {
        // Arrange
        val text = "   "

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            NativeQrGenerator.generateQrPng(text, VALID_SIZE, VALID_MARGIN)
        }
    }

    @Test
    fun generateQrPngThrowsOnInvalidSize() {
        // Arrange
        val size = 0

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            NativeQrGenerator.generateQrPng("invalid size", size, VALID_MARGIN)
        }
    }

    @Test
    fun generateQrPngThrowsOnInvalidMargin() {
        // Arrange
        val margin = QrOptions.MAX_MARGIN + 1

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            NativeQrGenerator.generateQrPng("invalid margin", VALID_SIZE, margin)
        }
    }

    @Test
    fun generateQrPngThrowsOnNegativeSize() {
        // Arrange: JNI 境界での u32 変換前に弾かれる
        val size = -1

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            NativeQrGenerator.generateQrPng("negative size", size, VALID_MARGIN)
        }
    }

    @Test
    fun generateQrPngThrowsOnNegativeMargin() {
        // Arrange: JNI 境界での u32 変換前に弾かれる
        val margin = -1

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            NativeQrGenerator.generateQrPng("negative margin", VALID_SIZE, margin)
        }
    }

    @Test
    fun generateQrPngThrowsGenerationFailedOnDataTooLarge() {
        // Arrange: QR capacity must reject clearly oversized input.
        val oversizedText = "A".repeat(10_000)

        // Act & Assert
        assertThrows(NativeQrGenerator.GenerationFailed::class.java) {
            NativeQrGenerator.generateQrPng(oversizedText, VALID_SIZE, VALID_MARGIN)
        }
    }

    private companion object {
        // JNI binding は default 値を持たないので、単に有効な値として扱う。
        private const val VALID_SIZE = QrOptions.DEFAULT_SIZE
        private const val VALID_MARGIN = QrOptions.DEFAULT_MARGIN
    }
}
