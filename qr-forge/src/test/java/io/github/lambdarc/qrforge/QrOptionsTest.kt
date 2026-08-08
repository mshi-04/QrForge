package io.github.lambdarc.qrforge

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class QrOptionsTest {
    @Test
    fun qrOptionsRejectsZeroSize() {
        // Arrange
        val size = 0

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            QrOptions(size = size)
        }
    }

    @Test
    fun qrOptionsRejectsTooLargeSize() {
        // Arrange
        val size = QrOptions.MAX_SIZE + 1

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            QrOptions(size = size)
        }
    }

    @Test
    fun qrOptionsRejectsNegativeMargin() {
        // Arrange
        val margin = -1

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            QrOptions(margin = margin)
        }
    }

    @Test
    fun qrOptionsRejectsTooLargeMargin() {
        // Arrange
        val margin = QrOptions.MAX_MARGIN + 1

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            QrOptions(margin = margin)
        }
    }

    @Test
    fun qrOptionsUsesDefaultSize() {
        // Arrange
        val options = QrOptions()

        // Act
        val size = options.size

        // Assert
        assertEquals(QrOptions.DEFAULT_SIZE, size)
    }

    @Test
    fun qrOptionsUsesDefaultMargin() {
        // Arrange
        val options = QrOptions()

        // Act
        val margin = options.margin

        // Assert
        assertEquals(QrOptions.DEFAULT_MARGIN, margin)
    }

    @Test
    fun qrOptionsAcceptsMinSize() {
        // Arrange
        val options = QrOptions(size = QrOptions.MIN_SIZE)

        // Act
        val size = options.size

        // Assert
        assertEquals(QrOptions.MIN_SIZE, size)
    }

    @Test
    fun qrOptionsAcceptsMaxSize() {
        // Arrange
        val options = QrOptions(size = QrOptions.MAX_SIZE)

        // Act
        val size = options.size

        // Assert
        assertEquals(QrOptions.MAX_SIZE, size)
    }

    @Test
    fun qrOptionsAcceptsMinMargin() {
        // Arrange
        val options = QrOptions(margin = QrOptions.MIN_MARGIN)

        // Act
        val margin = options.margin

        // Assert
        assertEquals(QrOptions.MIN_MARGIN, margin)
    }

    @Test
    fun qrOptionsAcceptsMaxMargin() {
        // Arrange
        val options = QrOptions(margin = QrOptions.MAX_MARGIN)

        // Act
        val margin = options.margin

        // Assert
        assertEquals(QrOptions.MAX_MARGIN, margin)
    }
}
