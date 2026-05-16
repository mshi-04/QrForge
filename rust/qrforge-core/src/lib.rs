#![deny(clippy::unwrap_used, clippy::expect_used)]

mod error;

use image::codecs::png::PngEncoder;
use image::{ImageEncoder, Luma};
use qrcode::QrCode;

pub use error::QrForgeError;

const DEFAULT_IMAGE_SIZE: u32 = 512;

pub struct QrOptions {
    /// Output image size in pixels (applied to both width and height; actual output may be larger
    /// due to QR module boundary rounding).
    pub size: u32,
    /// Quiet zone margin in QR modules. Set to 0 to disable. Exact module count is not
    /// guaranteed by the underlying renderer; any value > 0 enables the standard quiet zone.
    pub margin: u32,
}

impl Default for QrOptions {
    fn default() -> Self {
        Self {
            size: DEFAULT_IMAGE_SIZE,
            margin: 4,
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
        .quiet_zone(options.margin > 0)
        .min_dimensions(options.size, options.size)
        .build();

    let mut bytes = Vec::new();
    let (width, height) = (image.width(), image.height());
    PngEncoder::new(&mut bytes).write_image(
        image.into_raw().as_slice(),
        width,
        height,
        image::ExtendedColorType::L8,
    )?;

    Ok(bytes)
}
