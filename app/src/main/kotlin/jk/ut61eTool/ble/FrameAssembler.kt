package jk.ut61eTool.ble

/**
 * Re-assembles UT61E frames from the byte chunks the HM-11 serial module
 * delivers over BLE notifications.
 *
 * Protocol facts this class depends on:
 *  - a frame is exactly [FRAME_LENGTH] bytes and ends in CR LF;
 *  - every byte carries an odd-parity bit in 0x80, so the terminator has to be
 *    recognised on the masked value (0x0D / 0x0A after `and 0x7F`);
 *  - the 12 payload bytes are ASCII-ish (0x30..0x3F masked), so a masked CR LF
 *    pair can never occur inside a frame — the terminator is a reliable anchor;
 *  - with the default MTU of 23 a notification carries at most 20 bytes, so one
 *    notification usually holds exactly one frame, but frames do straddle
 *    notifications and two frames can share one.
 *
 * Not thread-safe: feed from a single collector.
 */
class FrameAssembler(private val maxBufferSize: Int = MAX_BUFFER_SIZE) {

    private var buffer = ByteArray(0)

    /** Bytes currently held back waiting for a terminator (test/diagnostics). */
    val buffered: Int get() = buffer.size

    /** Appends [chunk] and returns every complete frame it completed, in order. */
    fun feed(chunk: ByteArray): List<ByteArray> {
        if (chunk.isEmpty()) return emptyList()
        buffer += chunk

        val frames = mutableListOf<ByteArray>()
        var consumed = 0
        var i = 0
        while (i + 1 < buffer.size) {
            if (mask(buffer[i]) == CR && mask(buffer[i + 1]) == LF) {
                val end = i + 2
                // Anything in front of a full frame is garbage from a mid-stream
                // subscribe or a dropped notification: drop it and re-sync.
                if (end - consumed >= FRAME_LENGTH) {
                    frames += buffer.copyOfRange(end - FRAME_LENGTH, end)
                }
                consumed = end
                i = end
            } else {
                i++
            }
        }

        buffer = buffer.copyOfRange(consumed, buffer.size)
        if (buffer.size > maxBufferSize) {
            // Garbage flood without any terminator; keep only the newest bytes.
            buffer = buffer.copyOfRange(buffer.size - maxBufferSize, buffer.size)
        }
        return frames
    }

    /** Drops partial data, e.g. when a new connection starts. */
    fun reset() {
        buffer = ByteArray(0)
    }

    private fun mask(b: Byte): Int = b.toInt() and 0x7F

    companion object {
        const val FRAME_LENGTH = 14
        const val MAX_BUFFER_SIZE = 64

        private const val CR = 0x0D
        private const val LF = 0x0A
    }
}
