package ghasemi.abbas.autoclicker

import ghasemi.abbas.autoclicker.stream.SerializedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptOptionsTrailerTest {
    @Test fun oldReaderCanReadBaseRecordAndIgnoreNewTrailer() {
        val output = SerializedData()
        output.writeInt32(5)
        output.writeInt32(1)
        output.writeString("Sample")
        output.writeBool(false)
        output.writeInt64(60)
        output.writeBool(false)
        output.writeInt32(0)
        output.writeInt32(0)
        output.writeInt32(0)
        ScriptOptionsTrailer.write(output, listOf(ScriptOptionsTrailer.Options("com.example.target", 1)))

        val input = SerializedData(output.toByteArray())
        assertEquals(5, input.readInt32(false))
        assertEquals(1, input.readInt32(false))
        assertEquals("Sample", input.readString(false))
        assertEquals(false, input.readBool(false))
        assertEquals(60L, input.readInt64(false))
        assertEquals(false, input.readBool(false))
        assertEquals(0, input.readInt32(false))
        assertEquals(0, input.readInt32(false))
        assertEquals(0, input.readInt32(false))
        assertTrue(input.remaining() > 0)
        assertEquals(listOf(ScriptOptionsTrailer.Options("com.example.target", 1)),
            ScriptOptionsTrailer.read(input, 1))
    }

    @Test fun newReaderAcceptsOldStreamWithoutTrailer() {
        val output = SerializedData()
        output.writeInt32(5)
        output.writeInt32(0)
        val input = SerializedData(output.toByteArray())
        assertEquals(5, input.readInt32(false))
        assertEquals(0, input.readInt32(false))
        assertNull(ScriptOptionsTrailer.read(input, 0))
    }
}
