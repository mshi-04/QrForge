#![deny(clippy::unwrap_used, clippy::expect_used)]

use std::panic::catch_unwind;

use jni::objects::{JClass, JString};
use jni::sys::{jbyteArray, jint};
use jni::JNIEnv;
use qrforge_core::{generate_qr_png, QrGenerationError, QrOptions};

const GENERATION_FAILED_CLASS: &str =
    "com/appvoyager/qrforge/internal/NativeQrGenerator$GenerationFailed";
const ILLEGAL_ARGUMENT_EXCEPTION_CLASS: &str = "java/lang/IllegalArgumentException";

#[no_mangle]
pub extern "system" fn Java_com_appvoyager_qrforge_internal_NativeQrGenerator_nativeGenerateQrPng<
    'local,
>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    text: JString<'local>,
    size: jint,
    margin: jint,
) -> jbyteArray {
    // JavaStr からの借用ゼロコピーも可能だが、後段の env 可変借用と競合するため
    // 所有権を持つ String に変換する。入力サイズは小さく性能影響は無視できる。
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
            throw_or_fatal(&mut env, ILLEGAL_ARGUMENT_EXCEPTION_CLASS, message);
            return std::ptr::null_mut();
        }
    };

    let result = catch_unwind(|| generate_qr_png(&text_str, &options));

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
        Err(panic_info) => {
            let detail = panic_info
                .downcast_ref::<&str>()
                .map(|s| s.to_string())
                .or_else(|| panic_info.downcast_ref::<String>().cloned())
                .unwrap_or_else(|| "unknown cause".to_string());
            throw_or_fatal(
                &mut env,
                GENERATION_FAILED_CLASS,
                &format!("QR generation panicked: {detail}"),
            );
            std::ptr::null_mut()
        }
    }
}

fn options_from_jni(size: jint, margin: jint) -> Result<QrOptions, &'static str> {
    // JNI 境界では u32 への変換安全性のみを保証する。
    // 値域の妥当性は qrforge_core::validate_options が一元的に検証する。
    if size < 0 {
        return Err("QR image size must not be negative");
    }

    if margin < 0 {
        return Err("QR margin must not be negative");
    }

    Ok(QrOptions {
        size: size as u32,
        margin: margin as u32,
    })
}

fn throw_qr_error(env: &mut JNIEnv<'_>, error: QrGenerationError) {
    let class = match &error {
        QrGenerationError::BlankInput | QrGenerationError::InvalidOptions(_) => {
            ILLEGAL_ARGUMENT_EXCEPTION_CLASS
        }
        QrGenerationError::QrEncoding(_) | QrGenerationError::PngEncoding(_) => {
            GENERATION_FAILED_CLASS
        }
    };
    throw_or_fatal(env, class, &error.to_string());
}

fn throw_or_fatal(env: &mut JNIEnv<'_>, class: &str, message: &str) {
    // JNI 呼び出しの失敗は Java 例外が pending 状態のことがある。その場合は元の例外を
    // そのまま伝播させ、こちらの例外で上書きしない。
    if matches!(env.exception_check(), Ok(true)) {
        return;
    }

    if env.throw_new(class, message).is_err() {
        env.fatal_error(message);
    }
}
