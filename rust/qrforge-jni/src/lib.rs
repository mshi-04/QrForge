#![deny(clippy::unwrap_used, clippy::expect_used)]

use std::panic::{catch_unwind, AssertUnwindSafe};

use jni::objects::{JClass, JString};
use jni::sys::{jbyteArray, jint};
use jni::JNIEnv;
use qrforge_core::{generate_qr_png, QrForgeError, QrOptions};

const GENERATION_FAILED_CLASS: &str =
    "com/appvoyager/qrforge/internal/QrForgeNative$GenerationFailed";

#[no_mangle]
pub extern "system" fn Java_com_appvoyager_qrforge_internal_QrForgeNative_nativeGenerateQrPng<
    'local,
>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    text: JString<'local>,
    size: jint,
    margin: jint,
) -> jbyteArray {
    let text_str: String = match env.get_string(&text) {
        Ok(s) => s.into(),
        Err(e) => {
            throw_or_fatal(
                &mut env,
                GENERATION_FAILED_CLASS,
                &format!("failed to read text from JNI: {e}"),
            );
            return std::ptr::null_mut();
        }
    };
    let options = match options_from_jni(size, margin) {
        Ok(options) => options,
        Err(message) => {
            throw_or_fatal(&mut env, "java/lang/IllegalArgumentException", message);
            return std::ptr::null_mut();
        }
    };

    let result = catch_unwind(AssertUnwindSafe(|| generate_qr_png(&text_str, &options)));

    match result {
        Ok(Ok(bytes)) => match env.byte_array_from_slice(&bytes) {
            Ok(arr) => arr.into_raw(),
            Err(e) => {
                throw_or_fatal(
                    &mut env,
                    GENERATION_FAILED_CLASS,
                    &format!("failed to create JNI byte array: {e}"),
                );
                std::ptr::null_mut()
            }
        },
        Ok(Err(error)) => {
            throw_qr_error(&mut env, error);
            std::ptr::null_mut()
        }
        Err(_) => {
            throw_or_fatal(
                &mut env,
                GENERATION_FAILED_CLASS,
                "QR generation failed because native code panicked",
            );
            std::ptr::null_mut()
        }
    }
}

fn options_from_jni(size: jint, margin: jint) -> Result<QrOptions, &'static str> {
    if size < 1 || size > 4096 {
        return Err("QR image size must be between 1 and 4096 pixels");
    }

    if margin < 0 || margin > 64 {
        return Err("QR margin must be between 0 and 64 modules");
    }

    Ok(QrOptions {
        size: size as u32,
        margin: margin as u32,
    })
}

fn throw_qr_error(env: &mut JNIEnv<'_>, error: QrForgeError) {
    match error {
        QrForgeError::BlankInput => {
            throw_or_fatal(
                env,
                "java/lang/IllegalArgumentException",
                "QR text must not be blank",
            );
        }
        QrForgeError::InvalidOptions(message) => {
            throw_or_fatal(env, "java/lang/IllegalArgumentException", message);
        }
        QrForgeError::QrEncoding(e) => {
            throw_or_fatal(
                env,
                GENERATION_FAILED_CLASS,
                &format!("failed to encode QR data: {e}"),
            );
        }
        QrForgeError::PngEncoding(e) => {
            throw_or_fatal(
                env,
                GENERATION_FAILED_CLASS,
                &format!("failed to encode PNG data: {e}"),
            );
        }
    }
}

fn throw_or_fatal(env: &mut JNIEnv<'_>, class: &str, message: &str) {
    if env.throw_new(class, message).is_err() {
        env.fatal_error(message);
    }
}
