package com.appvoyager.qrforge

import com.appvoyager.qrforge.internal.QrForgeNative
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class QrForgeExceptionMappingTest {
    @Test
    fun createPngBytesMapsNativeLibraryUnavailable() {
        // Arrange
        val linkError = UnsatisfiedLinkError("test load failure")
        val nativeError = QrForgeNative.NativeLibraryUnavailable(
            message = "QrForge native library is unavailable",
            cause = linkError,
        )

        // Act
        val exception = assertThrows(QrForgeException.NativeLibraryUnavailable::class.java) {
            QrForge.createPngBytes("text", QrOptions()) { _, _, _ -> throw nativeError }
        }

        // Assert
        assertSame(nativeError, exception.cause)
    }

    @Test
    fun createPngBytesMapsGenerationFailed() {
        // Arrange
        val nativeError = QrForgeNative.GenerationFailed("test generation failure")

        // Act
        val exception = assertThrows(QrForgeException.GenerationFailed::class.java) {
            QrForge.createPngBytes("text", QrOptions()) { _, _, _ -> throw nativeError }
        }

        // Assert
        assertSame(nativeError, exception.cause)
    }
}
