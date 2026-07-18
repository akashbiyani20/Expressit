package com.expressit.journal.speech

import android.content.Context
import com.whispercpp.whisper.WhisperContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Owns the on-device Whisper model: download, storage, and transcription.
 *
 * The multilingual "base" model (~148 MB) is fetched once from Hugging Face
 * into app-private storage and everything afterwards runs fully offline.
 */
class WhisperModel(private val context: Context) {

    sealed interface DownloadState {
        data object NotDownloaded : DownloadState
        data class Downloading(val progress: Float) : DownloadState
        data object Ready : DownloadState
        data object Failed : DownloadState
    }

    private val _downloadState = MutableStateFlow<DownloadState>(
        if (modelFile.exists()) DownloadState.Ready else DownloadState.NotDownloaded
    )
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var whisperContext: WhisperContext? = null

    val isReady: Boolean get() = modelFile.exists()

    private val modelFile: File
        get() = File(File(context.filesDir, "models").apply { mkdirs() }, MODEL_FILE)

    suspend fun download() = withContext(Dispatchers.IO) {
        if (modelFile.exists()) {
            _downloadState.value = DownloadState.Ready
            return@withContext
        }
        _downloadState.value = DownloadState.Downloading(0f)
        val tmp = File(modelFile.parentFile, "$MODEL_FILE.part")
        try {
            val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15_000
                readTimeout = 30_000
            }
            val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: APPROX_SIZE_BYTES
            connection.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    val buffer = ByteArray(256 * 1024)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        _downloadState.value =
                            DownloadState.Downloading((copied.toFloat() / totalBytes).coerceIn(0f, 0.99f))
                    }
                }
            }
            if (!tmp.renameTo(modelFile)) throw IllegalStateException("rename failed")
            _downloadState.value = DownloadState.Ready
        } catch (e: Exception) {
            tmp.delete()
            _downloadState.value = DownloadState.Failed
        }
    }

    fun delete() {
        releaseBlocking()
        modelFile.delete()
        _downloadState.value = DownloadState.NotDownloaded
    }

    /** Transcribes 16 kHz mono float samples to punctuated text. */
    suspend fun transcribe(samples: FloatArray): String {
        val ctx = whisperContext ?: WhisperContext
            .createContextFromFile(modelFile.absolutePath)
            .also { whisperContext = it }
        return ctx.transcribeData(samples, printTimestamp = false).trim()
    }

    private fun releaseBlocking() {
        val ctx = whisperContext ?: return
        whisperContext = null
        kotlinx.coroutines.runBlocking { ctx.release() }
    }

    fun close() = releaseBlocking()

    companion object {
        private const val MODEL_FILE = "ggml-base.bin"
        private const val MODEL_URL =
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin"
        private const val APPROX_SIZE_BYTES = 148_000_000L
        const val APPROX_SIZE_LABEL = "148 MB"
    }
}
