package com.appvoyager.qrforge.internal

internal object QrForgeNative {
    @JvmSynthetic
    fun generateQrPng(text: String, size: Int, margin: Int): ByteArray {
        NativeLibraryLoader.load()
        return try {
            nativeGenerateQrPng(text, size, margin)
        } catch (error: UnsatisfiedLinkError) {
            throw NativeLibraryUnavailable(
                message = "QrForge native entry point is unavailable",
                cause = error,
            )
        }
    }

    @JvmStatic
    private external fun nativeGenerateQrPng(
        text: String,
        size: Int,
        margin: Int,
    ): ByteArray

    class NativeLibraryUnavailable(
        message: String,
        cause: Throwable,
    ) : RuntimeException(message, cause)

    class GenerationFailed(message: String) : RuntimeException(message)
}

private object NativeLibraryLoader {
    @Volatile private var isLoaded = false

    @Synchronized
    fun load() {
        if (isLoaded) {
            return
        }

        try {
            System.loadLibrary("qrforge")
            isLoaded = true
        } catch (error: UnsatisfiedLinkError) {
            throw QrForgeNative.NativeLibraryUnavailable(
                message = "QrForge native library is unavailable",
                cause = error,
            )
        }
    }
}
