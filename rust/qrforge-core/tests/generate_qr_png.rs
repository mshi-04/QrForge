use qrforge_core::{generate_qr_png, QrError};

const PNG_HEADER: &[u8; 8] = b"\x89PNG\r\n\x1a\n";

#[test]
fn returns_png_bytes_for_regular_text() {
    let bytes = generate_qr_png("Hello QrForge").expect("regular text should generate PNG bytes");

    assert!(!bytes.is_empty());
}

#[test]
fn returns_png_bytes_for_url_text() {
    let bytes =
        generate_qr_png("https://example.com").expect("URL text should generate PNG bytes");

    assert!(!bytes.is_empty());
}

#[test]
fn returns_error_for_empty_text() {
    let error = generate_qr_png("").expect_err("empty text should be rejected");

    assert!(matches!(error, QrError::BlankInput));
}

#[test]
fn returns_error_for_whitespace_only_text() {
    let error = generate_qr_png("   \t\n").expect_err("blank text should be rejected");

    assert!(matches!(error, QrError::BlankInput));
}

#[test]
fn returns_png_header() {
    let bytes = generate_qr_png("PNG header check").expect("text should generate PNG bytes");

    assert!(bytes.starts_with(PNG_HEADER));
}
