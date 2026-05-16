package com.appvoyager.qrforge

import org.junit.Assert.assertThrows
import org.junit.Test

class QrForgeTest {
    @Test
    fun createPngBytesThrowsIllegalArgumentForBlankText() {
        assertThrows(IllegalArgumentException::class.java) {
            QrForge.createPngBytes("   ")
        }
    }

    @Test
    fun createPngBytesThrowsIllegalArgumentForEmptyText() {
        assertThrows(IllegalArgumentException::class.java) {
            QrForge.createPngBytes("")
        }
    }

    @Test
    fun createPngBytesThrowsIllegalArgumentForTabAndNewline() {
        assertThrows(IllegalArgumentException::class.java) {
            QrForge.createPngBytes("\t\n")
        }
    }

    @Test
    fun createBitmapThrowsIllegalArgumentForBlankText() {
        assertThrows(IllegalArgumentException::class.java) {
            QrForge.createBitmap("")
        }
    }

    @Test
    fun createPngBytesDoesNotRejectSingleChar() {
        assertThrows(QrForgeException.NativeLibraryUnavailable::class.java) {
            QrForge.createPngBytes("a")
        }
    }

    @Test
    fun createPngBytesDoesNotRejectMultibyteText() {
        assertThrows(QrForgeException.NativeLibraryUnavailable::class.java) {
            QrForge.createPngBytes("日本語テスト")
        }
    }

    @Test
    fun createPngBytesWithOptionsDoesNotRejectRegularText() {
        assertThrows(QrForgeException.NativeLibraryUnavailable::class.java) {
            QrForge.createPngBytes("options", QrOptions(size = 256, margin = 8))
        }
    }

    @Test
    fun qrOptionsRejectsZeroSize() {
        assertThrows(IllegalArgumentException::class.java) {
            QrOptions(size = 0)
        }
    }

    @Test
    fun qrOptionsRejectsTooLargeSize() {
        assertThrows(IllegalArgumentException::class.java) {
            QrOptions(size = 4097)
        }
    }

    @Test
    fun qrOptionsRejectsNegativeMargin() {
        assertThrows(IllegalArgumentException::class.java) {
            QrOptions(margin = -1)
        }
    }

    @Test
    fun qrOptionsRejectsTooLargeMargin() {
        assertThrows(IllegalArgumentException::class.java) {
            QrOptions(margin = 65)
        }
    }
}
