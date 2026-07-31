package com.appvoyager.qrforge

/**
 * Instrumented Test 間で共有する定数。
 *
 * assert は置かない (テスト側に残す)。
 */
internal object QrForgeTestFixtures {
    const val CUSTOM_SIZE = 768
    const val CUSTOM_MARGIN = 6

    val PNG_HEADER = byteArrayOf(
        0x89.toByte(),
        0x50,
        0x4E,
        0x47,
        0x0D,
        0x0A,
        0x1A,
        0x0A,
    )
}
