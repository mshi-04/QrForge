#![deny(clippy::unwrap_used, clippy::expect_used)]

mod error;

use image::codecs::png::PngEncoder;
use image::{ImageBuffer, ImageEncoder, Luma};
use qrcode::types::Color;
use qrcode::QrCode;

pub use error::QrForgeError;

// QrOptions の値域はこの crate を正典とし、Kotlin 側 `QrOptions` の companion 定数と
// 同じ値を保つ。変更時は docs/api-design.md の「定数同期」表も合わせて更新する。
const DEFAULT_IMAGE_SIZE: u32 = 512;
const DEFAULT_MARGIN: u32 = 4;
const MIN_IMAGE_SIZE: u32 = 1;
const MAX_IMAGE_SIZE: u32 = 4096;
const MIN_MARGIN: u32 = 0;
const MAX_MARGIN: u32 = 64;

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

pub fn generate_qr_png(text: &str, options: &QrOptions) -> Result<Vec<u8>, QrForgeError> {
    if text.trim().is_empty() {
        return Err(QrForgeError::BlankInput);
    }
    validate_options(options)?;

    let code = QrCode::new(text.as_bytes())?;
    let image = render_qr_image(&code, options);

    let mut bytes = Vec::new();
    let (width, height) = (image.width(), image.height());
    let raw = image.into_raw();
    PngEncoder::new(&mut bytes).write_image(&raw, width, height, image::ExtendedColorType::L8)?;

    Ok(bytes)
}

fn validate_options(options: &QrOptions) -> Result<(), QrForgeError> {
    if options.size < MIN_IMAGE_SIZE || options.size > MAX_IMAGE_SIZE {
        return Err(QrForgeError::InvalidOptions(format!(
            "QR image size must be between {MIN_IMAGE_SIZE} and {MAX_IMAGE_SIZE} pixels"
        )));
    }

    if options.margin > MAX_MARGIN {
        return Err(QrForgeError::InvalidOptions(format!(
            "QR margin must be between {MIN_MARGIN} and {MAX_MARGIN} modules"
        )));
    }

    Ok(())
}

fn render_qr_image(code: &QrCode, options: &QrOptions) -> ImageBuffer<Luma<u8>, Vec<u8>> {
    // QR version 40 (max) の幅は 177 modules — u32 に確実に収まる
    let qr_width = code.width() as u32;
    let total_modules = qr_width + options.margin * 2;
    let module_size = options.size.div_ceil(total_modules);
    let image_size = total_modules * module_size;
    let mut image = ImageBuffer::from_pixel(image_size, image_size, Luma([255]));

    for y in 0..qr_width {
        for x in 0..qr_width {
            if code[(x as usize, y as usize)] != Color::Light {
                draw_module(&mut image, x, y, options.margin, module_size);
            }
        }
    }

    image
}

fn draw_module(
    image: &mut ImageBuffer<Luma<u8>, Vec<u8>>,
    module_x: u32,
    module_y: u32,
    margin: u32,
    module_size: u32,
) {
    // Luma<u8> は 1 ピクセル 1 バイトなので、raw buffer の行スライスをまとめて塗れる。
    // put_pixel をピクセル単位で呼ぶより、最大構成 (約 4270x4270) で大きく速い。
    let image_width = image.width() as usize;
    let start_x = ((module_x + margin) * module_size) as usize;
    let start_y = ((module_y + margin) * module_size) as usize;
    let module_size = module_size as usize;
    let raw: &mut [u8] = image;

    for y in start_y..start_y + module_size {
        let row_start = y * image_width + start_x;
        raw[row_start..row_start + module_size].fill(0);
    }
}
