package com.appvoyager.qrforge

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.appvoyager.qrforge.internal.QrForgeNative

object QrForge {
    private val DEFAULT_OPTIONS = QrOptions()

    // ARGB_8888 (BitmapFactory の既定構成) は 1 ピクセル 4 バイト。
    private const val BITMAP_BYTES_PER_PIXEL = 4

    // デコード後 Bitmap の確保メモリ上限。QrOptions.MAX_SIZE から生成され得る最大寸法
    // (約 4400x4400 ≒ 77MiB) に余裕を持たせた値。これを超える確保は捕捉不能な
    // OutOfMemoryError になる前に QrForgeException で明示的に失敗させる。
    internal const val MAX_BITMAP_BYTES = 128L * 1024 * 1024

    fun createBitmap(text: String): Bitmap = createBitmap(text, DEFAULT_OPTIONS)

    fun createBitmap(text: String, options: QrOptions): Bitmap {
        val bytes = createPngBytes(text, options)
        ensureDecodableWithinBudget(bytes)

        return try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw QrForgeException.DecodeFailed("Generated PNG could not be decoded")
        } catch (error: OutOfMemoryError) {
            // デコード時の一括確保で枯渇した場合、捕捉可能な型付き例外に変換して返す。
            throw QrForgeException.DecodeFailed(
                "Generated QR image is too large to allocate as a Bitmap",
                error,
            )
        }
    }

    private fun ensureDecodableWithinBudget(bytes: ByteArray) {
        // inJustDecodeBounds で寸法のみ取得し、巨大確保の前に予算を検査する。
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        ensureWithinBitmapBudget(bounds.outWidth, bounds.outHeight)
    }

    internal fun ensureWithinBitmapBudget(width: Int, height: Int) {
        if (width <= 0 || height <= 0) {
            // 寸法を取得できなかった場合は後続の実デコードにエラー処理を委ねる。
            return
        }

        // width * height * 4 の乗算前に除算で比較し、Long オーバーフローを回避する。
        // (width.toLong() * height.toLong() は Int 同士の積なので Long 内に必ず収まる)
        val maxPixels = MAX_BITMAP_BYTES / BITMAP_BYTES_PER_PIXEL
        if (width.toLong() * height.toLong() > maxPixels) {
            throw QrForgeException.DecodeFailed(
                "Generated QR image (${width}x$height) exceeds the maximum " +
                    "bitmap budget of $MAX_BITMAP_BYTES bytes",
            )
        }
    }

    fun createPngBytes(text: String): ByteArray = createPngBytes(text, DEFAULT_OPTIONS)

    fun createPngBytes(text: String, options: QrOptions): ByteArray {
        val validText = validateText(text)

        return try {
            QrForgeNative.generateQrPng(validText, options.size, options.margin)
        } catch (error: QrForgeNative.NativeLibraryUnavailable) {
            throw QrForgeException.NativeLibraryUnavailable(error.message.orEmpty(), error)
        } catch (error: QrForgeNative.GenerationFailed) {
            throw QrForgeException.GenerationFailed(error.message ?: "QR generation failed", error)
        }
    }

    private fun validateText(text: String): String {
        if (text.isBlank()) {
            throw IllegalArgumentException("QR text must not be blank")
        }

        return text
    }
}
