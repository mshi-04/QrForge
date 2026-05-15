mod error;

use std::io::Cursor;

use image::{DynamicImage, ImageFormat, Luma};
use qrcode::QrCode;

pub use error::QrForgeError;

const DEFAULT_IMAGE_SIZE: u32 = 512;

pub struct QrOptions {
    pub width: u32,
    pub height: u32,
}

impl Default for QrOptions {
    fn default() -> Self {
        Self {
            width: DEFAULT_IMAGE_SIZE,
            height: DEFAULT_IMAGE_SIZE,
        }
    }
}

pub fn generate_qr_png(text: &str, options: &QrOptions) -> Result<Vec<u8>, QrForgeError> {
    if text.trim().is_empty() {
        return Err(QrForgeError::BlankInput);
    }

    let code = QrCode::new(text.as_bytes())?;
    let image = code
        .render::<Luma<u8>>()
        .quiet_zone(true)
        .min_dimensions(options.width, options.height)
        .build();

    let mut bytes = Vec::new();
    DynamicImage::ImageLuma8(image).write_to(&mut Cursor::new(&mut bytes), ImageFormat::Png)?;

    Ok(bytes)
}
