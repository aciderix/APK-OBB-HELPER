package com.aciderix.obbinstaller

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object ObbCopier {

    /**
     * Copies the OBB into /storage/emulated/0/Android/obb/<targetPackage>/<filename>.
     *
     * Strategy:
     * 1. Verify the patched game actually shares our UID (proves sharedUserId took effect).
     * 2. Wait for the OS package/UID caches to settle - immediately after PackageInstaller
     *    success, MediaProvider may still report stale package list for our UID, which
     *    causes FUSE to deny mkdir on Android/obb/<game>/.
     * 3. Try the obb dir via the game's PackageContext (officially-blessed path).
     * 4. Fall back to direct path + mkdirs.
     * 5. Stream the OBB.
     */
    suspend fun copy(
        context: Context,
        source: FileSource,
        targetPackage: String,
        filename: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val pkgDir = resolveObbDir(context, targetPackage)
            val dest = File(pkgDir, filename)
            if (dest.exists()) dest.delete()

            val total = source.length(context).coerceAtLeast(1L)
            source.openStream(context).use { input ->
                dest.outputStream().buffered().use { output ->
                    val buf = ByteArray(512 * 1024)
                    var written = 0L
                    var n: Int
                    while (input.read(buf).also { n = it } > 0) {
                        output.write(buf, 0, n)
                        written += n
                        onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                    }
                    output.flush()
                }
            }
            dest
        }
    }

    private suspend fun resolveObbDir(context: Context, targetPackage: String): File {
        val pm = context.packageManager
        val ourUid = context.applicationInfo.uid

        // Step 1: confirm the game is installed and shares our UID
        val gameInfo = try {
            pm.getApplicationInfo(targetPackage, 0)
        } catch (e: Exception) {
            throw IOException("Le jeu $targetPackage n'est pas installé : ${e.message}")
        }
        if (gameInfo.uid != ourUid) {
            throw IOException(
                "sharedUserId n'a pas pris effet : hub UID=$ourUid, jeu UID=${gameInfo.uid}. " +
                "Cause probable : HyperOS/MIUI ignore sharedUserId, ou une ancienne version " +
                "du jeu avec une autre signature traîne. Désinstalle complètement le jeu " +
                "(paramètres > applis > $targetPackage > désinstaller) puis relance le hub."
            )
        }

        // Step 2: wait for PackageManager / MediaProvider caches to include the new package
        repeat(20) { i ->
            val pkgsForOurUid = pm.getPackagesForUid(ourUid) ?: emptyArray()
            if (targetPackage in pkgsForOurUid) return@repeat
            if (i == 19) throw IOException(
                "PackageManager ne lie toujours pas $targetPackage à notre UID après 10s. " +
                "Notre UID = $ourUid, packages = ${pkgsForOurUid.joinToString()}"
            )
            delay(500)
        }

        // Step 3: try the official path via the game's PackageContext (works because same UID)
        val obbDirFromContext = try {
            val gameCtx = context.createPackageContext(targetPackage, Context.CONTEXT_IGNORE_SECURITY)
            gameCtx.obbDir
        } catch (e: Exception) {
            null
        }

        // Step 4: candidate paths
        val candidates = listOfNotNull(
            obbDirFromContext,
            File(Environment.getExternalStorageDirectory(), "Android/obb/$targetPackage")
        ).distinctBy { it.absolutePath }

        for (dir in candidates) {
            if (dir.exists() && dir.canWrite()) return dir
            if (!dir.exists()) {
                // mkdirs() may transiently fail right after install; retry a few times.
                for (attempt in 0 until 6) {
                    if (dir.mkdirs() || dir.exists()) {
                        if (dir.canWrite()) return dir
                    }
                    delay(500)
                }
            }
        }

        val tried = candidates.joinToString { it.absolutePath }
        throw IOException(
            "Impossible de créer/écrire dans le dossier OBB. Chemins testés : $tried. " +
            "Cause probable : restriction HyperOS sur l'écriture inter-paquet même avec UID partagé."
        )
    }
}
