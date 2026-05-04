package com.aciderix.obbinstaller.axml

object ManifestPatcher {

    private const val ATTR_ID_NAME: Long = 0x01010003L
    private const val ATTR_ID_AUTHORITIES: Long = 0x01010018L
    private const val ATTR_ID_EXPORTED: Long = 0x01010010L
    private const val ATTR_ID_TARGET_SDK_VERSION: Long = 0x01010270L

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

    /**
     * Ensures `<uses-sdk android:targetSdkVersion>` is at least [minTargetSdk].
     * Android 14+ refuses to install APKs targeting too-old SDKs. Bumps existing
     * value or inserts a new `<uses-sdk>` child of `<manifest>` if missing.
     */
    fun bumpTargetSdk(originalManifest: ByteArray, minTargetSdk: Int): ByteArray {
        val doc = AxmlReader.parse(originalManifest)
        val sp = doc.stringPool
        val rm = doc.resourceMap
        val androidNsIdx = sp.indexOf(ANDROID_NS_URI).takeIf { it >= 0 }
            ?: ensureString(sp, ANDROID_NS_URI)

        val usesSdkNode = doc.nodes.firstOrNull {
            it is AxmlNode.StartElement && sp.get(it.name) == "uses-sdk"
        } as? AxmlNode.StartElement

        if (usesSdkNode != null) {
            val existing = usesSdkNode.attributes.firstOrNull { a ->
                a.ns == androidNsIdx && sp.get(a.name) == "targetSdkVersion"
            }
            if (existing != null) {
                val current = existing.typedValue.data.toInt()
                if (current < minTargetSdk) {
                    existing.rawValue = NULL_STRING_INDEX
                    existing.typedValue = ResValue(
                        dataType = ResValueType.INT_DEC,
                        data = minTargetSdk.toLong()
                    )
                }
            } else {
                val nameIdx = ensureAttributeName(sp, rm, "targetSdkVersion", ATTR_ID_TARGET_SDK_VERSION)
                usesSdkNode.attributes.add(
                    AxmlAttribute(
                        ns = androidNsIdx,
                        name = nameIdx,
                        rawValue = NULL_STRING_INDEX,
                        typedValue = ResValue(
                            dataType = ResValueType.INT_DEC,
                            data = minTargetSdk.toLong()
                        )
                    )
                )
            }
            return AxmlWriter.serialize(doc)
        }

        // No <uses-sdk> at all - inject one as the first child of <manifest>.
        val manifestStartIdx = doc.nodes.indexOfFirst {
            it is AxmlNode.StartElement && sp.get(it.name) == "manifest"
        }
        require(manifestStartIdx >= 0) { "no <manifest> element" }

        val usesSdkNameIdx = ensureString(sp, "uses-sdk")
        val targetAttrIdx = ensureAttributeName(sp, rm, "targetSdkVersion", ATTR_ID_TARGET_SDK_VERSION)

        val refLine = (doc.nodes[manifestStartIdx] as AxmlNode.StartElement).lineNumber
        val start = AxmlNode.StartElement(
            lineNumber = refLine,
            comment = -1,
            ns = NULL_STRING_INDEX,
            name = usesSdkNameIdx,
            idIndex = 0, classIndex = 0, styleIndex = 0,
            attributes = mutableListOf(
                AxmlAttribute(
                    ns = androidNsIdx,
                    name = targetAttrIdx,
                    rawValue = NULL_STRING_INDEX,
                    typedValue = ResValue(
                        dataType = ResValueType.INT_DEC,
                        data = minTargetSdk.toLong()
                    )
                )
            )
        )
        val end = AxmlNode.EndElement(
            lineNumber = refLine, comment = -1,
            ns = NULL_STRING_INDEX, name = usesSdkNameIdx
        )
        doc.nodes.add(manifestStartIdx + 1, start)
        doc.nodes.add(manifestStartIdx + 2, end)
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
