package com.appvoyager.qrforge.internal

internal object QrForgeNative {
    @JvmSynthetic
    fun generateQrPng(text: String, size: Int, margin: Int): ByteArray {
        NativeLibraryLoader.load()

        return mapLinkError("QrForge native entry point is unavailable") {
            nativeGenerateQrPng(text, size, margin)
        }
    }

    @JvmStatic
    private external fun nativeGenerateQrPng(
        text: String,
        size: Int,
        margin: Int,
    ): ByteArray

    // UnsatisfiedLinkError は library load 時と entry point 解決時の両方で起き得るので、
    // NativeLibraryUnavailable への変換をここに一本化する。
    private inline fun <T> mapLinkError(message: String, block: () -> T): T =
        try {
            block()
        } catch (error: UnsatisfiedLinkError) {
            throw NativeLibraryUnavailable(message = message, cause = error)
        }

    private object NativeLibraryLoader {
        @Volatile private var isLoaded = false

        @Synchronized
        fun load() {
            if (isLoaded) {
                return
            }

            mapLinkError("QrForge native library is unavailable") {
                System.loadLibrary("qrforge")
            }
            isLoaded = true
        }
    }

    class NativeLibraryUnavailable(
        message: String,
        cause: Throwable,
    ) : RuntimeException(message, cause)

    class GenerationFailed(message: String) : RuntimeException(message)
}
