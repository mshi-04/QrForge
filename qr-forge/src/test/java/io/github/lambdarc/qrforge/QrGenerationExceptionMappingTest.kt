package io.github.lambdarc.qrforge

import io.github.lambdarc.qrforge.internal.NativeQrGenerator
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class QrGenerationExceptionMappingTest {
    @Test
    fun createPngBytesMapsNativeLibraryUnavailable() {
        // Arrange
        val linkError = UnsatisfiedLinkError("test load failure")
        val nativeError = NativeQrGenerator.NativeLibraryUnavailable(
            message = "QR native library is unavailable",
            cause = linkError,
        )

        // Act
        val exception = assertThrows(QrGenerationException.NativeLibraryUnavailable::class.java) {
            QrGenerator.createPngBytes("text", QrOptions()) { _, _, _ -> throw nativeError }
        }

        // Assert
        assertSame(nativeError, exception.cause)
    }

    @Test
    fun createPngBytesMapsGenerationFailed() {
        // Arrange
        val nativeError = NativeQrGenerator.GenerationFailed("test generation failure")

        // Act
        val exception = assertThrows(QrGenerationException.GenerationFailed::class.java) {
            QrGenerator.createPngBytes("text", QrOptions()) { _, _, _ -> throw nativeError }
        }

        // Assert
        assertSame(nativeError, exception.cause)
    }
}
