use qrforge_core::{generate_qr_png, QrGenerationError, QrOptions};

const PNG_HEADER: &[u8; 8] = b"\x89PNG\r\n\x1a\n";
const DARK: u8 = 0;
const LIGHT: u8 = 255;
const VERSION_40_MODULE_WIDTH: u32 = 177;
const VERSION_40_BYTE_CAPACITY_AT_ECC_M: usize = 2_331;

#[test]
fn returns_png_bytes_for_regular_text() {
    // Arrange
    let text = "Hello QR";
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
fn accepts_version_40_byte_capacity_at_ecc_m() {
    // Arrange: QrCode::new の既定は ECC-M。小文字は alphanumeric mode に入らないため、
    // 2,331 bytes が Version 40 の byte mode 容量上限になる。
    let text = "a".repeat(VERSION_40_BYTE_CAPACITY_AT_ECC_M);
    let options = QrOptions { size: 1, margin: 0 };

    // Act
    let bytes = generate_qr_png(&text, &options)
        .expect("Version 40 byte capacity should generate PNG bytes at ECC-M");

    // Assert
    assert_eq!(png_width(&bytes), VERSION_40_MODULE_WIDTH);
}

#[test]
fn returns_error_for_empty_text() {
    // Arrange
    let text = "";
    let options = QrOptions::default();

    // Act
    let error = generate_qr_png(text, &options).expect_err("empty text should be rejected");

    // Assert
    assert!(matches!(error, QrGenerationError::BlankInput));
}

#[test]
fn returns_error_for_blank_text() {
    // Arrange
    let text = "   ";
    let options = QrOptions::default();

    // Act
    let error = generate_qr_png(text, &options).expect_err("blank text should be rejected");

    // Assert
    assert!(matches!(error, QrGenerationError::BlankInput));
}

#[test]
fn returns_error_for_whitespace_only_text() {
    // Arrange
    let text = "   \t\n";
    let options = QrOptions::default();

    // Act
    let error = generate_qr_png(text, &options).expect_err("blank text should be rejected");

    // Assert
    assert!(matches!(error, QrGenerationError::BlankInput));
}

#[test]
fn returns_error_for_kotlin_control_whitespace() {
    // Arrange: Kotlin/JVM の Char.isWhitespace は U+001C..U+001F を blank として扱う。
    let text = "\u{001C}\u{001D}\u{001E}\u{001F}";
    let options = QrOptions::default();

    // Act
    let error = generate_qr_png(text, &options)
        .expect_err("Kotlin-compatible control whitespace should be rejected");

    // Assert
    assert!(matches!(error, QrGenerationError::BlankInput));
}

#[test]
fn accepts_next_line_control_like_kotlin() {
    // Arrange: NEXT LINE (U+0085) は Kotlin/JVM の Char.isWhitespace ではない。
    let text = "\u{0085}";
    let options = QrOptions::default();

    // Act
    let bytes = generate_qr_png(text, &options)
        .expect("NEXT LINE control should be accepted as non-blank text");

    // Assert
    assert!(bytes.starts_with(PNG_HEADER));
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
    const MODULE_SIZE: u32 = 3;

    // Arrange: size=1, margin=0 の出力幅は QR module 数と一致するので、その 3 倍を指定すると
    // 1 module = 3x3 pixel になる
    let qr_width = png_width(
        &generate_qr_png("module fill", &QrOptions { size: 1, margin: 0 })
            .expect("text should generate PNG bytes"),
    );
    let options = QrOptions {
        size: qr_width * MODULE_SIZE,
        margin: 0,
    };

    // Act
    let image = decode_luma(
        &generate_qr_png("module fill", &options).expect("text should generate PNG bytes"),
    );

    // Assert: 左上 module の 3x3 全域が塗られている
    assert!((0..MODULE_SIZE).all(|y| (0..MODULE_SIZE).all(|x| image.get_pixel(x, y)[0] == DARK)));
}

#[test]
fn returns_error_for_zero_size() {
    // Arrange
    let options = QrOptions { size: 0, margin: 4 };

    // Act
    let error =
        generate_qr_png("invalid size", &options).expect_err("zero size should be rejected");

    // Assert
    assert!(matches!(error, QrGenerationError::InvalidOptions(_)));
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
    assert!(matches!(error, QrGenerationError::InvalidOptions(_)));
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
    assert!(matches!(error, QrGenerationError::InvalidOptions(_)));
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
    assert!(matches!(error, QrGenerationError::InvalidOptions(_)));
}

#[test]
fn returns_qr_encoding_error_above_version_40_byte_capacity_at_ecc_m() {
    // Arrange: byte mode の Version 40 / ECC-M 上限を 1 byte 超える
    let text = "a".repeat(VERSION_40_BYTE_CAPACITY_AT_ECC_M + 1);
    let options = QrOptions::default();

    // Act
    let error = generate_qr_png(&text, &options)
        .expect_err("text above the Version 40 byte capacity should be rejected at ECC-M");

    // Assert
    assert!(matches!(error, QrGenerationError::QrEncoding(_)));
}

#[test]
fn blank_input_display_message_is_stable() {
    // Arrange
    let error = QrGenerationError::BlankInput;

    // Act
    let message = error.to_string();

    // Assert
    assert_eq!(message, "QR text must not be blank");
}

#[test]
fn invalid_options_display_uses_validation_message() {
    // Arrange
    let error = QrGenerationError::InvalidOptions("custom validation message".to_string());

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
