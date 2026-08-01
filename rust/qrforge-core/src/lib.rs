#![deny(clippy::unwrap_used, clippy::expect_used)]

mod error;

use png::{BitDepth, ColorType, Encoder};
use qrcode::types::Color;
use qrcode::QrCode;

pub use error::QrGenerationError;

// QrOptions の値域はこの crate を正典とし、Kotlin 側 `QrOptions` の companion 定数と
// 同じ値を保つ。変更時は docs/api-design.md の「定数同期」表も合わせて更新する。
const DEFAULT_IMAGE_SIZE: u32 = 512;
const DEFAULT_MARGIN: u32 = 4;
const MIN_IMAGE_SIZE: u32 = 1;
const MAX_IMAGE_SIZE: u32 = 4096;
const MIN_MARGIN: u32 = 0;
const MAX_MARGIN: u32 = 64;
const DARK_PIXEL: u8 = 0;
const LIGHT_PIXEL: u8 = 255;

struct RenderedQrImage {
    side_length: u32,
    pixels: Vec<u8>,
}

/// Options controlling the generated QR image.
///
/// See [`generate_qr_png`] for a complete usage example.
pub struct QrOptions {
    /// Output image size in pixels (applied to both width and height; actual output may be larger
    /// due to QR module boundary rounding).
    pub size: u32,
    /// Quiet zone margin in QR modules. Set to 0 to disable.
    pub margin: u32,
}

impl Default for QrOptions {
    fn default() -> Self {
        Self {
            size: DEFAULT_IMAGE_SIZE,
            margin: DEFAULT_MARGIN,
        }
    }
}

/// Generates a QR code as PNG bytes.
///
/// # Examples
///
/// ```
/// use qrforge_core::{generate_qr_png, QrOptions};
/// # use qrforge_core::QrGenerationError;
///
/// # fn main() -> Result<(), QrGenerationError> {
/// let options = QrOptions {
///     size: 256,
///     margin: 4,
/// };
/// let png: Vec<u8> = generate_qr_png("https://example.com", &options)?;
///
/// assert!(!png.is_empty());
/// # Ok(())
/// # }
/// ```
///
/// # Errors
///
/// Returns [`QrGenerationError`] when the input is blank, the options are outside their allowed
/// ranges, or QR/PNG encoding fails.
pub fn generate_qr_png(text: &str, options: &QrOptions) -> Result<Vec<u8>, QrGenerationError> {
    if text.chars().all(is_contract_whitespace) {
        return Err(QrGenerationError::BlankInput);
    }
    validate_options(options)?;

    let code = QrCode::new(text.as_bytes())?;
    let image = render_qr_image(&code, options);
    encode_png(&image).map_err(QrGenerationError::from)
}

fn is_contract_whitespace(character: char) -> bool {
    // Kotlin/JVM の Char.isWhitespace と同じ契約にする。Rust の Unicode White_Space との差は、
    // U+001C..U+001F を追加し、NEXT LINE (U+0085) を除外する点。
    matches!(character, '\u{001C}'..='\u{001F}')
        || (character.is_whitespace() && character != '\u{0085}')
}

fn validate_options(options: &QrOptions) -> Result<(), QrGenerationError> {
    if options.size < MIN_IMAGE_SIZE || options.size > MAX_IMAGE_SIZE {
        return Err(QrGenerationError::InvalidOptions(format!(
            "QR image size must be between {MIN_IMAGE_SIZE} and {MAX_IMAGE_SIZE} pixels"
        )));
    }

    if options.margin > MAX_MARGIN {
        return Err(QrGenerationError::InvalidOptions(format!(
            "QR margin must be between {MIN_MARGIN} and {MAX_MARGIN} modules"
        )));
    }

    Ok(())
}

fn render_qr_image(code: &QrCode, options: &QrOptions) -> RenderedQrImage {
    // QR version 40 (max) の幅は 177 modules — u32 に確実に収まる
    let qr_width = code.width() as u32;
    let total_modules = qr_width + options.margin * 2;
    let module_size = options.size.div_ceil(total_modules);
    let image_size = total_modules * module_size;
    let image_width = image_size as usize;
    let mut pixels = vec![LIGHT_PIXEL; image_width * image_width];

    for y in 0..qr_width {
        for x in 0..qr_width {
            if code[(x as usize, y as usize)] != Color::Light {
                draw_module(&mut pixels, image_width, x, y, options.margin, module_size);
            }
        }
    }

    RenderedQrImage {
        side_length: image_size,
        pixels,
    }
}

fn draw_module(
    pixels: &mut [u8],
    image_width: usize,
    module_x: u32,
    module_y: u32,
    margin: u32,
    module_size: u32,
) {
    // Grayscale 8-bit は 1 ピクセル 1 バイトなので、raw buffer の行スライスをまとめて塗る。
    // module rounding を含む最大出力 (4368x4368) でもピクセル単位の処理を避けられる。
    let start_x = ((module_x + margin) * module_size) as usize;
    let start_y = ((module_y + margin) * module_size) as usize;
    let module_size = module_size as usize;

    for y in start_y..start_y + module_size {
        let row_start = y * image_width + start_x;
        pixels[row_start..row_start + module_size].fill(DARK_PIXEL);
    }
}

fn encode_png(image: &RenderedQrImage) -> Result<Vec<u8>, png::EncodingError> {
    let mut bytes = Vec::new();
    let mut encoder = Encoder::new(&mut bytes, image.side_length, image.side_length);
    encoder.set_color(ColorType::Grayscale);
    encoder.set_depth(BitDepth::Eight);

    let mut writer = encoder.write_header()?;
    writer.write_image_data(&image.pixels)?;
    writer.finish()?;

    Ok(bytes)
}
