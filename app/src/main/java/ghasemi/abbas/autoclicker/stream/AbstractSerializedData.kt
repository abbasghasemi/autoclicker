package ghasemi.abbas.autoclicker.stream

abstract class AbstractSerializedData {
    abstract fun writeInt32(x: Int)
    abstract fun writeInt64(x: Long)
    abstract fun writeBool(value: Boolean)
    abstract fun writeBytes(b: ByteArray?)
    abstract fun writeBytes(b: ByteArray?, offset: Int, count: Int)
    abstract fun writeByte(i: Int)
    abstract fun writeByte(b: Byte)
    abstract fun writeString(s: String?)
    abstract fun writeByteArray(b: ByteArray?, offset: Int, count: Int)
    abstract fun writeByteArray(b: ByteArray?)
    abstract fun writeDouble(d: Double)
    abstract fun writeFloat(f: Float)
    abstract fun readInt32(exception: Boolean): Int
    abstract fun readBool(exception: Boolean): Boolean
    abstract fun readInt64(exception: Boolean): Long
    abstract fun readByte(exception: Boolean): Byte
    abstract fun readBytes(b: ByteArray?, exception: Boolean)
    abstract fun readData(count: Int, exception: Boolean): ByteArray?
    abstract fun readString(exception: Boolean): String?
    abstract fun readByteArray(exception: Boolean): ByteArray?
    abstract fun readFloat(exception: Boolean): Float
    abstract fun readDouble(exception: Boolean): Double
    abstract fun length(): Int
    abstract fun skip(count: Int)
    abstract val position: Int
    abstract fun remaining(): Int
}