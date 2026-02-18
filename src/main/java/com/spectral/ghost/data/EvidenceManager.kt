
package com.spectral.ghost.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Surface
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EvidenceManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentVideoFile: File? = null

    // Configuración de Grabación
    private val videoWidth = 1920
    private val videoHeight = 1080
    private val videoBitrate = 10_000_000 // 10 Mbps for high quality

    fun startRecording(surface: Surface) {
        val fileName = "SPECTRAL_EVIDENCE_${System.currentTimeMillis()}.mp4"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        currentVideoFile = File(storageDir, fileName)

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(currentVideoFile)
            setVideoEncodingBitRate(videoBitrate)
            setVideoFrameRate(30)
            setVideoSize(videoWidth, videoHeight)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setInputSurface(surface) // Conectar al Surface de AR/OpenGL
            
            try {
                prepare()
                start()
                println("[EVIDENCE] Recording started: ${currentVideoFile?.absolutePath}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun stopRecording(): EvidenceResult {
        return withContext(Dispatchers.IO) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                
                val file = currentVideoFile ?: return@withContext EvidenceResult.Error("No file generated")
                
                // 1. Calcular Hash SHA-256
                val hash = calculateSHA256(file)
                
                // 2. Guardar en MediaStore (Galería)
                val uri = saveToGallery(file)
                
                EvidenceResult.Success(file, hash, uri.toString())
            } catch (e: Exception) {
                EvidenceResult.Error(e.message ?: "Unknown recording error")
            }
        }
    }

    private fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val inputStream = FileInputStream(file)
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        inputStream.close() 
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun saveToGallery(file: File): android.net.Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/SpectralEvidence")
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
        }
        
        // Nota simplificada: En prod real se copiaría el stream al ContentResolver
        return null // Placeholder Uri
    }
    
    fun shareEvidence(file: File, hash: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "EVIDENCE LOG // SPECTRAL-01\nSHA-256: $hash")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Secure Export Via"))
    }
}

sealed class EvidenceResult {
    data class Success(val file: File, val hash: String, val uri: String) : EvidenceResult()
    data class Error(val message: String) : EvidenceResult()
}
