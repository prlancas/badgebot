package com.badgebot.controller.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/** Helpers for exporting generated content (markers, recordings) via the system share sheet. */
object Sharing {

    private fun sharedDir(context: Context): File =
        File(context.cacheDir, "shared").apply { mkdirs() }

    private fun uriFor(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Writes [bitmap] to a PNG in the cache and launches a share chooser. */
    fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String, title: String) {
        val file = File(sharedDir(context), fileName)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = uriFor(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Writes [text] to a file in the cache and launches a share chooser. */
    fun shareText(context: Context, text: String, fileName: String, title: String) {
        val file = File(sharedDir(context), fileName)
        file.writeText(text)
        val uri = uriFor(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
