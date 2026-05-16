package com.appvoyager.qrforge.internal

internal object QrForgeNative {
    fun generateQrPng(text: String): ByteArray {
        NativeLibraryLoader.load()
        return nativeGenerateQrPng(text)
    }

    @JvmStatic
    private external fun nativeGenerateQrPng(text: String): ByteArray

    class NativeLibraryUnavailable(
        message: String,
        cause: Throwable,
    ) : RuntimeException(message, cause)
}

private object NativeLibraryLoader {
    @Volatile private var isLoaded = false

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
