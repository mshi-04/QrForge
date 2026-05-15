package com.appvoyager.qrforge.internal

internal object QrForgeNative {
    init {
        System.loadLibrary("qrforge")
    }

    @JvmStatic
    external fun generateQrPng(text: String): ByteArray
}
