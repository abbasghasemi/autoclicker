package ghasemi.abbas.autoclicker

import ghasemi.abbas.autoclicker.stream.SerializedData

internal object ScriptOptionsTrailer {
    private const val MAGIC = 0x53435250
    private const val VERSION = 1

    data class Options(val targetPackage: String, val onLeaveAction: Int)

    fun write(stream: SerializedData, options: List<Options>) {
        stream.writeInt32(MAGIC)
        stream.writeInt32(VERSION)
        stream.writeInt32(options.size)
        options.forEach {
            stream.writeString(it.targetPackage)
            stream.writeInt32(it.onLeaveAction)
        }
    }

    fun read(stream: SerializedData, expectedCount: Int): List<Options>? {
        if (stream.remaining() < 12 || stream.readInt32(false) != MAGIC ||
            stream.readInt32(false) != VERSION || stream.readInt32(false) != expectedCount) return null
        val options = ArrayList<Options>(expectedCount)
        repeat(expectedCount) {
            if (stream.remaining() < 8) return null
            val targetPackage = stream.readString(false) ?: return null
            if (stream.remaining() < 4) return null
            options.add(Options(targetPackage, stream.readInt32(false).coerceIn(0, 2)))
        }
        return options
    }
}
