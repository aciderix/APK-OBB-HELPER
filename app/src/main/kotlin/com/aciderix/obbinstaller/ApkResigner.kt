package com.aciderix.obbinstaller

import android.content.Context
import com.aciderix.obbinstaller.axml.ManifestPatcher
import com.android.apksig.ApkSigner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

const val SHARED_UID = "com.aciderix.hub.shared"

object ApkResigner {

    private const val KEYSTORE_ASSET = "hub.keystore"
    private const val KEY_ALIAS = "hub"
    private const val KEY_PASSWORD = "obbinstaller"

    private fun loadKeyMaterial(context: Context): Pair<PrivateKey, X509Certificate> {
        // keytool default since JDK 9 is PKCS12
        val ks = KeyStore.getInstance("PKCS12")
        context.assets.open(KEYSTORE_ASSET).use { ks.load(it, KEY_PASSWORD.toCharArray()) }
        val key = ks.getKey(KEY_ALIAS, KEY_PASSWORD.toCharArray()) as PrivateKey
        val cert = ks.getCertificate(KEY_ALIAS) as X509Certificate
        return key to cert
    }

    /**
     * Reads the input APK, patches its AndroidManifest.xml to add android:sharedUserId,
     * strips any existing v1 signatures, and re-signs the result with the hub key.
     * Returns the path to the signed output APK.
     */
    suspend fun patchAndResign(
        context: Context,
        inputApk: File,
        sharedUserId: String = SHARED_UID,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val unsigned = File(context.cacheDir, "patched-unsigned.apk")
        val signed = File(context.cacheDir, "patched-signed.apk")
        if (unsigned.exists()) unsigned.delete()
        if (signed.exists()) signed.delete()

        // Phase 1: rewrite ZIP with patched manifest, dropping v1 signature entries.
        ZipFile(inputApk).use { zip ->
            ZipOutputStream(unsigned.outputStream().buffered()).use { out ->
                val entries = zip.entries().toList()
                val total = entries.size.coerceAtLeast(1)
                for ((i, e) in entries.withIndex()) {
                    onProgress(0.45f * i / total)
                    val name = e.name
                    if (e.isDirectory) continue
                    if (name.startsWith("META-INF/") && (
                        name == "META-INF/MANIFEST.MF" ||
                        name.endsWith(".SF") || name.endsWith(".RSA") ||
                        name.endsWith(".DSA") || name.endsWith(".EC")
                    )) continue

                    val isManifest = name == "AndroidManifest.xml"
                    val newEntry = ZipEntry(name)
                    if (isManifest) {
                        // Always re-deflate the patched manifest
                        newEntry.method = ZipEntry.DEFLATED
                    } else {
                        newEntry.method = e.method
                        if (e.method == ZipEntry.STORED) {
                            newEntry.size = e.size
                            newEntry.crc = e.crc
                            newEntry.compressedSize = e.compressedSize
                        }
                    }
                    out.putNextEntry(newEntry)
                    if (isManifest) {
                        val original = zip.getInputStream(e).use { it.readBytes() }
                        val patched = ManifestPatcher.addSharedUserId(original, sharedUserId)
                        out.write(patched)
                    } else {
                        zip.getInputStream(e).use { it.copyTo(out) }
                    }
                    out.closeEntry()
                }
            }
        }
        onProgress(0.5f)

        // Phase 2: sign with apksig (v1+v2+v3).
        val (key, cert) = loadKeyMaterial(context)
        val signerConfig = ApkSigner.SignerConfig.Builder(KEY_ALIAS, key, listOf(cert)).build()
        ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(unsigned)
            .setOutputApk(signed)
            .setMinSdkVersion(30)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .setOtherSignersSignaturesPreserved(false)
            .build()
            .sign()

        unsigned.delete()
        onProgress(1f)
        signed
    }
}
