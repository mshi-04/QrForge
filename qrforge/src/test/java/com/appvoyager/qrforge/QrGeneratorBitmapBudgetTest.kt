package com.appvoyager.qrforge

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class QrGeneratorBitmapBudgetTest {
    @Test
    fun ensureWithinBitmapBudgetThrowsWhenOverBudget() {
        // Arrange: 8193x4096x4 bytes は production の 128MiB 予算を超える
        val width = 8193
        val height = 4096

        // Act & Assert
        assertThrows(QrGenerationException.DecodeFailed::class.java) {
            QrGenerator.ensureWithinBitmapBudget(width = width, height = height)
        }
    }

    @Test
    fun ensureWithinBitmapBudgetAcceptsExactBudget() {
        // Arrange: 8192x4096x4 bytes は production の 128MiB 予算と等しい
        val width = 8192
        val height = 4096

        // Act & Assert
        assertDoesNotThrow {
            QrGenerator.ensureWithinBitmapBudget(width = width, height = height)
        }
    }

    @Test
    fun ensureWithinBitmapBudgetAcceptsMaximumQrOutput() {
        // Arrange: 有効な option から生成され得る最大出力
        val sideLength = 4368

        // Act & Assert
        assertDoesNotThrow {
            QrGenerator.ensureWithinBitmapBudget(width = sideLength, height = sideLength)
        }
    }

    @Test
    fun ensureWithinBitmapBudgetIgnoresZeroDimension() {
        // Arrange: bounds decode が寸法を取得できなかったケース
        val width = 0

        // Act & Assert
        assertDoesNotThrow {
            QrGenerator.ensureWithinBitmapBudget(width = width, height = 512)
        }
    }

    @Test
    fun ensureWithinBitmapBudgetIgnoresNegativeDimension() {
        // Arrange: BitmapFactory が返し得る不正な bounds
        val width = -1

        // Act & Assert
        assertDoesNotThrow {
            QrGenerator.ensureWithinBitmapBudget(width = width, height = 512)
        }
    }
}
