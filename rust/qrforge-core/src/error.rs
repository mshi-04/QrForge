use std::error::Error;
use std::fmt::{self, Display, Formatter};

#[derive(Debug)]
pub enum QrForgeError {
    BlankInput,
    InvalidOptions(&'static str),
    QrEncoding(qrcode::types::QrError),
    PngEncoding(image::ImageError),
}

impl Display for QrForgeError {
    fn fmt(&self, formatter: &mut Formatter<'_>) -> fmt::Result {
        match self {
            Self::BlankInput => formatter.write_str("QR text must not be blank"),
            Self::InvalidOptions(message) => formatter.write_str(message),
            Self::QrEncoding(error) => write!(formatter, "failed to encode QR data: {error}"),
            Self::PngEncoding(error) => write!(formatter, "failed to encode PNG data: {error}"),
        }
    }
}

impl Error for QrForgeError {
    fn source(&self) -> Option<&(dyn Error + 'static)> {
        match self {
            Self::BlankInput => None,
            Self::InvalidOptions(_) => None,
            Self::QrEncoding(error) => Some(error),
            Self::PngEncoding(error) => Some(error),
        }
    }
}

impl From<qrcode::types::QrError> for QrForgeError {
    fn from(error: qrcode::types::QrError) -> Self {
        Self::QrEncoding(error)
    }
}

impl From<image::ImageError> for QrForgeError {
    fn from(error: image::ImageError) -> Self {
        Self::PngEncoding(error)
    }
}
