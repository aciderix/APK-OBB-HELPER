package com.aciderix.obbinstaller.axml

object ManifestPatcher {

    /**
     * Adds (or updates) android:sharedUserId on the <manifest> root element.
     * Returns the patched binary AndroidManifest.xml bytes.
     */
    fun addSharedUserId(originalManifest: ByteArray, sharedUserId: String): ByteArray {
        val doc = AxmlReader.parse(originalManifest)
        val sp = doc.stringPool

        // Find <manifest> start element
        val manifestNode = doc.nodes.firstOrNull {
            it is AxmlNode.StartElement && sp.get(it.name) == "manifest"
        } as? AxmlNode.StartElement
            ?: error("AndroidManifest.xml has no <manifest> element")

        // Find or add android namespace URI string
        val androidNsIdx = ensureString(sp, ANDROID_NS_URI)

        // Already has sharedUserId? update its value and exit.
        val existing = manifestNode.attributes.firstOrNull { a ->
            a.ns == androidNsIdx && sp.get(a.name) == "sharedUserId"
        }
        if (existing != null) {
            val newValueIdx = ensureString(sp, sharedUserId)
            existing.rawValue = newValueIdx
            existing.typedValue = ResValue(dataType = ResValueType.STRING, data = newValueIdx.toLong())
            return AxmlWriter.serialize(doc)
        }

        // Make sure "sharedUserId" string exists AND has a resource map entry pointing
        // to attr id 0x0101000B. The resource map only covers indices 0..M-1, so we
        // extend it as needed.
        val nameIdx = ensureAttributeName(sp, doc.resourceMap, "sharedUserId", AndroidAttrIds.SHARED_USER_ID)
        val valueIdx = ensureString(sp, sharedUserId)

        manifestNode.attributes.add(
            AxmlAttribute(
                ns = androidNsIdx,
                name = nameIdx,
                rawValue = valueIdx,
                typedValue = ResValue(dataType = ResValueType.STRING, data = valueIdx.toLong())
            )
        )

        return AxmlWriter.serialize(doc)
    }

    private fun ensureString(pool: StringPool, s: String): Int {
        val existing = pool.indexOf(s)
        if (existing >= 0) return existing
        return pool.append(s)
    }

    /**
     * Ensures `name` is in the string pool and that the resource map maps its index
     * to `resourceId`. Extends the resource map if needed.
     */
    private fun ensureAttributeName(
        pool: StringPool,
        resourceMap: MutableList<Long>,
        name: String,
        resourceId: Long
    ): Int {
        var idx = pool.indexOf(name)
        if (idx < 0) {
            idx = pool.append(name)
        }
        // Extend resource map with zeros up to idx
        while (resourceMap.size <= idx) resourceMap.add(0L)
        resourceMap[idx] = resourceId
        return idx
    }
}
