package io.github.lambdarc.qrforge

data class QrOptions(val size: Int = DEFAULT_SIZE, val margin: Int = DEFAULT_MARGIN) {
    init {
        require(size in MIN_SIZE..MAX_SIZE) {
            "QR image size must be between $MIN_SIZE and $MAX_SIZE pixels"
        }
        require(margin in MIN_MARGIN..MAX_MARGIN) {
            "QR margin must be between $MIN_MARGIN and $MAX_MARGIN modules"
        }
    }

    companion object {
        const val DEFAULT_SIZE = 512
        const val DEFAULT_MARGIN = 4
        const val MIN_SIZE = 1
        const val MAX_SIZE = 4096
        const val MIN_MARGIN = 0
        const val MAX_MARGIN = 64
    }
}
