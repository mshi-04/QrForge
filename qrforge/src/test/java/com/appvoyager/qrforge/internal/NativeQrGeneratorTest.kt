package com.appvoyager.qrforge.internal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NativeQrGeneratorTest {
    @Test
    fun generateQrPngMapsLibraryLoadFailure() {
        // Arrange
        val linkError = UnsatisfiedLinkError("test load failure")

        // Act
        val exception = assertThrows(NativeQrGenerator.NativeLibraryUnavailable::class.java) {
            NativeQrGenerator.generateQrPng(
                text = "text",
                size = SAMPLE_SIZE,
                margin = SAMPLE_MARGIN,
                loadLibrary = { throw linkError },
                invokeNative = { _, _, _ -> byteArrayOf(1) },
            )
        }

        // Assert
        assertEquals("QR native library is unavailable", exception.message)
    }

    @Test
    fun generateQrPngMapsMissingEntryPoint() {
        // Arrange
        val linkError = UnsatisfiedLinkError("test entry point failure")

        // Act
        val exception = assertThrows(NativeQrGenerator.NativeLibraryUnavailable::class.java) {
            NativeQrGenerator.generateQrPng(
                text = "text",
                size = SAMPLE_SIZE,
                margin = SAMPLE_MARGIN,
                loadLibrary = {},
                invokeNative = { _, _, _ -> throw linkError },
            )
        }

        // Assert
        assertEquals("QR native entry point is unavailable", exception.message)
    }

    private companion object {
        private const val SAMPLE_SIZE = 512
        private const val SAMPLE_MARGIN = 4
    }
}
