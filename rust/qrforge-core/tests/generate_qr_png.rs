use qrforge_core::{generate_qr_png, QrGenerationError, QrOptions};

const PNG_HEADER: &[u8; 8] = b"\x89PNG\r\n\x1a\n";
const DARK: u8 = 0;
const LIGHT: u8 = 255;
const VERSION_40_MODULE_WIDTH: u32 = 177;
const VERSION_40_BYTE_CAPACITY_AT_ECC_M: usize = 2_331;

#[test]
fn returns_png_bytes_for_regular_text() -> Result<(), QrGenerationError> {
    let text = "Hello QR";
    let options = QrOptions::default();

    let bytes = generate_qr_png(text, &options)?;

    assert!(bytes.starts_with(PNG_HEADER));

    Ok(())
}

#[test]
fn returns_png_bytes_for_url_text() -> Result<(), QrGenerationError> {
    let text = "https://example.com";
    let options = QrOptions::default();

    let bytes = generate_qr_png(text, &options)?;

    assert!(bytes.starts_with(PNG_HEADER));

    Ok(())
}

#[test]
fn returns_square_png_image() -> Result<(), QrGenerationError> {
    let options = QrOptions::default();

    let bytes = generate_qr_png("square image", &options)?;

    assert_eq!(png_width(&bytes), png_height(&bytes));

    Ok(())
}

#[test]
fn default_options_return_at_least_default_size() -> Result<(), QrGenerationError> {
    let options = QrOptions::default();

    let bytes = generate_qr_png("default size", &options)?;

    assert!(png_width(&bytes) >= 512);

    Ok(())
}

#[test]
fn accepts_version_40_byte_capacity_at_ecc_m() -> Result<(), QrGenerationError> {
    // QrCode::new の既定は ECC-M。小文字は alphanumeric mode に入らないため、
    // 2,331 bytes が Version 40 の byte mode 容量上限になる。
    let text = "a".repeat(VERSION_40_BYTE_CAPACITY_AT_ECC_M);
    let options = QrOptions { size: 1, margin: 0 };

    let bytes = generate_qr_png(&text, &options)?;

    assert_eq!(png_width(&bytes), VERSION_40_MODULE_WIDTH);

    Ok(())
}

#[test]
fn returns_blank_input_for_empty_text() {
    let text = "";
    let options = QrOptions::default();

    let error = generate_qr_png(text, &options).expect_err("empty text should be rejected");

    assert!(
        matches!(error, QrGenerationError::BlankInput),
        "unexpected error: {error}"
    );
}

#[test]
fn returns_blank_input_for_space_only_text() {
    let text = "   ";
    let options = QrOptions::default();

    let error = generate_qr_png(text, &options).expect_err("blank text should be rejected");

    assert!(
        matches!(error, QrGenerationError::BlankInput),
        "unexpected error: {error}"
    );
}

#[test]
fn returns_blank_input_for_whitespace_only_text() {
    let text = "   \t\n";
    let options = QrOptions::default();

    let error = generate_qr_png(text, &options).expect_err("blank text should be rejected");

    assert!(
        matches!(error, QrGenerationError::BlankInput),
        "unexpected error: {error}"
    );
}

#[test]
fn returns_blank_input_for_kotlin_control_whitespace() {
    // Kotlin/JVM の Char.isWhitespace は U+001C..U+001F を blank として扱う。
    let text = "\u{001C}\u{001D}\u{001E}\u{001F}";
    let options = QrOptions::default();

    let error = generate_qr_png(text, &options)
        .expect_err("Kotlin-compatible control whitespace should be rejected");

    assert!(
        matches!(error, QrGenerationError::BlankInput),
        "unexpected error: {error}"
    );
}

#[test]
fn accepts_next_line_control_like_kotlin() -> Result<(), QrGenerationError> {
    // NEXT LINE (U+0085) は Kotlin/JVM の Char.isWhitespace ではない。
    let text = "\u{0085}";
    let options = QrOptions::default();

    let bytes = generate_qr_png(text, &options)?;

    assert!(bytes.starts_with(PNG_HEADER));

    Ok(())
}

#[test]
fn returns_at_least_requested_custom_dimensions() -> Result<(), QrGenerationError> {
    let options = QrOptions {
        size: 256,
        margin: 4,
    };

    let bytes = generate_qr_png("custom size", &options)?;

    assert!(png_width(&bytes) >= 256);
    assert!(png_height(&bytes) >= 256);

    Ok(())
}

#[test]
fn applies_zero_margin_to_png_output() -> Result<(), QrGenerationError> {
    let options = QrOptions { size: 1, margin: 0 };

    let bytes = generate_qr_png("zero margin", &options)?;

    assert!(bytes.starts_with(PNG_HEADER));

    Ok(())
}

#[test]
fn applies_max_margin_to_png_output() -> Result<(), QrGenerationError> {
    let options = QrOptions {
        size: 512,
        margin: 64,
    };

    let bytes = generate_qr_png("max margin", &options)?;

    assert!(bytes.starts_with(PNG_HEADER));

    Ok(())
}

#[test]
fn accepts_max_size_and_max_margin() -> Result<(), QrGenerationError> {
    let options = QrOptions {
        size: 4096,
        margin: 64,
    };

    let bytes = generate_qr_png("max options", &options)?;

    assert!(bytes.starts_with(PNG_HEADER));

    Ok(())
}

