mod error;

use std::io::Cursor;

use image::{DynamicImage, ImageFormat, Luma};
use qrcode::QrCode;

pub use error::QrError;

const DEFAULT_IMAGE_SIZE: u32 = 512;

pub fn generate_qr_png(text: &str) -> Result<Vec<u8>, QrError> {
    if text.trim().is_empty() {
        return Err(QrError::BlankInput);
    }

    let code = QrCode::new(text.as_bytes())?;
    let image = code
        .render::<Luma<u8>>()
        .quiet_zone(true)
        .min_dimensions(DEFAULT_IMAGE_SIZE, DEFAULT_IMAGE_SIZE)
        .build();

    let mut bytes = Vec::new();
    DynamicImage::ImageLuma8(image).write_to(&mut Cursor::new(&mut bytes), ImageFormat::Png)?;

    Ok(bytes)
}
