package com.aciderix.obbinstaller

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * In-place patcher for old Android .so files that still rely on text relocations.
 * Ancient native libraries (e.g. libogg from a 2012 game) trigger
 * `dlopen failed: ... has text relocations` on Android 7+ when the calling
 * app targets SDK >= 24.
 *
 * This patcher:
 * 1. Marks any executable LOAD segment as writable (adds PF_W). Without this,
 *    the runtime relocations against the .text page would SIGSEGV.
 * 2. Clears DT_TEXTREL and the DF_TEXTREL bit in DT_FLAGS in the dynamic
 *    section. Without this, the dynamic linker bails out before mapping.
 *
 * Both ELF32 (arm/armeabi-v7a) and ELF64 (arm64-v8a, x86_64) are supported.
 */
object ElfTextrelPatcher {

    private const val EI_CLASS = 4
    private const val EI_DATA = 5
    private const val ELFCLASS32 = 1
    private const val ELFCLASS64 = 2
    private const val ELFDATA2LSB = 1
    private const val PT_LOAD = 1
    private const val PT_DYNAMIC = 2
    private const val PF_X = 1
    private const val PF_W = 2
    private const val DT_NULL = 0L
    private const val DT_TEXTREL = 22L
    private const val DT_FLAGS = 30L
    private const val DF_TEXTREL = 0x4L

    /**
     * Returns true iff the buffer was modified (i.e. textrels were found and
     * silenced). Mutates [bytes] in place; non-ELF or already-clean files are
     * left untouched.
     */
    fun patch(bytes: ByteArray): Boolean {
        if (bytes.size < 52) return false
        if (bytes[0] != 0x7F.toByte() || bytes[1] != 'E'.code.toByte() ||
            bytes[2] != 'L'.code.toByte() || bytes[3] != 'F'.code.toByte()) return false
        val klass = bytes[EI_CLASS].toInt() and 0xFF
        val data = bytes[EI_DATA].toInt() and 0xFF
        if (data != ELFDATA2LSB) return false  // Android is always LE
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        return when (klass) {
            ELFCLASS32 -> patch32(buf, bytes)
            ELFCLASS64 -> patch64(buf, bytes)
            else -> false
        }
    }

    private fun patch32(buf: ByteBuffer, bytes: ByteArray): Boolean {
        val phoff = buf.getInt(28).toLong() and 0xFFFFFFFFL
        val phentsize = buf.getShort(42).toInt() and 0xFFFF
        val phnum = buf.getShort(44).toInt() and 0xFFFF
        if (phoff == 0L || phnum == 0) return false
        var changed = false
        var dynOffset = 0L
        var dynSize = 0L
        for (i in 0 until phnum) {
            val off = (phoff + i * phentsize).toInt()
            val pType = buf.getInt(off)
            val pFlags = buf.getInt(off + 24)
            val pOffset = buf.getInt(off + 4).toLong() and 0xFFFFFFFFL
            val pFilesz = buf.getInt(off + 16).toLong() and 0xFFFFFFFFL
            if (pType == PT_LOAD && (pFlags and PF_X) != 0 && (pFlags and PF_W) == 0) {
                buf.putInt(off + 24, pFlags or PF_W)
                changed = true
            }
            if (pType == PT_DYNAMIC) {
                dynOffset = pOffset
                dynSize = pFilesz
            }
        }
        if (dynOffset > 0 && dynSize > 0) {
            var off = dynOffset.toInt()
            val end = (dynOffset + dynSize).toInt().coerceAtMost(bytes.size)
            while (off + 8 <= end) {
                val tag = buf.getInt(off).toLong() and 0xFFFFFFFFL
                if (tag == DT_NULL) break
                if (tag == DT_TEXTREL) {
                    buf.putInt(off, DT_NULL.toInt())
                    changed = true
                } else if (tag == DT_FLAGS) {
                    val v = buf.getInt(off + 4).toLong() and 0xFFFFFFFFL
                    if ((v and DF_TEXTREL) != 0L) {
                        buf.putInt(off + 4, (v and DF_TEXTREL.inv()).toInt())
                        changed = true
                    }
                }
                off += 8
            }
        }
        return changed
    }

    private fun patch64(buf: ByteBuffer, bytes: ByteArray): Boolean {
        val phoff = buf.getLong(32)
        val phentsize = buf.getShort(54).toInt() and 0xFFFF
        val phnum = buf.getShort(56).toInt() and 0xFFFF
        if (phoff == 0L || phnum == 0) return false
        var changed = false
        var dynOffset = 0L
        var dynSize = 0L
        for (i in 0 until phnum) {
            val off = (phoff + i * phentsize).toInt()
            val pType = buf.getInt(off)
            val pFlags = buf.getInt(off + 4)  // ELF64 puts flags right after type
            val pOffset = buf.getLong(off + 8)
            val pFilesz = buf.getLong(off + 32)
            if (pType == PT_LOAD && (pFlags and PF_X) != 0 && (pFlags and PF_W) == 0) {
                buf.putInt(off + 4, pFlags or PF_W)
                changed = true
            }
            if (pType == PT_DYNAMIC) {
                dynOffset = pOffset
                dynSize = pFilesz
            }
        }
        if (dynOffset > 0 && dynSize > 0) {
            var off = dynOffset.toInt()
            val end = (dynOffset + dynSize).toInt().coerceAtMost(bytes.size)
            while (off + 16 <= end) {
                val tag = buf.getLong(off)
                if (tag == DT_NULL) break
                if (tag == DT_TEXTREL) {
                    buf.putLong(off, DT_NULL)
                    changed = true
                } else if (tag == DT_FLAGS) {
                    val v = buf.getLong(off + 8)
                    if ((v and DF_TEXTREL) != 0L) {
                        buf.putLong(off + 8, v and DF_TEXTREL.inv())
                        changed = true
                    }
                }
                off += 16
            }
        }
        return changed
    }
}
