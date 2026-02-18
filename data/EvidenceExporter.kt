
package com.spectral.ghost.data.core

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class EvidenceExporter(private val context: Context) {

    fun finalizeEvidence(tempFile: File, fullHash: String): String {
        // Generate Short Hash (First 8 chars)
        val shortHash = fullHash.take(8).uppercase()
        val finalFileName = "EVIDENCIA_$shortHash.mp4"

        // Export to Public Gallery
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, finalFileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/SpectralEvidence")
                put(MediaStore.Video.Media.IS_PENDING, 1) // Mark as pending while writing
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let { outputUri ->
            resolver.openOutputStream(outputUri).use { outputStream ->
                FileInputStream(tempFile).use { inputStream ->
                    inputStream.copyTo(outputStream!!)
                }
            }

            // Finish pending state
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(outputUri, contentValues, null, null)
            }
            
            // Clean up temp file
            tempFile.delete()
            return finalFileName
        }
        
        return "ERROR_EXPORT"
    }
}
