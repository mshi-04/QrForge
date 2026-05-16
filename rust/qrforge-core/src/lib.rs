#![deny(clippy::unwrap_used, clippy::expect_used)]

mod error;

use image::codecs::png::PngEncoder;
use image::{ImageBuffer, ImageEncoder, Luma};
use qrcode::types::Color;
use qrcode::QrCode;

pub use error::QrForgeError;

const DEFAULT_IMAGE_SIZE: u32 = 512;
const DEFAULT_MARGIN: u32 = 4;
const MIN_IMAGE_SIZE: u32 = 1;
const MAX_IMAGE_SIZE: u32 = 4096;
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
        return Err(QrForgeError::InvalidOptions(
            "QR image size must be between 1 and 4096 pixels",
        ));
    }

    if options.margin > MAX_MARGIN {
        return Err(QrForgeError::InvalidOptions(
            "QR margin must be between 0 and 64 modules",
        ));
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
    let start_x = (module_x + margin) * module_size;
    let start_y = (module_y + margin) * module_size;

    for y in start_y..start_y + module_size {
        for x in start_x..start_x + module_size {
            image.put_pixel(x, y, Luma([0]));
        }
    }
}
