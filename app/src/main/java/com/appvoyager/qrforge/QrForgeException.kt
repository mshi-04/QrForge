package com.appvoyager.qrforge

sealed class QrForgeException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    class InvalidInput(
        message: String,
        cause: Throwable? = null,
    ) : QrForgeException(message, cause)

    class GenerationFailed(
        message: String,
        cause: Throwable? = null,
    ) : QrForgeException(message, cause)

    class DecodeFailed(
        message: String,
        cause: Throwable? = null,
    ) : QrForgeException(message, cause)

    class NativeLibraryUnavailable(
        message: String,
        cause: Throwable? = null,
    ) : QrForgeException(message, cause)
}
