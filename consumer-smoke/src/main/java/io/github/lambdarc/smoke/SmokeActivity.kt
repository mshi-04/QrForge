package io.github.lambdarc.smoke

import android.app.Activity
import android.os.Bundle
import io.github.lambdarc.qrforge.QrGenerator
import io.github.lambdarc.qrforge.QrOptions

// 実行しない build-only fixture。公開座標から解決した public API が compile でき、R8 を通しても
// JNI 契約が残ることだけを確かめる。
class SmokeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        QrGenerator.createBitmap(TEXT)
        QrGenerator.createBitmap(TEXT, QrOptions(size = 256, margin = 2))
        QrGenerator.createPngBytes(TEXT)
        QrGenerator.createPngBytes(TEXT, QrOptions())
    }

    private companion object {
        const val TEXT = "https://example.com"
    }
}
