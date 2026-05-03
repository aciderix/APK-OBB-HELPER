package com.aciderix.obbinstaller.axml

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AxmlWriter {

    fun serialize(doc: AxmlDocument): ByteArray {
        val stringPoolBytes = writeStringPool(doc.stringPool)
        val resourceMapBytes = if (doc.resourceMap.isEmpty()) ByteArray(0)
                               else writeResourceMap(doc.resourceMap)
        val nodesBytes = writeNodes(doc.nodes)

        val total = 8 + stringPoolBytes.size + resourceMapBytes.size + nodesBytes.size
        val out = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
        // XML chunk header
        out.putShort(Chunk.XML.toShort())
        out.putShort(8.toShort()) // headerSize
        out.putInt(total)
        out.put(stringPoolBytes)
        out.put(resourceMapBytes)
        out.put(nodesBytes)
        return out.array()
    }

    private fun writeStringPool(pool: StringPool): ByteArray {
        val stringCount = pool.strings.size
        val styleCount = 0
        val flags = (if (pool.utf8) StringPoolFlags.UTF8 else 0) or
                    (if (pool.sorted) StringPoolFlags.SORTED else 0)

        // First write all strings into a buffer + record offsets
        val stringDataBuf = ByteArrayOutputStream()
        val stringOffsets = IntArray(stringCount)
        for (i in 0 until stringCount) {
            stringOffsets[i] = stringDataBuf.size()
            val s = pool.strings[i]
            if (pool.utf8) writeUtf8(stringDataBuf, s) else writeUtf16(stringDataBuf, s)
        }
        // pad to 4 bytes
        while (stringDataBuf.size() % 4 != 0) stringDataBuf.write(0)

        val headerSize = 0x1C
        val offsetsSize = stringCount * 4 + styleCount * 4
        val stringsStart = headerSize + offsetsSize
        val stylesStart = 0
        val totalSize = stringsStart + stringDataBuf.size()

        val out = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        out.putShort(Chunk.STRING_POOL.toShort())
        out.putShort(headerSize.toShort())
        out.putInt(totalSize)
        out.putInt(stringCount)
        out.putInt(styleCount)
        out.putInt(flags)
        out.putInt(stringsStart)
        out.putInt(stylesStart)
        for (off in stringOffsets) out.putInt(off)
        out.put(stringDataBuf.toByteArray())
        return out.array()
    }

    private fun writeUtf16(out: ByteArrayOutputStream, s: String) {
        val len = s.length
        val buf = ByteBuffer.allocate(2 + 2 * len + 2).order(ByteOrder.LITTLE_ENDIAN)
        if (len > 0x7FFF) {
            buf.putShort(((len ushr 16) or 0x8000).toShort())
            buf.putShort((len and 0xFFFF).toShort())
        } else {
            buf.putShort(len.toShort())
        }
        for (c in s) buf.putShort(c.code.toShort())
        buf.putShort(0)
        out.write(buf.array(), 0, buf.position())
    }

    private fun writeUtf8(out: ByteArrayOutputStream, s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        // utf16-char-count + utf8-byte-count + bytes + null terminator
        writeUtf8Length(out, s.length)
        writeUtf8Length(out, bytes.size)
        out.write(bytes)
        out.write(0)
    }

    private fun writeUtf8Length(out: ByteArrayOutputStream, n: Int) {
        if (n > 0x7F) {
            out.write(((n ushr 8) or 0x80) and 0xFF)
            out.write(n and 0xFF)
        } else {
            out.write(n and 0xFF)
        }
    }

    private fun writeResourceMap(ids: List<Long>): ByteArray {
        val headerSize = 8
        val totalSize = headerSize + ids.size * 4
        val out = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        out.putShort(Chunk.XML_RESOURCE_MAP.toShort())
        out.putShort(headerSize.toShort())
        out.putInt(totalSize)
        for (id in ids) out.putInt(id.toInt())
        return out.array()
    }

    private fun writeNodes(nodes: List<AxmlNode>): ByteArray {
        val out = ByteArrayOutputStream()
        for (node in nodes) {
            val chunk = when (node) {
                is AxmlNode.StartNamespace -> writeNamespace(node.lineNumber, node.comment, node.prefix, node.uri, isStart = true)
                is AxmlNode.EndNamespace -> writeNamespace(node.lineNumber, node.comment, node.prefix, node.uri, isStart = false)
                is AxmlNode.StartElement -> writeStartElement(node)
                is AxmlNode.EndElement -> writeEndElement(node)
                is AxmlNode.CData -> writeCData(node)
            }
            out.write(chunk)
        }
        return out.toByteArray()
    }

    private fun writeNamespace(line: Int, comment: Int, prefix: Int, uri: Int, isStart: Boolean): ByteArray {
        val type = if (isStart) Chunk.XML_START_NAMESPACE else Chunk.XML_END_NAMESPACE
        val headerSize = 0x10
        val totalSize = headerSize + 8
        val out = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        out.putShort(type.toShort())
        out.putShort(headerSize.toShort())
        out.putInt(totalSize)
        out.putInt(line)
        out.putInt(comment)
        out.putInt(prefix)
        out.putInt(uri)
        return out.array()
    }

    private fun writeStartElement(node: AxmlNode.StartElement): ByteArray {
        val headerSize = 0x10
        val attrSize = 0x14
        val attrCount = node.attributes.size
        val payloadFixedSize = 8 + 8 + attrCount * attrSize  // ns,name + 4 short + attrs
        // Actually layout:
        //   ns(4) name(4) attributeStart(2) attributeSize(2) attributeCount(2) idIndex(2) classIndex(2) styleIndex(2)
        //   = 20 bytes of fixed extension
        val fixedExt = 20
        val totalSize = headerSize + fixedExt + attrCount * attrSize
        val out = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        out.putShort(Chunk.XML_START_ELEMENT.toShort())
        out.putShort(headerSize.toShort())
        out.putInt(totalSize)
        out.putInt(node.lineNumber)
        out.putInt(node.comment)
        out.putInt(node.ns)
        out.putInt(node.name)
        out.putShort(fixedExt.toShort())   // attributeStart (offset from end of headerSize)
        out.putShort(attrSize.toShort())
        out.putShort(attrCount.toShort())
        out.putShort(node.idIndex.toShort())
        out.putShort(node.classIndex.toShort())
        out.putShort(node.styleIndex.toShort())
        for (a in node.attributes) {
            out.putInt(a.ns)
            out.putInt(a.name)
            out.putInt(a.rawValue)
            out.putShort(a.typedValue.size.toShort())
            out.put(a.typedValue.res0.toByte())
            out.put(a.typedValue.dataType.toByte())
            out.putInt(a.typedValue.data.toInt())
        }
        return out.array()
    }

    private fun writeEndElement(node: AxmlNode.EndElement): ByteArray {
        val headerSize = 0x10
        val totalSize = headerSize + 8
        val out = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        out.putShort(Chunk.XML_END_ELEMENT.toShort())
        out.putShort(headerSize.toShort())
        out.putInt(totalSize)
        out.putInt(node.lineNumber)
        out.putInt(node.comment)
        out.putInt(node.ns)
        out.putInt(node.name)
        return out.array()
    }

    private fun writeCData(node: AxmlNode.CData): ByteArray {
        val headerSize = 0x10
        val totalSize = headerSize + 12
        val out = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        out.putShort(Chunk.XML_CDATA.toShort())
        out.putShort(headerSize.toShort())
        out.putInt(totalSize)
        out.putInt(node.lineNumber)
        out.putInt(node.comment)
        out.putInt(node.data)
        out.putShort(node.typedValue.size.toShort())
        out.put(node.typedValue.res0.toByte())
        out.put(node.typedValue.dataType.toByte())
        out.putInt(node.typedValue.data.toInt())
        return out.array()
    }
}
