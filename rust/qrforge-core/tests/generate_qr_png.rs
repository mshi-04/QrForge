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
        size: 256,
        margin: 4,
    };
    let bytes = generate_qr_png("custom size", &options)
        .expect("text should generate PNG bytes with custom dimensions");

    assert!(png_width(&bytes) >= 256);
}

#[test]
fn applies_custom_margin_to_output_dimensions() {
    let without_margin = generate_qr_png("custom margin", &QrOptions { size: 1, margin: 0 })
        .expect("text should generate PNG without margin");
    let with_margin = generate_qr_png("custom margin", &QrOptions { size: 1, margin: 8 })
        .expect("text should generate PNG with margin");

    assert!(png_width(&with_margin) > png_width(&without_margin));
}

#[test]
fn returns_error_for_zero_size() {
    let error = generate_qr_png("invalid size", &QrOptions { size: 0, margin: 4 })
        .expect_err("zero size should be rejected");

    assert!(matches!(error, QrForgeError::InvalidOptions(_)));
}

#[test]
fn returns_error_for_too_large_size() {
    let error = generate_qr_png(
        "invalid size",
        &QrOptions {
            size: 4097,
            margin: 4,
        },
    )
    .expect_err("too large size should be rejected");

    assert!(matches!(error, QrForgeError::InvalidOptions(_)));
}

#[test]
fn returns_error_for_too_large_margin() {
    let error = generate_qr_png(
        "invalid margin",
        &QrOptions {
            size: 512,
            margin: 65,
        },
    )
    .expect_err("too large margin should be rejected");

    assert!(matches!(error, QrForgeError::InvalidOptions(_)));
}

fn png_width(bytes: &[u8]) -> u32 {
    assert!(bytes.starts_with(PNG_HEADER));
    u32::from_be_bytes([bytes[16], bytes[17], bytes[18], bytes[19]])
}
