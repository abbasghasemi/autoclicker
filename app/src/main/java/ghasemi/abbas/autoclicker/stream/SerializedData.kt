package ghasemi.abbas.autoclicker.stream

import ghasemi.abbas.autoclicker.BuildVars
import ghasemi.abbas.autoclicker.FileLog.e
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset

class SerializedData : AbstractSerializedData {
    protected var isOut = true
    private var outbuf: ByteArrayOutputStream? = null
    private var out: DataOutputStream? = null
    private var inbuf: ByteArrayInputStream? = null
    private var input: DataInputStream? = null
    private var justCalc = false
    override var position = 0
        private set

    constructor() {
        outbuf = ByteArrayOutputStream()
        out = DataOutputStream(outbuf)
    }

    constructor(calculate: Boolean) {
        if (!calculate) {
            outbuf = ByteArrayOutputStream()
            out = DataOutputStream(outbuf)
        }
        justCalc = calculate
        position = 0
    }

    constructor(size: Int) {
        outbuf = ByteArrayOutputStream(size)
        out = DataOutputStream(outbuf)
    }

    constructor(data: ByteArray?) {
        isOut = false
        inbuf = ByteArrayInputStream(data)
        input = DataInputStream(inbuf)
        position = 0
    }

    fun cleanup() {
        try {
            if (inbuf != null) {
                inbuf!!.close()
                inbuf = null
            }
        } catch (e: Exception) {
            e(e)
        }
        try {
            if (input != null) {
                input!!.close()
                input = null
            }
        } catch (e: Exception) {
            e(e)
        }
        try {
            if (outbuf != null) {
                outbuf!!.close()
                outbuf = null
            }
        } catch (e: Exception) {
            e(e)
        }
        try {
            if (out != null) {
                out!!.close()
                out = null
            }
        } catch (e: Exception) {
            e(e)
        }
    }

    constructor(file: File) {
        val inputStream = FileInputStream(file)
        val data = ByteArray(file.length().toInt())
        DataInputStream(inputStream).readFully(data)
        inputStream.close()
        isOut = false
        inbuf = ByteArrayInputStream(data)
        input = DataInputStream(inbuf)
    }

    override fun writeInt32(x: Int) {
        if (!justCalc) {
            writeInt32(x, out)
        } else {
            position += 4
        }
    }

    private fun writeInt32(x: Int, out: DataOutputStream?) {
        try {
            for (i in 0..3) {
                out!!.write(x shr i * 8)
            }
        } catch (e: Exception) {
            if (BuildVars.LOGS_ENABLED) {
                e("write int32 error")
                e(e)
            }
        }
    }

    override fun writeInt64(i: Long) {
        if (!justCalc) {
            writeInt64(i, out)
        } else {
            position += 8
        }
    }

    private fun writeInt64(x: Long, out: DataOutputStream?) {
        try {
            for (i in 0..7) {
                out!!.write((x shr i * 8).toInt())
            }
        } catch (e: Exception) {
            if (BuildVars.LOGS_ENABLED) {
                e("write int64 error")
                e(e)
            }
        }
    }

    override fun writeBool(value: Boolean) {
        if (!justCalc) {
            if (value) {
                writeInt32(-0x668d8a4b)
            } else {
                writeInt32(-0x438668c9)
            }
        } else {
            position += 4
        }
    }

    override fun writeBytes(b: ByteArray?) {
        try {
            if (!justCalc) {
                out!!.write(b)
            } else {
                position += b!!.size
            }
        } catch (e: Exception) {
            if (BuildVars.LOGS_ENABLED) {
                e("write raw error")
                e(e)
            }
        }
    }

    override fun writeBytes(b: ByteArray?, offset: Int, count: Int) {
        try {
            if (!justCalc) {
                out!!.write(b, offset, count)
            } else {
                position += count
            }
        } catch (e: Exception) {
            if (BuildVars.LOGS_ENABLED) {
                e("write bytes error")
                e(e)
            }
        }
    }

    override fun writeByte(i: Int) {
        try {
            if (!justCalc) {
                out!!.writeByte(i.toByte().toInt())
            } else {
                position += 1
            }
        } catch (e: Exception) {
            if (BuildVars.LOGS_ENABLED) {
                e("write byte error")
                e(e)
            }
        }
    }

