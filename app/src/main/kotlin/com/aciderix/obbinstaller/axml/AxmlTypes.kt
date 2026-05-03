package com.aciderix.obbinstaller.axml

/**
 * Minimal Android binary XML parser/writer focused on the operation we need:
 * "add a sharedUserId attribute to the <manifest> element".
 *
 * Reference for the binary format:
 * https://android.googlesource.com/platform/frameworks/base/+/master/libs/androidfw/include/androidfw/ResourceTypes.h
 */

internal object Chunk {
    const val NULL = 0x0000
    const val STRING_POOL = 0x0001
    const val XML = 0x0003
    const val XML_START_NAMESPACE = 0x0100
    const val XML_END_NAMESPACE = 0x0101
    const val XML_START_ELEMENT = 0x0102
    const val XML_END_ELEMENT = 0x0103
    const val XML_CDATA = 0x0104
    const val XML_RESOURCE_MAP = 0x0180
}

internal object StringPoolFlags {
    const val SORTED = 0x1
    const val UTF8 = 0x100
}

internal object ResValueType {
    const val NULL = 0x00
    const val REFERENCE = 0x01
    const val STRING = 0x03
    const val INT_DEC = 0x10
    const val INT_HEX = 0x11
    const val INT_BOOLEAN = 0x12
}

object AndroidAttrIds {
    // Public Android attribute resource IDs.
    // See: frameworks/base/core/res/res/values/public.xml
    const val SHARED_USER_ID: Long = 0x0101000B
    const val NAME: Long = 0x01010003
}

const val ANDROID_NS_URI = "http://schemas.android.com/apk/res/android"
const val NULL_STRING_INDEX: Int = -1

data class ResValue(
    var size: Int = 8,         // always 8 in practice
    var res0: Int = 0,
    var dataType: Int,
    var data: Long
)

data class AxmlAttribute(
    var ns: Int,
    var name: Int,
    var rawValue: Int,
    var typedValue: ResValue
)

sealed class AxmlNode {
    abstract var lineNumber: Int
    abstract var comment: Int

    data class StartNamespace(
        override var lineNumber: Int,
        override var comment: Int,
        var prefix: Int,
        var uri: Int
    ) : AxmlNode()

    data class EndNamespace(
        override var lineNumber: Int,
        override var comment: Int,
        var prefix: Int,
        var uri: Int
    ) : AxmlNode()

    data class StartElement(
        override var lineNumber: Int,
        override var comment: Int,
        var ns: Int,
        var name: Int,
        var idIndex: Int,
        var classIndex: Int,
        var styleIndex: Int,
        val attributes: MutableList<AxmlAttribute>
    ) : AxmlNode()

    data class EndElement(
        override var lineNumber: Int,
        override var comment: Int,
        var ns: Int,
        var name: Int
    ) : AxmlNode()

    data class CData(
        override var lineNumber: Int,
        override var comment: Int,
        var data: Int,
        var typedValue: ResValue
    ) : AxmlNode()
}

class StringPool(
    val strings: MutableList<String> = mutableListOf(),
    var utf8: Boolean = false,
    var sorted: Boolean = false
) {
    fun indexOf(s: String): Int = strings.indexOf(s)
    fun get(index: Int): String? = strings.getOrNull(index)

    /** Append a new string at the end and return its index. */
    fun append(s: String): Int {
        strings.add(s)
        return strings.size - 1
    }
}

class AxmlDocument(
    val stringPool: StringPool,
    val resourceMap: MutableList<Long>,
    val nodes: MutableList<AxmlNode>
)
