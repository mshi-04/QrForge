package io.github.lambdarc.qrforge

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.github.lambdarc.qrforge.internal.NativeQrGenerator

object QrGenerator {
    private val DEFAULT_OPTIONS = QrOptions()

    private const val BITMAP_BYTES_PER_PIXEL = 4

    private const val MAX_BITMAP_BYTES = 128L * 1024 * 1024

    @JvmOverloads
    fun createBitmap(text: String, options: QrOptions = DEFAULT_OPTIONS): Bitmap {
        val bytes = createPngBytes(text, options)
        ensureDecodableWithinBudget(bytes)

        return try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw QrGenerationException.DecodeFailed("Generated PNG could not be decoded")
        } catch (error: OutOfMemoryError) {
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
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        ensureWithinBitmapBudget(bounds.outWidth, bounds.outHeight)
    }

    // BitmapFactory を使わない予算判定を JVM UnitTest から検証するため internal に置いている。
    // Java 利用者向けの public API ではない。
    @JvmSynthetic
    internal fun ensureWithinBitmapBudget(width: Int, height: Int) {
        if (width <= 0 || height <= 0) {
            return
        }

        val maxPixels = MAX_BITMAP_BYTES / BITMAP_BYTES_PER_PIXEL
        if (width.toLong() * height.toLong() > maxPixels) {
            throw QrGenerationException.DecodeFailed(
                "Generated QR image (${width}x$height) exceeds the maximum " +
                    "bitmap budget of $MAX_BITMAP_BYTES bytes",
            )
        }
    }
}