    override fun writeByte(b: Byte) {
        try {
            if (!justCalc) {
                out!!.writeByte(b.toInt())
            } else {
                position += 1
            }
        } catch (e: Exception) {
            if (BuildVars.LOGS_ENABLED) {
                e("write byte error")
                e(e)
            }
        }
    }

    override fun writeByteArray(b: ByteArray?) {
        try {
            if (b!!.size <= 253) {
                if (!justCalc) {
                    out!!.write(b.size)
                } else {
                    position += 1
                }
            } else {
                if (!justCalc) {
                    out!!.write(254)
                    out!!.write(b.size)
                    out!!.write(b.size shr 8)
                    out!!.write(b.size shr 16)
                } else {
                    position += 4
                }
            }
            if (!justCalc) {
                out!!.write(b)
            } else {
                position += b.size
            }
            var i = if (b.size <= 253) 1 else 4
            while ((b.size + i) % 4 != 0) {
                if (!justCalc) {
                    out!!.write(0)
                } else {
                    position += 1
                }
                i++
            }
        } catch (e: Exception) {
            if (BuildVars.LOGS_ENABLED) {
                e("write byte array error")
                e(e)
            }
        }
    }

    override fun writeString(s: String?) {
        try {
            writeByteArray(s!!.toByteArray(charset("UTF-8")))
        } catch (e: Exception) {
            if (BuildVars.LOGS_ENABLED) {
                e("write string error")
                e(e)
            }
        }
    }

    override fun writeByteArray(b: ByteArray?, offset: Int, count: Int) {
        try {
            if (count <= 253) {
                if (!justCalc) {
                    out!!.write(count)
                } else {
                    position += 1
                }
            } else {
                if (!justCalc) {
                    out!!.write(254)
                    out!!.write(count)
                    out!!.write(count shr 8)
                    out!!.write(count shr 16)
                } else {
                    position += 4
                }
            }
            if (!justCalc) {
                out!!.write(b, offset, count)
            } else {
                position += count
            }
            var i = if (count <= 253) 1 else 4
            while ((count + i) % 4 != 0) {
                if (!justCalc) {
                    out!!.write(0)
                } else {
                    position += 1
                }
                i++
            }
        } catch (e: Exception) {
            if (BuildVars.LOGS_ENABLED) {
                e("write byte array error")
                e(e)
            }
        }
    }

    override fun writeDouble(d: Double) {
        try {
            writeInt64(java.lang.Double.doubleToRawLongBits(d))
        } catch (e: Exception) {
            if (BuildVars.LOGS_ENABLED) {
                e("write double error")
                e(e)
            }
        }
    }

    override fun writeFloat(d: Float) {
        try {
            writeInt32(java.lang.Float.floatToIntBits(d))
        } catch (e: Exception) {
            if (BuildVars.LOGS_ENABLED) {
                e("write float error")
                e(e)
            }
        }
    }

    override fun length(): Int {
        return if (!justCalc) {
            if (isOut) outbuf!!.size() else inbuf!!.available()
        } else position
    }

    protected fun set(newData: ByteArray?) {
        isOut = false
        inbuf = ByteArrayInputStream(newData)
        input = DataInputStream(inbuf)
    }

    fun toByteArray(): ByteArray {
        return outbuf!!.toByteArray()
    }

    override fun skip(count: Int) {
        if (count == 0) {
            return
        }
        if (!justCalc) {
            if (input != null) {
                try {
                    input!!.skipBytes(count)
                } catch (e: Exception) {
                    e(e)
                }
            }
        } else {
            position += count
        }
    }

    override fun readBool(exception: Boolean): Boolean {
        val consructor = readInt32(exception)
        if (consructor == -0x668d8a4b) {
            return true
        } else if (consructor == -0x438668c9) {
            return false
        }
        if (exception) {
            throw RuntimeException("Not bool value!")
        } else {
            if (BuildVars.LOGS_ENABLED) {
                e("Not bool value!")
            }
        }
        return false
    }

