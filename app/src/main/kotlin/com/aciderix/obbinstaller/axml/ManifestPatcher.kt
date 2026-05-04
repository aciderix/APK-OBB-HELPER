package com.aciderix.obbinstaller.axml

object ManifestPatcher {

    private const val ATTR_ID_NAME: Long = 0x01010003L
    private const val ATTR_ID_AUTHORITIES: Long = 0x01010018L
    private const val ATTR_ID_EXPORTED: Long = 0x01010010L

    /**
     * Inserts a `<provider>` element inside `<application>` with the supplied
     * class name and authority. Returns the patched binary AndroidManifest.xml.
     */
    fun addBootstrapProvider(
        originalManifest: ByteArray,
        providerClass: String,
        authority: String
    ): ByteArray {
        val doc = AxmlReader.parse(originalManifest)
        val sp = doc.stringPool
        val rm = doc.resourceMap
        sp.sorted = false  // we're going to append, breaking any sort order

        val androidNsIdx = ensureString(sp, ANDROID_NS_URI)

        // Find <application> StartElement and matching EndElement
        val appStartIdx = doc.nodes.indexOfFirst {
            it is AxmlNode.StartElement && sp.get(it.name) == "application"
        }
        require(appStartIdx >= 0) { "AndroidManifest.xml has no <application>" }

        var depth = 0
        var appEndIdx = -1
        for (i in appStartIdx until doc.nodes.size) {
            val n = doc.nodes[i]
            if (n is AxmlNode.StartElement) depth++
            else if (n is AxmlNode.EndElement) {
                depth--
                if (depth == 0) { appEndIdx = i; break }
            }
        }
        require(appEndIdx >= 0) { "no matching </application>" }

        // If a provider with this authority already exists, do nothing.
        val authorityProbe = sp.indexOf(authority)
        if (authorityProbe >= 0) {
            for (i in appStartIdx + 1 until appEndIdx) {
                val n = doc.nodes[i]
                if (n !is AxmlNode.StartElement) continue
                if (sp.get(n.name) != "provider") continue
                val hasAuthority = n.attributes.any {
                    sp.get(it.name) == "authorities" && it.rawValue == authorityProbe
                }
                if (hasAuthority) return AxmlWriter.serialize(doc)
            }
        }

        // Ensure attribute-name strings + resource map entries
        val nameAttrIdx = ensureAttributeName(sp, rm, "name", ATTR_ID_NAME)
        val exportedAttrIdx = ensureAttributeName(sp, rm, "exported", ATTR_ID_EXPORTED)
        val authoritiesAttrIdx = ensureAttributeName(sp, rm, "authorities", ATTR_ID_AUTHORITIES)
        val providerElementIdx = ensureString(sp, "provider")
        val providerClassIdx = ensureString(sp, providerClass)
        val authorityIdx = ensureString(sp, authority)

        // Sorted by resource ID (aapt2 convention).
        val attrs = mutableListOf(
            AxmlAttribute(  // android:name
                ns = androidNsIdx,
                name = nameAttrIdx,
                rawValue = providerClassIdx,
                typedValue = ResValue(dataType = ResValueType.STRING, data = providerClassIdx.toLong())
            ),
            AxmlAttribute(  // android:exported="false"
                ns = androidNsIdx,
                name = exportedAttrIdx,
                rawValue = NULL_STRING_INDEX,
                typedValue = ResValue(dataType = ResValueType.INT_BOOLEAN, data = 0L)
            ),
            AxmlAttribute(  // android:authorities
                ns = androidNsIdx,
                name = authoritiesAttrIdx,
                rawValue = authorityIdx,
                typedValue = ResValue(dataType = ResValueType.STRING, data = authorityIdx.toLong())
            )
        )

        val refLine = (doc.nodes[appStartIdx] as AxmlNode.StartElement).lineNumber
        val providerStart = AxmlNode.StartElement(
            lineNumber = refLine,
            comment = -1,
            ns = NULL_STRING_INDEX,
            name = providerElementIdx,
            idIndex = 0, classIndex = 0, styleIndex = 0,
            attributes = attrs
        )
        val providerEnd = AxmlNode.EndElement(
            lineNumber = refLine,
            comment = -1,
            ns = NULL_STRING_INDEX,
            name = providerElementIdx
        )

        // Insert immediately before </application>
        doc.nodes.add(appEndIdx, providerEnd)
        doc.nodes.add(appEndIdx, providerStart)

        return AxmlWriter.serialize(doc)
    }

    private fun ensureString(pool: StringPool, s: String): Int {
        val existing = pool.indexOf(s)
        if (existing >= 0) return existing
        pool.sorted = false
        return pool.append(s)
    }

    private fun ensureAttributeName(
        pool: StringPool,
        resourceMap: MutableList<Long>,
        name: String,
        resourceId: Long
    ): Int {
        var idx = pool.indexOf(name)
        if (idx < 0) {
            pool.sorted = false
            idx = pool.append(name)
        }
        // The resource map only covers indices [0, resourceMap.size). Extend with
        // zeros for any non-attribute strings between, then set the entry.
        while (resourceMap.size <= idx) resourceMap.add(0L)
        resourceMap[idx] = resourceId
        return idx
    }
}
