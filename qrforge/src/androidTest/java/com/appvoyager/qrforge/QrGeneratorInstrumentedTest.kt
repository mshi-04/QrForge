package com.appvoyager.qrforge

import android.graphics.Bitmap
import com.appvoyager.qrforge.QrTestFixtures.CUSTOM_MARGIN
import com.appvoyager.qrforge.QrTestFixtures.CUSTOM_SIZE
import com.appvoyager.qrforge.QrTestFixtures.PNG_HEADER
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class QrGeneratorInstrumentedTest {
    @Test
    fun createPngBytesReturnsPngHeader() {
        // Arrange
        val text = "Hello QR"

        // Act
        val bytes = QrGenerator.createPngBytes(text)

        // Assert
        assertArrayEquals(PNG_HEADER, bytes.copyOf(PNG_HEADER.size))
    }

    @Test
    fun createBitmapReturnsAtLeastDefaultWidthBitmap() {
        // Arrange
        val text = "Hello QR"

        // Act
        val bitmap = QrGenerator.createBitmap(text)

        // Assert
        assertTrue(bitmap.width >= QrOptions.DEFAULT_SIZE)
    }

    @Test
    fun createBitmapReturnsAtLeastDefaultHeightBitmap() {
        // Arrange
        val text = "Hello QR"

        // Act
        val bitmap = QrGenerator.createBitmap(text)

        // Assert
        assertTrue(bitmap.height >= QrOptions.DEFAULT_SIZE)
    }

    @Test
    fun createBitmapReturnsArgb8888Config() {
        // Arrange
        val text = "Hello QR"

        // Act
        val bitmap = QrGenerator.createBitmap(text)

        // Assert
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    }

    @Test
    fun createPngBytesWithOptionsReturnsPngHeader() {
        // Arrange
        val options = QrOptions(size = CUSTOM_SIZE, margin = CUSTOM_MARGIN)

        // Act
        val bytes = QrGenerator.createPngBytes(
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
        val bitmap = QrGenerator.createBitmap(
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
        val bitmap = QrGenerator.createBitmap(
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
        val bitmap = QrGenerator.createBitmap(
            text = "Hello custom bitmap",
            options = options,
        )

        // Assert
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    }

    @Test
    fun createPngBytesThrowsGenerationFailedOnDataTooLarge() {
        // Arrange: QR capacity must reject clearly oversized input.
        val oversizedText = "A".repeat(10_000)

        // Act & Assert
        assertThrows(QrGenerationException.GenerationFailed::class.java) {
            QrGenerator.createPngBytes(oversizedText)
        }
    }

    @Test
    fun createPngBytesHandlesSingleCharText() {
        // Arrange
        val text = "a"

        // Act
        val bytes = QrGenerator.createPngBytes(text)

        // Assert
        assertArrayEquals(PNG_HEADER, bytes.copyOf(PNG_HEADER.size))
    }

    @Test
    fun createPngBytesHandlesJapaneseText() {
        // Arrange
        val text = "日本語テスト"

        // Act
        val bytes = QrGenerator.createPngBytes(text)

        // Assert
        assertArrayEquals(PNG_HEADER, bytes.copyOf(PNG_HEADER.size))
    }

    @Test
    fun createBitmapHandlesEmojiText() {
        // Arrange
        val text = "QR 🚀🌟"

        // Act
        val bitmap = QrGenerator.createBitmap(text)

        // Assert
        assertTrue(bitmap.width >= QrOptions.DEFAULT_SIZE)
    }

}