    override fun readByte(exception: Boolean): Byte {
        try {
            val result = input!!.readByte()
            position += 1
            return result
        } catch (e: Exception) {
            if (exception) {
                throw RuntimeException("read byte error", e)
            } else {
                if (BuildVars.LOGS_ENABLED) {
                    e("read byte error")
                    e(e)
                }
            }
        }
        return 0
    }

    override fun readBytes(b: ByteArray?, exception: Boolean) {
        try {
            input!!.read(b)
            position += b!!.size
        } catch (e: Exception) {
            if (exception) {
                throw RuntimeException("read bytes error", e)
            } else {
                if (BuildVars.LOGS_ENABLED) {
                    e("read bytes error")
                    e(e)
                }
            }
        }
    }

    override fun readData(count: Int, exception: Boolean): ByteArray? {
        val arr = ByteArray(count)
        readBytes(arr, exception)
        return arr
    }

    override fun readString(exception: Boolean): String? {
        try {
            var sl = 1
            var l = input!!.read()
            position++
            if (l >= 254) {
                l = input!!.read() or (input!!.read() shl 8) or (input!!.read() shl 16)
                position += 3
                sl = 4
            }
            val b = ByteArray(l)
            input!!.read(b)
            position++
            var i = sl
            while ((l + i) % 4 != 0) {
                input!!.read()
                position++
                i++
            }
            return String(b, Charset.forName("UTF-8"))
        } catch (e: Exception) {
            if (exception) {
                throw RuntimeException("read string error", e)
            } else {
                if (BuildVars.LOGS_ENABLED) {
                    e("read string error")
                    e(e)
                }
            }
        }
        return null
    }

    override fun readByteArray(exception: Boolean): ByteArray? {
        try {
            var sl = 1
            var l = input!!.read()
            position++
            if (l >= 254) {
                l = input!!.read() or (input!!.read() shl 8) or (input!!.read() shl 16)
                position += 3
                sl = 4
            }
            val b = ByteArray(l)
            input!!.read(b)
            position++
            var i = sl
            while ((l + i) % 4 != 0) {
                input!!.read()
                position++
                i++
            }
            return b
        } catch (e: Exception) {
            if (exception) {
                throw RuntimeException("read byte array error", e)
            } else {
                if (BuildVars.LOGS_ENABLED) {
                    e("read byte array error")
                    e(e)
                }
            }
        }
        return null
    }

    override fun readDouble(exception: Boolean): Double {
        try {
            return java.lang.Double.longBitsToDouble(readInt64(exception))
        } catch (e: Exception) {
            if (exception) {
                throw RuntimeException("read double error", e)
            } else {
                if (BuildVars.LOGS_ENABLED) {
                    e("read double error")
                    e(e)
                }
            }
        }
        return 0E1
    }

    override fun readFloat(exception: Boolean): Float {
        try {
            return java.lang.Float.intBitsToFloat(readInt32(exception))
        } catch (e: Exception) {
            if (exception) {
                throw RuntimeException("read float error", e)
            } else {
                if (BuildVars.LOGS_ENABLED) {
                    e("read float error")
                    e(e)
                }
            }
        }
        return 0f
    }

    override fun readInt32(exception: Boolean): Int {
        try {
            var i = 0
            for (j in 0..3) {
                i = i or (input!!.read() shl j * 8)
                position++
            }
            return i
        } catch (e: Exception) {
            if (exception) {
                throw RuntimeException("read int32 error", e)
            } else {
                if (BuildVars.LOGS_ENABLED) {
                    e("read int32 error")
                    e(e)
                }
            }
        }
        return 0
    }

    override fun readInt64(exception: Boolean): Long {
        try {
            var i: Long = 0
            for (j in 0..7) {
                i = i or (input!!.read().toLong() shl j * 8)
                position++
            }
            return i
        } catch (e: Exception) {
            if (exception) {
                throw RuntimeException("read int64 error", e)
            } else {
                if (BuildVars.LOGS_ENABLED) {
                    e("read int64 error")
                    e(e)
                }
            }
        }
        return 0
    }

    override fun remaining(): Int {
        return try {
            input!!.available()
        } catch (e: Exception) {
            Int.MAX_VALUE
        }
    }
}