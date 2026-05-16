use qrforge_core::{generate_qr_png, QrForgeError, QrOptions};

const PNG_HEADER: &[u8; 8] = b"\x89PNG\r\n\x1a\n";

#[test]
fn returns_png_bytes_for_regular_text() {
    // Arrange
    let text = "Hello QrForge";
    let options = QrOptions::default();

    // Act
    let bytes = generate_qr_png(text, &options).expect("regular text should generate PNG bytes");

    // Assert
    assert!(bytes.starts_with(PNG_HEADER));
}

#[test]
fn returns_png_bytes_for_url_text() {
    // Arrange
    let text = "https://example.com";
    let options = QrOptions::default();

    // Act
    let bytes = generate_qr_png(text, &options).expect("URL text should generate PNG bytes");

    // Assert
    assert!(bytes.starts_with(PNG_HEADER));
}

#[test]
fn returns_error_for_empty_text() {
    // Arrange
    let text = "";
    let options = QrOptions::default();

    // Act
    let error = generate_qr_png(text, &options).expect_err("empty text should be rejected");

    // Assert
    assert!(matches!(error, QrForgeError::BlankInput));
}

#[test]
fn returns_error_for_blank_text() {
    // Arrange
    let text = "   ";
    let options = QrOptions::default();

    // Act
    let error = generate_qr_png(text, &options).expect_err("blank text should be rejected");

    // Assert
    assert!(matches!(error, QrForgeError::BlankInput));
}

#[test]
fn returns_error_for_whitespace_only_text() {
    // Arrange
    let text = "   \t\n";
    let options = QrOptions::default();

    // Act
    let error = generate_qr_png(text, &options).expect_err("blank text should be rejected");

    // Assert
    assert!(matches!(error, QrForgeError::BlankInput));
}

#[test]
fn respects_custom_dimensions() {
    // Arrange
    let options = QrOptions {
        size: 256,
        margin: 4,
    };
    let bytes = generate_qr_png("custom size", &options)
        .expect("text should generate PNG bytes with custom dimensions");

    // Assert
    assert!(bytes.starts_with(PNG_HEADER));
    assert!(png_width(&bytes) >= 256);
}

#[test]
fn applies_zero_margin_to_png_output() {
    // Arrange
    let options = QrOptions { size: 1, margin: 0 };

    // Act
    let bytes = generate_qr_png("zero margin", &options)
        .expect("text should generate PNG bytes without margin");

    // Assert
    assert!(bytes.starts_with(PNG_HEADER));
}

#[test]
fn applies_max_margin_to_png_output() {
    // Arrange
    let options = QrOptions {
        size: 512,
        margin: 64,
    };

    // Act
    let bytes = generate_qr_png("max margin", &options).expect("text should generate PNG bytes");

    // Assert
    assert!(bytes.starts_with(PNG_HEADER));
}

#[test]
fn applies_custom_margin_to_output_width() {
    // Arrange
    let without_margin = generate_qr_png("custom margin", &QrOptions { size: 1, margin: 0 })
        .expect("text should generate PNG without margin");
    let with_margin = generate_qr_png("custom margin", &QrOptions { size: 1, margin: 8 })
        .expect("text should generate PNG with margin");

    // Assert
    assert!(without_margin.starts_with(PNG_HEADER));
    assert!(with_margin.starts_with(PNG_HEADER));
    assert!(png_width(&with_margin) > png_width(&without_margin));
}

#[test]
fn returns_error_for_zero_size() {
    // Arrange
    let options = QrOptions { size: 0, margin: 4 };

    // Act
    let error =
        generate_qr_png("invalid size", &options).expect_err("zero size should be rejected");

    // Assert
    assert!(matches!(error, QrForgeError::InvalidOptions(_)));
}

#[test]
fn returns_error_for_too_large_size() {
    // Arrange
    let options = QrOptions {
        size: 4097,
        margin: 4,
    };

    // Act
    let error =
        generate_qr_png("invalid size", &options).expect_err("too large size should be rejected");

    // Assert
    assert!(matches!(error, QrForgeError::InvalidOptions(_)));
}

#[test]
fn returns_error_for_too_large_margin() {
    // Arrange
    let options = QrOptions {
        size: 512,
        margin: 65,
    };

    // Act
    let error = generate_qr_png("invalid margin", &options)
        .expect_err("too large margin should be rejected");

    // Assert
    assert!(matches!(error, QrForgeError::InvalidOptions(_)));
}

#[test]
fn returns_error_for_invalid_size_regardless_of_margin() {
    // Arrange
    let options = QrOptions {
        size: 4097,
        margin: 64,
    };

    // Act
    let error = generate_qr_png("invalid options", &options)
        .expect_err("too large size should be rejected");

    // Assert
    assert!(matches!(error, QrForgeError::InvalidOptions(_)));
}

fn png_width(bytes: &[u8]) -> u32 {
    u32::from_be_bytes([bytes[16], bytes[17], bytes[18], bytes[19]])
}
