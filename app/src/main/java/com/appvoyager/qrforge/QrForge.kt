package com.appvoyager.qrforge

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.appvoyager.qrforge.internal.QrForgeNative

object QrForge {
    fun createBitmap(text: String): Bitmap = createBitmap(text, QrOptions())

    fun createBitmap(text: String, options: QrOptions): Bitmap {
        val bytes = createPngBytes(text, options)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw QrForgeException.DecodeFailed("Generated PNG could not be decoded")
    }

    fun createPngBytes(text: String): ByteArray = createPngBytes(text, QrOptions())

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
