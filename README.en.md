# QrForge

QrForge is a Rust-powered QR code generation library exposed to Android applications through a Kotlin API.

Its public API is centered on `QrGenerator`, while QR generation and PNG encoding are handled in Rust. JNI and native-library details remain internal, allowing Android clients to use QrForge like a regular Kotlin library.

## Features

- Generate QR codes as PNG data from strings
- Generate QR codes as Android `Bitmap` objects
- Configure the minimum image size and quiet-zone margin
- Distinguish invalid input, native-library loading failures, generation failures, and image-decoding failures by exception type
- Support `arm64-v8a`, `armeabi-v7a`, and `x86_64`
- Keep QR generation logic in a Rust core that is independent of Android

## Usage

Add the Maven Central dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.lambdarc:qr-forge:1.0.0")
}
```

### Generate a Bitmap

```kotlin
import com.appvoyager.qrforge.QrGenerator

val bitmap = QrGenerator.createBitmap("https://example.com")
imageView.setImageBitmap(bitmap)
```

### Generate PNG data

```kotlin
import com.appvoyager.qrforge.QrGenerator

val pngBytes: ByteArray = QrGenerator.createPngBytes("Hello QR")
```

### Configure the size and margin

```kotlin
import com.appvoyager.qrforge.QrGenerator
import com.appvoyager.qrforge.QrOptions

val bitmap = QrGenerator.createBitmap(
    text = "https://example.com",
    options = QrOptions(
        size = 768,
        margin = 6,
    ),
)
```

## Options

| Property | Default | Range | Description |
|----------|---------|-------|-------------|
| `size` | `512` | `1..4096` | Minimum side length of the output image in pixels |
| `margin` | `4` | `0..64` | Quiet-zone width around the QR code in QR modules |

The actual output image may be larger than `size` because it is expanded to align with QR module boundaries. Set `margin` to `0` to disable the quiet zone.

A blank string or an out-of-range option causes an `IllegalArgumentException`. Leading and trailing whitespace is preserved and encoded as part of the QR content.

## Error handling

| Case | Exception |
|------|-----------|
| Blank string or out-of-range option | `IllegalArgumentException` |
| Native library cannot be loaded or its JNI entry point cannot be resolved | `QrGenerationException.NativeLibraryUnavailable` |
| QR or PNG generation fails | `QrGenerationException.GenerationFailed` |
| PNG decoding or `Bitmap` allocation fails | `QrGenerationException.DecodeFailed` |

`QrGenerationException` is a sealed class, so callers can handle every library failure with an exhaustive `when` expression.

```kotlin
import com.appvoyager.qrforge.QrGenerationException
import com.appvoyager.qrforge.QrGenerator

try {
    val bitmap = QrGenerator.createBitmap("https://example.com")
} catch (error: QrGenerationException) {
    when (error) {
        is QrGenerationException.NativeLibraryUnavailable -> Unit
        is QrGenerationException.GenerationFailed -> Unit
        is QrGenerationException.DecodeFailed -> Unit
    }
}
```

## Architecture

```text
Android app
    ↓
Kotlin public API (QrGenerator, QrOptions, QrGenerationException)
    ↓
Kotlin JNI binding
    ↓
Rust JNI bridge
    ↓
Rust core (QR generation and PNG encoding)
```

Android applications and library consumers are not expected to call the JNI API directly.

## Supported environments

- Android API 28 and later
- `arm64-v8a`
- `armeabi-v7a`
- `x86_64`

The Android library module packages `libqrforge.so` for each supported ABI.

## Availability

Published versions are available from Maven Central as `io.github.lambdarc:qr-forge:<version>`.
Before the first release, QrForge can be used as the `:qrforge` Android library module in this repository.

## License

[MIT License](LICENSE)
