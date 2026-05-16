package com.appvoyager.qrforge

import org.junit.Assert.assertThrows
import org.junit.Test

class QrForgeTest {
    @Test
    fun createPngBytesThrowsInvalidInputForBlankText() {
        assertThrows(QrForgeException.InvalidInput::class.java) {
            QrForge.createPngBytes("   ")
        }
    }

    @Test
    fun createPngBytesThrowsInvalidInputForEmptyText() {
        assertThrows(QrForgeException.InvalidInput::class.java) {
            QrForge.createPngBytes("")
        }
    }

    @Test
    fun createPngBytesThrowsInvalidInputForTabAndNewline() {
        assertThrows(QrForgeException.InvalidInput::class.java) {
            QrForge.createPngBytes("\t\n")
        }
    }

    @Test
    fun createBitmapThrowsInvalidInputForBlankText() {
        assertThrows(QrForgeException.InvalidInput::class.java) {
            QrForge.createBitmap("")
        }
    }

    @Test
    fun createPngBytesPassesValidationForSingleChar() {
        assertThrows(QrForgeException.NativeLibraryUnavailable::class.java) {
            QrForge.createPngBytes("a")
        }
    }

    @Test
    fun createPngBytesPassesValidationForMultibyteText() {
        assertThrows(QrForgeException.NativeLibraryUnavailable::class.java) {
            QrForge.createPngBytes("日本語テスト")
        }
    }
}
