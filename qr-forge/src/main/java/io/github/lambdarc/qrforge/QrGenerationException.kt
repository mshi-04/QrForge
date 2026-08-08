package io.github.lambdarc.qrforge

/**
 * Signals that a valid request could not be fulfilled.
 *
 * Rejected input is reported as `IllegalArgumentException` and never as this type, so reaching one
 * of these subclasses means the arguments were accepted and the failure happened afterwards. The
 * subclasses separate the three causes a caller can act on differently: ship the native libraries,
 * retry with different input, or give the device more memory.
 *
 * This is a sealed class, so `when` over it can be exhaustive.
 */
sealed class QrGenerationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    /** Encoding the text into a QR code, or the QR code into a PNG, failed. */
    class GenerationFailed(message: String, cause: Throwable? = null) : QrGenerationException(message, cause)

    /**
     * The generated PNG could not be turned into a `Bitmap`.
     *
     * Also raised when the image would exceed the bitmap memory budget, which is checked before
     * decoding so the caller gets this exception instead of an `OutOfMemoryError`.
     */
    class DecodeFailed(message: String, cause: Throwable? = null) : QrGenerationException(message, cause)

    /**
     * The native library could not be loaded, or its JNI entry point could not be resolved.
     *
     * Usually means the `.so` for the device ABI is missing from the packaged application, or a
     * consumer-side shrinker removed the classes the JNI bridge resolves by name.
     */
    class NativeLibraryUnavailable(message: String, cause: Throwable? = null) : QrGenerationException(message, cause)
}
