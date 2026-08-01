package com.appvoyager.qrforge

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.appvoyager.qrforge.internal.NativeQrGenerator

object QrGenerator {
    private val DEFAULT_OPTIONS = QrOptions()

    // ARGB_8888 (BitmapFactory の既定構成) は 1 ピクセル 4 バイト。
    private const val BITMAP_BYTES_PER_PIXEL = 4

    // デコード後 Bitmap の確保メモリ上限。QrOptions.MAX_SIZE から生成され得る最大寸法
    // (最大 4368x4368 ≒ 73MiB) に余裕を持たせた値。これを超える確保は捕捉不能な
    // OutOfMemoryError になる前に QrGenerationException で明示的に失敗させる。
    private const val MAX_BITMAP_BYTES = 128L * 1024 * 1024

    @JvmOverloads
    fun createBitmap(text: String, options: QrOptions = DEFAULT_OPTIONS): Bitmap {
        val bytes = createPngBytes(text, options)
        ensureDecodableWithinBudget(bytes)

        return try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw QrGenerationException.DecodeFailed("Generated PNG could not be decoded")
        } catch (error: OutOfMemoryError) {
            // デコード時の一括確保で枯渇した場合、捕捉可能な型付き例外に変換して返す。
            throw QrGenerationException.DecodeFailed(
                "Generated QR image is too large to allocate as a Bitmap",
                error,
            )
        }
    }

    @JvmOverloads
    fun createPngBytes(text: String, options: QrOptions = DEFAULT_OPTIONS): ByteArray =
        createPngBytes(text, options, NativeQrGenerator::generateQrPng)

    // native 実装を使わず wrapper 境界を JVM UnitTest から検証するため internal に置いている。
    // Java 利用者向けの public API ではない。
    @JvmSynthetic
    internal fun createPngBytes(
        text: String,
        options: QrOptions,
        generateQrPng: (String, Int, Int) -> ByteArray,
    ): ByteArray {
        require(text.isNotBlank()) { "QR text must not be blank" }

        return try {
            generateQrPng(text, options.size, options.margin)
        } catch (error: NativeQrGenerator.NativeLibraryUnavailable) {
            throw QrGenerationException.NativeLibraryUnavailable(
                error.message ?: "QR native library is unavailable",
                error,
            )
        } catch (error: NativeQrGenerator.GenerationFailed) {
            throw QrGenerationException.GenerationFailed(error.message ?: "QR generation failed", error)
        }
    }

    private fun ensureDecodableWithinBudget(bytes: ByteArray) {
        // inJustDecodeBounds で寸法のみ取得し、巨大確保の前に予算を検査する。
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        ensureWithinBitmapBudget(bounds.outWidth, bounds.outHeight)
    }

    // BitmapFactory を使わない予算判定を JVM UnitTest から検証するため internal に置いている。
    // Java 利用者向けの public API ではない。
    @JvmSynthetic
    internal fun ensureWithinBitmapBudget(width: Int, height: Int) {
        if (width <= 0 || height <= 0) {
            // 寸法を取得できなかった場合は後続の実デコードにエラー処理を委ねる。
            return
        }

        // width * height * 4 の乗算前に除算で比較し、Long オーバーフローを回避する。
        // width と height は正の Int なので、Long へ変換した積は Long の範囲内に収まる。
        val maxPixels = MAX_BITMAP_BYTES / BITMAP_BYTES_PER_PIXEL
        if (width.toLong() * height.toLong() > maxPixels) {
            throw QrGenerationException.DecodeFailed(
                "Generated QR image (${width}x$height) exceeds the maximum " +
                    "bitmap budget of $MAX_BITMAP_BYTES bytes",
            )
        }
    }
}
