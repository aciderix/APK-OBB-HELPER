package com.aciderix.obbinstaller.axml

import java.nio.ByteBuffer
import java.nio.ByteOrder

object AxmlReader {

    fun parse(bytes: ByteArray): AxmlDocument {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // Outer XML chunk header
        val xmlType = buf.short.toInt() and 0xFFFF
        require(xmlType == Chunk.XML) { "Not an AXML file (got chunk 0x%04X)".format(xmlType) }
        val xmlHeaderSize = buf.short.toInt() and 0xFFFF
        val xmlSize = buf.int
        require(xmlSize <= bytes.size) { "AXML chunk size (${xmlSize}) larger than buffer (${bytes.size})" }
        // Skip any extra header bytes
        buf.position(xmlHeaderSize)

        var stringPool: StringPool? = null
        val resourceMap = mutableListOf<Long>()
        val nodes = mutableListOf<AxmlNode>()

        while (buf.position() < xmlSize) {
            val chunkStart = buf.position()
            val type = buf.short.toInt() and 0xFFFF
            val headerSize = buf.short.toInt() and 0xFFFF
            val size = buf.int

            when (type) {
                Chunk.STRING_POOL -> stringPool = readStringPool(buf, chunkStart, headerSize, size)
                Chunk.XML_RESOURCE_MAP -> readResourceMap(buf, chunkStart, headerSize, size, resourceMap)
                Chunk.XML_START_NAMESPACE -> nodes.add(readNamespaceNode(buf, chunkStart, headerSize, isStart = true))
                Chunk.XML_END_NAMESPACE -> nodes.add(readNamespaceNode(buf, chunkStart, headerSize, isStart = false))
                Chunk.XML_START_ELEMENT -> nodes.add(readStartElement(buf, chunkStart, headerSize, size))
                Chunk.XML_END_ELEMENT -> nodes.add(readEndElement(buf, chunkStart, headerSize, size))
                Chunk.XML_CDATA -> nodes.add(readCData(buf, chunkStart, headerSize, size))
                else -> {
                    // Unknown chunk - skip
                    buf.position(chunkStart + size)
                }
            }
            // Always advance to the declared end of the chunk to be defensive
            buf.position(chunkStart + size)
        }

        requireNotNull(stringPool) { "AXML missing string pool" }
        return AxmlDocument(stringPool, resourceMap, nodes)
    }

    private fun readStringPool(buf: ByteBuffer, start: Int, headerSize: Int, size: Int): StringPool {
        val stringCount = buf.int
        val styleCount = buf.int
        val flags = buf.int
        val stringsStart = buf.int
        val stylesStart = buf.int
        // (any extra header bytes)
        buf.position(start + headerSize)

        val stringOffsets = IntArray(stringCount) { buf.int }
        // skip style offsets
        repeat(styleCount) { buf.int }

        val pool = StringPool(
            utf8 = (flags and StringPoolFlags.UTF8) != 0,
            sorted = (flags and StringPoolFlags.SORTED) != 0
        )

        val stringsBase = start + stringsStart
        for (off in stringOffsets) {
            buf.position(stringsBase + off)
            pool.strings.add(if (pool.utf8) readUtf8(buf) else readUtf16(buf))
        }

        // styles ignored - manifests don't use them
        buf.position(start + size)
        return pool
    }

    private fun readUtf16(buf: ByteBuffer): String {
        var len = buf.short.toInt() and 0xFFFF
        if (len and 0x8000 != 0) {
            // Two-word length
            val high = len and 0x7FFF
            val low = buf.short.toInt() and 0xFFFF
            len = (high shl 16) or low
        }
        val chars = CharArray(len)
        for (i in 0 until len) chars[i] = buf.short.toInt().toChar()
        // null terminator
        buf.short
        return String(chars)
    }

