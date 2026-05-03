package com.aciderix.obbinstaller

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.InputStream

sealed class FileSource {
    abstract val displayName: String
    abstract fun openStream(context: Context): InputStream
    abstract fun length(context: Context): Long

    data class Asset(val name: String) : FileSource() {
        override val displayName: String get() = name
        override fun openStream(context: Context): InputStream = context.assets.open(name)
        override fun length(context: Context): Long = context.assets.openFd(name).use { it.length }
    }

    data class UriSource(val uri: Uri, override val displayName: String) : FileSource() {
        override fun openStream(context: Context): InputStream =
            context.contentResolver.openInputStream(uri) ?: error("cannot open $uri")
        override fun length(context: Context): Long {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) return c.getLong(0)
            }
            return -1L
        }
    }
}

fun resolveDisplayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst() && !c.isNull(0)) return c.getString(0)
    }
    return uri.lastPathSegment ?: "file"
}
