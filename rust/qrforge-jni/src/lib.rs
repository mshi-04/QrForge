use std::panic::{catch_unwind, AssertUnwindSafe};

use jni::objects::{JClass, JString};
use jni::sys::jbyteArray;
use jni::JNIEnv;
use qrforge_core::{generate_qr_png, QrForgeError, QrOptions};

#[no_mangle]
pub extern "system" fn Java_com_appvoyager_qrforge_internal_QrForgeNative_generateQrPng<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    text: JString<'local>,
) -> jbyteArray {
    // env must not be used inside catch_unwind - extract the string first
    let text_str: String = match env.get_string(&text) {
        Ok(s) => s.into(),
        Err(e) => {
            throw_or_fatal(
                &mut env,
                "java/lang/RuntimeException",
                &format!("failed to read text from JNI: {e}"),
            );
            return std::ptr::null_mut();
        }
    };

    // limit catch_unwind scope to Rust core only
    let result = catch_unwind(AssertUnwindSafe(|| {
        generate_qr_png(&text_str, &QrOptions::default())
    }));

    match result {
        Ok(Ok(bytes)) => match env.byte_array_from_slice(&bytes) {
            Ok(arr) => arr.into_raw(),
            Err(e) => {
                throw_or_fatal(
                    &mut env,
                    "java/lang/RuntimeException",
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
                "java/lang/RuntimeException",
                "QR generation failed because native code panicked",
            );
            std::ptr::null_mut()
        }
    }
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
        QrForgeError::QrEncoding(e) => {
            throw_or_fatal(
                env,
                "java/lang/RuntimeException",
                &format!("failed to encode QR data: {e}"),
            );
        }
        QrForgeError::PngEncoding(e) => {
            throw_or_fatal(
                env,
                "java/lang/RuntimeException",
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
