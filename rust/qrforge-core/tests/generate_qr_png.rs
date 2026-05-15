use qrforge_core::{generate_qr_png, QrForgeError, QrOptions};

const PNG_HEADER: &[u8; 8] = b"\x89PNG\r\n\x1a\n";

#[test]
fn returns_png_bytes_for_regular_text() {
    let bytes = generate_qr_png("Hello QrForge", &QrOptions::default())
        .expect("regular text should generate PNG bytes");

    assert!(bytes.starts_with(PNG_HEADER));
}

#[test]
fn returns_png_bytes_for_url_text() {
    let bytes = generate_qr_png("https://example.com", &QrOptions::default())
        .expect("URL text should generate PNG bytes");

    assert!(bytes.starts_with(PNG_HEADER));
}

#[test]
fn returns_error_for_empty_text() {
    let error =
        generate_qr_png("", &QrOptions::default()).expect_err("empty text should be rejected");

    assert!(matches!(error, QrForgeError::BlankInput));
}

#[test]
fn returns_error_for_whitespace_only_text() {
    let error = generate_qr_png("   \t\n", &QrOptions::default())
        .expect_err("blank text should be rejected");

    assert!(matches!(error, QrForgeError::BlankInput));
}

#[test]
fn respects_custom_dimensions() {
    let options = QrOptions {
        width: 256,
        height: 256,
    };
    let bytes = generate_qr_png("custom size", &options)
        .expect("text should generate PNG bytes with custom dimensions");

    assert!(bytes.starts_with(PNG_HEADER));
}
