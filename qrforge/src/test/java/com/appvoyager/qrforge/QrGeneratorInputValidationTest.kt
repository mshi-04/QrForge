package com.appvoyager.qrforge

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class QrGeneratorInputValidationTest {
    @Test
    fun createPngBytesThrowsIllegalArgumentForBlankText() {
        // Arrange
        val text = "   "

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            QrGenerator.createPngBytes(text)
        }
    }

    @Test
    fun createPngBytesThrowsIllegalArgumentForEmptyText() {
        // Arrange
        val text = ""

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            QrGenerator.createPngBytes(text)
        }
    }

    @Test
    fun createPngBytesThrowsIllegalArgumentForTabAndNewline() {
        // Arrange
        val text = "\t\n"

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            QrGenerator.createPngBytes(text)
        }
    }

    @Test
    fun createPngBytesThrowsIllegalArgumentForControlWhitespace() {
        // Arrange: Kotlin/JVM の Char.isWhitespace が扱う情報分離文字
        val text = "\u001C\u001D\u001E\u001F"

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            QrGenerator.createPngBytes(text)
        }
    }

    @Test
    fun createBitmapThrowsIllegalArgumentForBlankText() {
        // Arrange
        val text = ""

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            QrGenerator.createBitmap(text)
        }
    }

    @Test
    fun createPngBytesAcceptsSingleChar() {
        // Arrange
        val text = "a"

        // Act & Assert
        assertDoesNotThrow {
            QrGenerator.createPngBytes(text, QrOptions(), ::generateStubPng)
        }
    }

    @Test
    fun createPngBytesAcceptsMultibyteText() {
        // Arrange
        val text = "日本語テスト"

        // Act & Assert
        assertDoesNotThrow {
            QrGenerator.createPngBytes(text, QrOptions(), ::generateStubPng)
        }
    }

    @Test
    fun createPngBytesAcceptsNextLineControl() {
        // Arrange: NEXT LINE は Kotlin/JVM の Char.isWhitespace ではない
        val text = "\u0085"

        // Act & Assert
        assertDoesNotThrow {
            QrGenerator.createPngBytes(text, QrOptions(), ::generateStubPng)
        }
    }

    @Test
    fun createPngBytesPassesTextWithSurroundingSpacesUnchanged() {
        // Arrange
        val text = "  padded  "
        var receivedText: String? = null

        // Act
        QrGenerator.createPngBytes(
            text = text,
            options = QrOptions(),
            generateQrPng = { nativeText, _, _ ->
                receivedText = nativeText
                byteArrayOf(1)
            },
        )

        // Assert
        assertEquals(text, receivedText)
    }

    @Test
    fun createPngBytesPassesOptionsUnchanged() {
        // Arrange
        val options = QrOptions(size = 768, margin = 6)
        var receivedOptions: Pair<Int, Int>? = null

        // Act
        QrGenerator.createPngBytes(
            text = "text",
            options = options,
            generateQrPng = { _, size, margin ->
                receivedOptions = size to margin
                byteArrayOf(1)
            },
        )

        // Assert
        assertEquals(options.size to options.margin, receivedOptions)
    }

    private fun generateStubPng(
        @Suppress("UNUSED_PARAMETER") text: String,
        @Suppress("UNUSED_PARAMETER") size: Int,
        @Suppress("UNUSED_PARAMETER") margin: Int,
    ): ByteArray = byteArrayOf(1)
}
