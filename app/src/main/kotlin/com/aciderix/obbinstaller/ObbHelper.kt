package com.aciderix.obbinstaller

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ObbHelper {

    private const val EXTERNAL_AUTHORITY = "com.android.externalstorage.documents"

    /**
     * Builds a content URI pre-pointing the SAF picker at Android/obb/<targetPackage>.
     * Used as DocumentsContract.EXTRA_INITIAL_URI so the user only has to tap "Use this folder".
     */
    fun buildInitialUriFor(targetPackage: String): Uri {
        val docId = "primary:Android/obb/$targetPackage"
        return DocumentsContract.buildDocumentUri(EXTERNAL_AUTHORITY, docId)
    }

    fun openObbPickerIntent(targetPackage: String): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, buildInitialUriFor(targetPackage))
        }

    fun persistPermission(context: Context, treeUri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(treeUri, flags) }
    }

    /**
     * Resolves the target Android/obb/<package> directory from whatever the user granted.
     * Handles three cases: user granted the package folder itself, granted Android/obb,
     * or granted Android (and we descend / create as needed).
     */
    fun resolveTargetDir(
        context: Context,
        treeUri: Uri,
        targetPackage: String
    ): DocumentFile? {
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val name = tree.name?.lowercase()
        return when {
            tree.name == targetPackage -> tree
            name == "obb" -> tree.findFile(targetPackage) ?: tree.createDirectory(targetPackage)
            name == "android" -> {
                val obb = tree.findFile("obb") ?: tree.createDirectory("obb") ?: return null
                obb.findFile(targetPackage) ?: obb.createDirectory(targetPackage)
            }
            else -> {
                // Unknown root - try descending obb/<pkg> if present, else create child with pkg name
                tree.findFile(targetPackage)
                    ?: tree.findFile("obb")?.let { it.findFile(targetPackage) ?: it.createDirectory(targetPackage) }
                    ?: tree.createDirectory(targetPackage)
            }
        }
    }

    suspend fun copyObb(
        context: Context,
        source: FileSource,
        destDir: DocumentFile,
        filename: String,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            destDir.findFile(filename)?.delete()
            val dest = destDir.createFile("application/octet-stream", filename)
                ?: error("could not create destination file")
            // createFile may sanitize the name; ensure it matches
            if (dest.name != filename) {
                // try rename
                runCatching { dest.renameTo(filename) }
            }
            val total = source.length(context).coerceAtLeast(1L)
            context.contentResolver.openOutputStream(dest.uri, "w")?.use { output ->
                source.openStream(context).use { input ->
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
            } ?: error("cannot open output stream")
            Unit
        }
    }
}