    private fun readUtf8(buf: ByteBuffer): String {
        // Two length fields, each one or two bytes
        readUtf8Length(buf) // utf-16 char count (we ignore)
        val byteLen = readUtf8Length(buf)
        val bytes = ByteArray(byteLen)
        buf.get(bytes)
        // null terminator
        buf.get()
        return String(bytes, Charsets.UTF_8)
    }

    private fun readUtf8Length(buf: ByteBuffer): Int {
        val first = buf.get().toInt() and 0xFF
        return if (first and 0x80 != 0) {
            val second = buf.get().toInt() and 0xFF
            ((first and 0x7F) shl 8) or second
        } else first
    }

    private fun readResourceMap(
        buf: ByteBuffer,
        start: Int,
        headerSize: Int,
        size: Int,
        out: MutableList<Long>
    ) {
        buf.position(start + headerSize)
        val count = (size - headerSize) / 4
        repeat(count) { out.add(buf.int.toLong() and 0xFFFFFFFFL) }
    }

    private fun readNamespaceNode(
        buf: ByteBuffer,
        chunkStart: Int,
        headerSize: Int,
        isStart: Boolean
    ): AxmlNode {
        val line = buf.int
        val comment = buf.int
        buf.position(chunkStart + headerSize)
        val prefix = buf.int
        val uri = buf.int
        return if (isStart) AxmlNode.StartNamespace(line, comment, prefix, uri)
               else AxmlNode.EndNamespace(line, comment, prefix, uri)
    }

    private fun readStartElement(buf: ByteBuffer, start: Int, headerSize: Int, size: Int): AxmlNode.StartElement {
        val line = buf.int
        val comment = buf.int
        buf.position(start + headerSize)
        val ns = buf.int
        val name = buf.int
        val attributeStart = buf.short.toInt() and 0xFFFF
        val attributeSize = buf.short.toInt() and 0xFFFF
        val attributeCount = buf.short.toInt() and 0xFFFF
        val idIndex = buf.short.toInt() and 0xFFFF
        val classIndex = buf.short.toInt() and 0xFFFF
        val styleIndex = buf.short.toInt() and 0xFFFF

        // attributes start at `start + headerSize + attributeStart`
        val attrs = mutableListOf<AxmlAttribute>()
        val attrsBase = start + headerSize + attributeStart
        for (i in 0 until attributeCount) {
            buf.position(attrsBase + i * attributeSize)
            attrs.add(readAttribute(buf))
        }
        return AxmlNode.StartElement(line, comment, ns, name, idIndex, classIndex, styleIndex, attrs)
    }

    private fun readAttribute(buf: ByteBuffer): AxmlAttribute {
        val ns = buf.int
        val name = buf.int
        val rawValue = buf.int
        val tvSize = buf.short.toInt() and 0xFFFF
        val tvRes0 = buf.get().toInt() and 0xFF
        val tvType = buf.get().toInt() and 0xFF
        val tvData = buf.int.toLong() and 0xFFFFFFFFL
        return AxmlAttribute(
            ns = ns,
            name = name,
            rawValue = rawValue,
            typedValue = ResValue(size = tvSize, res0 = tvRes0, dataType = tvType, data = tvData)
        )
    }

    private fun readEndElement(buf: ByteBuffer, start: Int, headerSize: Int, size: Int): AxmlNode.EndElement {
        val line = buf.int
        val comment = buf.int
        buf.position(start + headerSize)
        val ns = buf.int
        val name = buf.int
        return AxmlNode.EndElement(line, comment, ns, name)
    }

    private fun readCData(buf: ByteBuffer, start: Int, headerSize: Int, size: Int): AxmlNode.CData {
        val line = buf.int
        val comment = buf.int
        buf.position(start + headerSize)
        val data = buf.int
        val tvSize = buf.short.toInt() and 0xFFFF
        val tvRes0 = buf.get().toInt() and 0xFF
        val tvType = buf.get().toInt() and 0xFF
        val tvData = buf.int.toLong() and 0xFFFFFFFFL
        return AxmlNode.CData(line, comment, data, ResValue(tvSize, tvRes0, tvType, tvData))
    }
}
