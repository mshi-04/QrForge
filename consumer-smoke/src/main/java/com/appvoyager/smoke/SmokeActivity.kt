package com.appvoyager.smoke

import android.app.Activity
import android.os.Bundle
import com.appvoyager.qrforge.QrGenerationException
import com.appvoyager.qrforge.QrGenerator
import com.appvoyager.qrforge.QrOptions

class SmokeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            QrGenerator.createBitmap(TEXT)
            QrGenerator.createBitmap(TEXT, QrOptions(size = 256, margin = 2))
            QrGenerator.createPngBytes(TEXT)
            QrGenerator.createPngBytes(TEXT, QrOptions())
        } catch (error: QrGenerationException) {
            finish()
        }
    }

    private companion object {
        const val TEXT = "https://example.com"
    }
}