#[test]
fn applies_custom_margin_to_output_width() -> Result<(), QrGenerationError> {
    let without_margin = generate_qr_png("custom margin", &QrOptions { size: 1, margin: 0 })?;
    let with_margin = generate_qr_png("custom margin", &QrOptions { size: 1, margin: 8 })?;

    assert!(png_width(&with_margin) > png_width(&without_margin));

    Ok(())
}

#[test]
fn renders_dark_module_at_image_origin_without_margin() -> Result<(), QrGenerationError> {
    // size=1, margin=0 では 1 module = 1 pixel になる。
    let options = QrOptions { size: 1, margin: 0 };

    let image = decode_luma(&generate_qr_png("module origin", &options)?);

    // finder pattern の左上 module は必ず dark。
    assert_eq!(image.get_pixel(0, 0)[0], DARK);

    Ok(())
}

#[test]
fn renders_quiet_zone_at_image_origin_with_margin() -> Result<(), QrGenerationError> {
    let options = QrOptions { size: 1, margin: 2 };

    let image = decode_luma(&generate_qr_png("quiet zone", &options)?);

    assert_eq!(image.get_pixel(0, 0)[0], LIGHT);

    Ok(())
}

#[test]
fn offsets_modules_by_margin() -> Result<(), QrGenerationError> {
    let options = QrOptions { size: 1, margin: 2 };

    let image = decode_luma(&generate_qr_png("margin offset", &options)?);

    // finder pattern の左上 module が margin 分だけずれた位置に来る。
    assert_eq!(image.get_pixel(2, 2)[0], DARK);

    Ok(())
}

#[test]
fn fills_whole_module_area_when_module_size_is_larger_than_one() -> Result<(), QrGenerationError> {
    const MODULE_SIZE: u32 = 3;

    // size=1, margin=0 の出力幅は QR module 数と一致するので、その 3 倍を指定すると
    // 1 module = 3x3 pixel になる
    let qr_width = png_width(&generate_qr_png(
        "module fill",
        &QrOptions { size: 1, margin: 0 },
    )?);
    let options = QrOptions {
        size: qr_width * MODULE_SIZE,
        margin: 0,
    };

    let image = decode_luma(&generate_qr_png("module fill", &options)?);

    // 左上 module の 3x3 全域が塗られている。
    assert!((0..MODULE_SIZE).all(|y| (0..MODULE_SIZE).all(|x| image.get_pixel(x, y)[0] == DARK)));

    Ok(())
}

#[test]
fn returns_invalid_options_for_zero_size() {
    let options = QrOptions { size: 0, margin: 4 };

    let error =
        generate_qr_png("invalid size", &options).expect_err("zero size should be rejected");

    assert!(
        matches!(error, QrGenerationError::InvalidOptions(_)),
        "unexpected error: {error}"
    );
}

#[test]
fn returns_invalid_options_with_allowed_range_for_too_large_size() {
    let options = QrOptions {
        size: 4097,
        margin: 4,
    };

    let error =
        generate_qr_png("invalid size", &options).expect_err("too large size should be rejected");

    assert!(
        matches!(error, QrGenerationError::InvalidOptions(_)),
        "unexpected error: {error}"
    );
    assert_eq!(
        error.to_string(),
        "QR image size must be between 1 and 4096 pixels"
    );
}

#[test]
fn returns_invalid_options_with_allowed_range_for_too_large_margin() {
    let options = QrOptions {
        size: 512,
        margin: 65,
    };

    let error = generate_qr_png("invalid margin", &options)
        .expect_err("too large margin should be rejected");

    assert!(
        matches!(error, QrGenerationError::InvalidOptions(_)),
        "unexpected error: {error}"
    );
    assert_eq!(
        error.to_string(),
        "QR margin must be between 0 and 64 modules"
    );
}

#[test]
fn returns_invalid_options_for_too_large_size_regardless_of_margin() {
    let options = QrOptions {
        size: 4097,
        margin: 64,
    };

    let error = generate_qr_png("invalid options", &options)
        .expect_err("too large size should be rejected");

    assert!(
        matches!(error, QrGenerationError::InvalidOptions(_)),
        "unexpected error: {error}"
    );
}

#[test]
fn returns_qr_encoding_error_above_version_40_byte_capacity_at_ecc_m() {
    // byte mode の Version 40 / ECC-M 上限を 1 byte 超える。
    let text = "a".repeat(VERSION_40_BYTE_CAPACITY_AT_ECC_M + 1);
    let options = QrOptions::default();

    let error = generate_qr_png(&text, &options)
        .expect_err("text above the Version 40 byte capacity should be rejected at ECC-M");

    assert!(
        matches!(error, QrGenerationError::QrEncoding(_)),
        "unexpected error: {error}"
    );
}

#[test]
fn blank_input_display_message_is_stable() {
    let error = QrGenerationError::BlankInput;

    let message = error.to_string();

    assert_eq!(message, "QR text must not be blank");
}

#[test]
fn invalid_options_display_uses_validation_message() {
    let error = QrGenerationError::InvalidOptions("custom validation message".to_string());

    let message = error.to_string();

    assert_eq!(message, "custom validation message");
}

// 検証対象は QR 生成なので、image crate の decode 失敗はテスト基盤の失敗として扱う。
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
