package com.appvoyager.qrforge

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.appvoyager.qrforge.internal.QrForgeNative

object QrForge {
    fun createBitmap(text: String): Bitmap {
        val bytes = createPngBytes(text)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw QrForgeException.DecodeFailed("Generated PNG could not be decoded")
    }

    fun createPngBytes(text: String): ByteArray {
        val validText = validateText(text)

        return try {
            QrForgeNative.generateQrPng(validText)
        } catch (error: QrForgeNative.NativeLibraryUnavailable) {
            throw QrForgeException.NativeLibraryUnavailable(error.message.orEmpty(), error)
        } catch (error: IllegalArgumentException) {
            throw QrForgeException.InvalidInput(error.message ?: "QR text is invalid", error)
        } catch (error: QrForgeNative.GenerationFailed) {
            throw QrForgeException.GenerationFailed(error.message ?: "QR generation failed", error)
        }
    }

    private fun validateText(text: String): String {
        if (text.isBlank()) {
            throw QrForgeException.InvalidInput("QR text must not be blank")
        }

        return text
    }
}
