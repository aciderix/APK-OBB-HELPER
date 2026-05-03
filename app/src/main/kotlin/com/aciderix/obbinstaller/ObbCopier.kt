package com.aciderix.obbinstaller

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ObbCopier {

    /**
     * Copies the OBB into /storage/emulated/0/Android/obb/<targetPackage>/<filename>.
     *
     * This is allowed without SAF because the hub shares UID with the patched game,
     * so the file system treats us as the game's owner.
     */
    suspend fun copy(
        context: Context,
        source: FileSource,
        targetPackage: String,
        filename: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val obbRoot = File(Environment.getExternalStorageDirectory(), "Android/obb")
            val pkgDir = File(obbRoot, targetPackage)
            if (!pkgDir.exists() && !pkgDir.mkdirs()) {
                error("Cannot create $pkgDir (UID share may not be active - did you uninstall the previous game?)")
            }
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
}
