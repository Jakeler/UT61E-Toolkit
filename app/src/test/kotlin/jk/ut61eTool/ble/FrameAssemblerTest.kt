package jk.ut61eTool.ble

import com.jake.ut61e.Ut61eFrameDecoder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameAssemblerTest {

    /** 225.8 mV DC, overloaded — captured from a real UT61E (parity bits intact). */
    private val frameA = byteArrayOf(52, 50, 50, -75, 56, -80, 59, 49, -80, -80, 56, -80, 13, -118)

    /** 0.320 nF, auto range — captured from a real UT61E. */
    private val frameB = byteArrayOf(-80, -80, -80, -77, 50, -80, -74, -80, -80, -80, 50, -80, 13, -118)

    @Test
    fun `one notification carrying exactly one frame yields that frame`() {
        val frames = FrameAssembler().feed(frameA)

        assertEquals(1, frames.size)
        assertArrayEquals(frameA, frames[0])
    }

    @Test
    fun `emitted frames are still decodable, parity bits untouched`() {
        val frame = FrameAssembler().feed(frameA).single()

        assertTrue(Ut61eFrameDecoder.decode(frame).isSuccess)
    }

    @Test
    fun `a frame split across two notifications is completed`() {
        val assembler = FrameAssembler()

        val first = assembler.feed(frameA.copyOfRange(0, 8))
        val second = assembler.feed(frameA.copyOfRange(8, frameA.size))

        assertTrue(first.isEmpty())
        assertEquals(1, second.size)
        assertArrayEquals(frameA, second[0])
    }

    @Test
    fun `two frames in one notification yield both, in order`() {
        val frames = FrameAssembler().feed(frameA + frameB)

        assertEquals(2, frames.size)
        assertArrayEquals(frameA, frames[0])
        assertArrayEquals(frameB, frames[1])
    }

    @Test
    fun `a garbage prefix is dropped and the stream re-syncs`() {
        val assembler = FrameAssembler()

        // Subscribing mid-frame: five stray bytes, then a truncated frame ending
        // in CR LF, then a clean one.
        val garbage = byteArrayOf(0x31, 0x32, 0x33, 0x0D.toByte(), 0x0A.toByte())
        val first = assembler.feed(garbage)
        val second = assembler.feed(frameA)

        assertTrue(first.isEmpty())
        assertEquals(1, second.size)
        assertArrayEquals(frameA, second[0])
    }

    @Test
    fun `a garbage prefix in the same chunk as a frame is dropped`() {
        val frames = FrameAssembler().feed(byteArrayOf(0x41, 0x42, 0x43) + frameA)

        assertEquals(1, frames.size)
        assertArrayEquals(frameA, frames[0])
    }

    @Test
    fun `a terminator-less flood is trimmed and does not break the next frame`() {
        val assembler = FrameAssembler()

        val flood = ByteArray(500) { 0x31 }
        assertTrue(assembler.feed(flood).isEmpty())
        assertEquals(FrameAssembler.MAX_BUFFER_SIZE, assembler.buffered)

        val frames = assembler.feed(frameA)

        assertEquals(1, frames.size)
        assertArrayEquals(frameA, frames[0])
        assertEquals(0, assembler.buffered)
    }

    @Test
    fun `reset drops a partial frame`() {
        val assembler = FrameAssembler()

        assembler.feed(frameA.copyOfRange(0, 8))
        assembler.reset()
        val frames = assembler.feed(frameA.copyOfRange(8, frameA.size))

        assertEquals(0, assembler.buffered)
        assertTrue(frames.isEmpty())
    }

    @Test
    fun `empty chunks are ignored`() {
        val assembler = FrameAssembler()

        assertTrue(assembler.feed(ByteArray(0)).isEmpty())
        assertEquals(1, assembler.feed(frameA).size)
    }
}
