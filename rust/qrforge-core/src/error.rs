use std::error::Error;
use std::fmt::{self, Display, Formatter};

/// Errors returned by [`crate::generate_qr_png`].
#[derive(Debug)]
pub enum QrGenerationError {
    BlankInput,
    /// Option validation failure. 範囲を含む文言を組み立てるため String を持つ。
    InvalidOptions(String),
    QrEncoding(qrcode::types::QrError),
    PngEncoding(png::EncodingError),
}

impl Display for QrGenerationError {
    fn fmt(&self, formatter: &mut Formatter<'_>) -> fmt::Result {
        match self {
            Self::BlankInput => formatter.write_str("QR text must not be blank"),
            Self::InvalidOptions(message) => formatter.write_str(message),
            Self::QrEncoding(error) => write!(formatter, "failed to encode QR data: {error}"),
            Self::PngEncoding(error) => write!(formatter, "failed to encode PNG data: {error}"),
        }
    }
}

impl Error for QrGenerationError {
    fn source(&self) -> Option<&(dyn Error + 'static)> {
        match self {
            Self::BlankInput => None,
            Self::InvalidOptions(_) => None,
            Self::QrEncoding(error) => Some(error),
            Self::PngEncoding(error) => Some(error),
        }
    }
}

impl From<qrcode::types::QrError> for QrGenerationError {
    fn from(error: qrcode::types::QrError) -> Self {
        Self::QrEncoding(error)
    }
}

impl From<png::EncodingError> for QrGenerationError {
    fn from(error: png::EncodingError) -> Self {
        Self::PngEncoding(error)
    }
}
