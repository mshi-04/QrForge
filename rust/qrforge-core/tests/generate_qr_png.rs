use qrforge_core::{generate_qr_png, QrForgeError, QrOptions};

const PNG_HEADER: &[u8; 8] = b"\x89PNG\r\n\x1a\n";
const DARK: u8 = 0;
const LIGHT: u8 = 255;

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
fn returns_square_png_image() {
    // Arrange
    let options = QrOptions::default();

    // Act
    let bytes = generate_qr_png("square image", &options).expect("text should generate PNG bytes");

    // Assert
    assert_eq!(png_width(&bytes), png_height(&bytes));
}

#[test]
fn default_options_return_at_least_default_size() {
    // Arrange
    let options = QrOptions::default();

    // Act
    let bytes = generate_qr_png("default size", &options).expect("text should generate PNG bytes");

    // Assert
    assert!(png_width(&bytes) >= 512);
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
fn accepts_max_size_and_max_margin() {
    // Arrange
    let options = QrOptions {
        size: 4096,
        margin: 64,
    };

    // Act
    let bytes = generate_qr_png("max options", &options)
        .expect("text should generate PNG bytes with max options");

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
fn renders_dark_module_at_image_origin_without_margin() {
    // Arrange: size=1, margin=0 では 1 module = 1 pixel になる
    let options = QrOptions { size: 1, margin: 0 };

    // Act
    let image = decode_luma(
        &generate_qr_png("module origin", &options).expect("text should generate PNG bytes"),
    );

    // Assert: finder pattern の左上 module は必ず dark
    assert_eq!(image.get_pixel(0, 0)[0], DARK);
}

#[test]
fn renders_quiet_zone_at_image_origin_with_margin() {
    // Arrange
    let options = QrOptions { size: 1, margin: 2 };

    // Act
    let image = decode_luma(
        &generate_qr_png("quiet zone", &options).expect("text should generate PNG bytes"),
    );

    // Assert
    assert_eq!(image.get_pixel(0, 0)[0], LIGHT);
}

#[test]
fn offsets_modules_by_margin() {
    // Arrange
    let options = QrOptions { size: 1, margin: 2 };

    // Act
    let image = decode_luma(
        &generate_qr_png("margin offset", &options).expect("text should generate PNG bytes"),
    );

    // Assert: finder pattern の左上 module が margin 分だけずれた位置に来る
    assert_eq!(image.get_pixel(2, 2)[0], DARK);
}

#[test]
fn fills_whole_module_area_when_module_size_is_larger_than_one() {
    // Arrange: size=1, margin=0 の出力幅は QR module 数と一致するので、その 3 倍を指定すると
    // 1 module = 3x3 pixel になる
    let qr_width = png_width(
        &generate_qr_png("module fill", &QrOptions { size: 1, margin: 0 })
            .expect("text should generate PNG bytes"),
    );
    let options = QrOptions {
        size: qr_width * 3,
        margin: 0,
    };

    // Act
    let image = decode_luma(
        &generate_qr_png("module fill", &options).expect("text should generate PNG bytes"),
    );

    // Assert: 左上 module の 3x3 全域が塗られている
    assert_eq!(image.get_pixel(2, 2)[0], DARK);
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
fn invalid_size_error_reports_allowed_size_range() {
    // Arrange
    let options = QrOptions {
        size: 4097,
        margin: 4,
    };

    // Act
    let error =
        generate_qr_png("invalid size", &options).expect_err("too large size should be rejected");

    // Assert
    assert_eq!(
        error.to_string(),
        "QR image size must be between 1 and 4096 pixels"
    );
}

#[test]
fn invalid_margin_error_reports_allowed_margin_range() {
    // Arrange
    let options = QrOptions {
        size: 512,
        margin: 65,
    };

    // Act
    let error = generate_qr_png("invalid margin", &options)
        .expect_err("too large margin should be rejected");

    // Assert
    assert_eq!(
        error.to_string(),
        "QR margin must be between 0 and 64 modules"
    );
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

#[test]
fn returns_qr_encoding_error_for_oversized_text() {
    // Arrange
    let text = "a".repeat(10_000);
    let options = QrOptions::default();

    // Act
    let error = generate_qr_png(&text, &options).expect_err("oversized text should be rejected");

    // Assert
    assert!(matches!(error, QrForgeError::QrEncoding(_)));
}

#[test]
fn blank_input_display_message_is_stable() {
    // Arrange
    let error = QrForgeError::BlankInput;

    // Act
    let message = error.to_string();

    // Assert
    assert_eq!(message, "QR text must not be blank");
}

#[test]
fn invalid_options_display_uses_validation_message() {
    // Arrange
    let error = QrForgeError::InvalidOptions("custom validation message".to_string());

    // Act
    let message = error.to_string();

    // Assert
    assert_eq!(message, "custom validation message");
}

fn decode_luma(bytes: &[u8]) -> image::GrayImage {
    image::load_from_memory(bytes)
        .expect("generated PNG should be decodable")
        .to_luma8()
}

fn png_width(bytes: &[u8]) -> u32 {
    u32::from_be_bytes([bytes[16], bytes[17], bytes[18], bytes[19]])
}

fn png_height(bytes: &[u8]) -> u32 {
    u32::from_be_bytes([bytes[20], bytes[21], bytes[22], bytes[23]])
}
