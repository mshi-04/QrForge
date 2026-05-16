package com.appvoyager.qrforge

import android.graphics.Bitmap
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class QrForgeInstrumentedTest {
    @Test
    fun createPngBytesReturnsNonEmptyBytes() {
        // Arrange
        val text = "Hello QrForge"

        // Act
        val bytes = QrForge.createPngBytes(text)

        // Assert
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun createPngBytesReturnsPngHeader() {
        // Arrange
        val text = "Hello QrForge"

        // Act
        val bytes = QrForge.createPngBytes(text)

        // Assert
        assertArrayEquals(PNG_HEADER, bytes.copyOf(PNG_HEADER.size))
    }

    @Test
    fun createBitmapReturnsAtLeastDefaultWidthBitmap() {
        // Arrange
        val text = "Hello QrForge"

        // Act
        val bitmap = QrForge.createBitmap(text)

        // Assert
        assertTrue(bitmap.width >= QrOptions.DEFAULT_SIZE)
    }

    @Test
    fun createBitmapReturnsAtLeastDefaultHeightBitmap() {
        // Arrange
        val text = "Hello QrForge"

        // Act
        val bitmap = QrForge.createBitmap(text)

        // Assert
        assertTrue(bitmap.height >= QrOptions.DEFAULT_SIZE)
    }

    @Test
    fun createBitmapReturnsArgb8888Config() {
        // Arrange
        val text = "Hello QrForge"

        // Act
        val bitmap = QrForge.createBitmap(text)

        // Assert
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    }

    @Test
    fun createPngBytesWithOptionsReturnsNonEmptyBytes() {
        // Arrange
        val options = QrOptions(size = CUSTOM_SIZE, margin = CUSTOM_MARGIN)

        // Act
        val bytes = QrForge.createPngBytes(
            text = "Hello options",
            options = options,
        )

        // Assert
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun createPngBytesWithOptionsReturnsPngHeader() {
        // Arrange
        val options = QrOptions(size = CUSTOM_SIZE, margin = CUSTOM_MARGIN)

        // Act
        val bytes = QrForge.createPngBytes(
            text = "Hello options",
            options = options,
        )

        // Assert
        assertArrayEquals(PNG_HEADER, bytes.copyOf(PNG_HEADER.size))
    }

    @Test
    fun createBitmapWithOptionsReturnsAtLeastCustomWidthBitmap() {
        // Arrange
        val options = QrOptions(size = CUSTOM_SIZE, margin = CUSTOM_MARGIN)

        // Act
        val bitmap = QrForge.createBitmap(
            text = "Hello custom bitmap",
            options = options,
        )

        // Assert
        assertTrue(bitmap.width >= CUSTOM_SIZE)
    }

    @Test
    fun createBitmapWithOptionsReturnsAtLeastCustomHeightBitmap() {
        // Arrange
        val options = QrOptions(size = CUSTOM_SIZE, margin = CUSTOM_MARGIN)

        // Act
        val bitmap = QrForge.createBitmap(
            text = "Hello custom bitmap",
            options = options,
        )

        // Assert
        assertTrue(bitmap.height >= CUSTOM_SIZE)
    }

    @Test
    fun createBitmapWithOptionsReturnsArgb8888Config() {
        // Arrange
        val options = QrOptions(size = CUSTOM_SIZE, margin = CUSTOM_MARGIN)

        // Act
        val bitmap = QrForge.createBitmap(
            text = "Hello custom bitmap",
            options = options,
        )

        // Assert
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    }

    @Test
    fun createPngBytesThrowsIllegalArgumentForBlankText() {
        // Arrange
        val text = "   "

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            QrForge.createPngBytes(text)
        }
    }

    @Test
    fun createBitmapThrowsIllegalArgumentForBlankText() {
        // Arrange
        val text = ""

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            QrForge.createBitmap(text)
        }
    }

    private companion object {
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
