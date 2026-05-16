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
    fun createBitmapThrowsInvalidInputForBlankText() {
        assertThrows(QrForgeException.InvalidInput::class.java) {
            QrForge.createBitmap("")
        }
    }
}
