package io.github.lambdarc.qrforge

/**
 * Size and quiet zone of the generated image.
 *
 * @property size Lower bound for the length of one side, in pixels. The image is drawn in whole QR
 *   modules, so the actual side is rounded **up** to the next module boundary and is therefore
 *   greater than or equal to this value — rarely equal to it. A 21-module code with the defaults
 *   comes out at 522x522, not 512x512. Must be in [MIN_SIZE]..[MAX_SIZE].
 * @property margin Width of the quiet zone around the code, counted in QR modules rather than
 *   pixels. `0` omits it. Must be in [MIN_MARGIN]..[MAX_MARGIN].
 * @throws IllegalArgumentException if [size] or [margin] is outside its range.
 */
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
        /** Default for [size]. */
        const val DEFAULT_SIZE = 512

        /** Default for [margin]. */
        const val DEFAULT_MARGIN = 4

        /** Smallest accepted [size]. */
        const val MIN_SIZE = 1

        /** Largest accepted [size]. At this size a bitmap can reach roughly 73 MiB. */
        const val MAX_SIZE = 4096

        /** Smallest accepted [margin], which omits the quiet zone. */
        const val MIN_MARGIN = 0

        /** Largest accepted [margin]. */
        const val MAX_MARGIN = 64
    }
}
