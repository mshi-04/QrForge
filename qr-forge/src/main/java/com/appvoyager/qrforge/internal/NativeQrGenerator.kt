package com.appvoyager.qrforge.internal

internal object NativeQrGenerator {
    private const val NATIVE_LIBRARY_UNAVAILABLE_MESSAGE =
        "QR native library is unavailable"
    private const val NATIVE_ENTRY_POINT_UNAVAILABLE_MESSAGE =
        "QR native entry point is unavailable"

    @JvmSynthetic
    fun generateQrPng(text: String, size: Int, margin: Int): ByteArray = generateQrPng(
        text = text,
        size = size,
        margin = margin,
        loadLibrary = NativeLibraryLoader::load,
        invokeNative = ::nativeGenerateQrPng,
    )

    // load と entry point 解決の失敗を ambient な native 環境に依存せず検証するため internal に置く。
    // Java 利用者向けの public API ではない。
    @JvmSynthetic
    internal fun generateQrPng(
        text: String,
        size: Int,
        margin: Int,
        loadLibrary: () -> Unit,
        invokeNative: (String, Int, Int) -> ByteArray,
    ): ByteArray {
        mapLinkError(NATIVE_LIBRARY_UNAVAILABLE_MESSAGE) {
            loadLibrary()
        }

        return mapLinkError(NATIVE_ENTRY_POINT_UNAVAILABLE_MESSAGE) {
            invokeNative(text, size, margin)
        }
    }

    @JvmStatic
    private external fun nativeGenerateQrPng(text: String, size: Int, margin: Int): ByteArray

    private inline fun <T> mapLinkError(message: String, block: () -> T): T = try {
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

            System.loadLibrary("qrforge")
            isLoaded = true
        }
    }

    class NativeLibraryUnavailable(message: String, cause: Throwable) : RuntimeException(message, cause)

    class GenerationFailed(message: String) : RuntimeException(message)
}
