package com.devfahim00.openridmik

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

/**
 * Minimal, zero-dependency ContentProvider that serves ONLY the downloaded
 * in-app update APK from cacheDir to Android's package installer (read-only).
 */
class ApkFileProvider : ContentProvider() {

    companion object {
        const val UPDATE_APK_NAME = "myavro_update.apk"

        fun apkFile(cacheDir: File): File = File(cacheDir, UPDATE_APK_NAME)

        fun apkUri(packageName: String): Uri =
            Uri.parse("content://$packageName.apkprovider/$UPDATE_APK_NAME")
    }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = "application/vnd.android.package-archive"

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw SecurityException("Read-only provider")
        val ctx = context ?: throw FileNotFoundException("No context")
        val file = apkFile(ctx.cacheDir)
        if (!file.exists()) throw FileNotFoundException("Update APK not found")
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
