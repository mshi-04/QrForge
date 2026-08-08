package io.github.lambdarc.qrforge

sealed class QrGenerationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class GenerationFailed(message: String, cause: Throwable? = null) : QrGenerationException(message, cause)

    class DecodeFailed(message: String, cause: Throwable? = null) : QrGenerationException(message, cause)

    class NativeLibraryUnavailable(message: String, cause: Throwable? = null) : QrGenerationException(message, cause)
}
