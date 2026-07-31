package com.appvoyager.qrforge

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class QrForgeTest {
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
    fun createPngBytesThrowsIllegalArgumentForEmptyText() {
        // Arrange
        val text = ""

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            QrForge.createPngBytes(text)
        }
    }

    @Test
    fun createPngBytesThrowsIllegalArgumentForTabAndNewline() {
        // Arrange
        val text = "\t\n"

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

    // native library を解決できない JVM 上では「生成が成功すること」を検証できないため、
    // 入力検証そのものを直接確認する。native 経路は Instrumented Test で担保する。
    @Test
    fun requireNonBlankTextAcceptsSingleChar() {
        // Arrange
        val text = "a"

        // Act & Assert
        assertDoesNotThrow {
            QrForge.requireNonBlankText(text)
        }
    }

    @Test
    fun requireNonBlankTextAcceptsMultibyteText() {
        // Arrange
        val text = "日本語テスト"

        // Act & Assert
        assertDoesNotThrow {
            QrForge.requireNonBlankText(text)
        }
    }

    @Test
    fun requireNonBlankTextAcceptsTextWithSurroundingSpaces() {
        // Arrange: SDK 側で trim しないため、前後の空白は入力として受理する
        val text = "  padded  "

        // Act & Assert
        assertDoesNotThrow {
            QrForge.requireNonBlankText(text)
        }
    }

    @Test
    fun requireNonBlankTextRejectsBlankText() {
        // Arrange
        val text = "   "

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            QrForge.requireNonBlankText(text)
        }
    }

    @Test
    fun ensureWithinBitmapBudgetThrowsWhenOverBudget() {
        // Arrange: MAX_BITMAP_BYTES を 1 ピクセル超える正方形寸法
        val side = Math.sqrt((QrForge.MAX_BITMAP_BYTES / 4L + 1L).toDouble()).toInt() + 1

        // Act & Assert
        assertThrows(QrForgeException.DecodeFailed::class.java) {
            QrForge.ensureWithinBitmapBudget(side, side)
        }
    }

    @Test
    fun ensureWithinBitmapBudgetAcceptsMaxSizeOutput() {
        // Arrange: QrOptions.MAX_SIZE から生成され得る最大寸法に相当する大きさ
        val side = 4400

        // Act & Assert: 例外なく通過する
        QrForge.ensureWithinBitmapBudget(side, side)
    }

    @Test
    fun ensureWithinBitmapBudgetIgnoresNonPositiveDimensions() {
        // Act & Assert: 寸法不明 (0 / 負) は後続デコードに委ねるため通過する
        QrForge.ensureWithinBitmapBudget(0, 0)
        QrForge.ensureWithinBitmapBudget(-1, 512)
    }

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
