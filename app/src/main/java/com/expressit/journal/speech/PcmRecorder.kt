package com.expressit.journal.speech

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

/**
 * Captures microphone audio as 16 kHz mono float PCM — exactly what Whisper
 * expects. Audio lives only in memory and is discarded after transcription;
 * nothing is ever written to disk.
 */
class PcmRecorder {

    companion object {
        const val SAMPLE_RATE = 16_000
        /** Hard cap so a forgotten recording can't eat memory (~20 min). */
        private const val MAX_SAMPLES = SAMPLE_RATE * 60 * 20
    }

    private val recording = AtomicBoolean(false)
    private var thread: Thread? = null
    private val chunks = ArrayList<ShortArray>()

    val isRecording: Boolean get() = recording.get()

    /**
     * Starts capturing. [onLevel] receives a normalized 0..1 loudness roughly
     * every 100 ms for the mic animation.
     */
    @SuppressLint("MissingPermission") // caller checks RECORD_AUDIO first
    fun start(onLevel: (Float) -> Unit, onError: () -> Unit) {
        if (recording.get()) return
        chunks.clear()

        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer, SAMPLE_RATE / 2)
            )
        } catch (e: Exception) {
            onError(); return
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release(); onError(); return
        }

        recording.set(true)
        record.startRecording()

        thread = Thread {
            val buffer = ShortArray(SAMPLE_RATE / 10) // 100 ms
            var total = 0
            while (recording.get() && total < MAX_SAMPLES) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    synchronized(chunks) { chunks.add(buffer.copyOf(read)) }
                    total += read
                    var sum = 0.0
                    for (i in 0 until read) {
                        val s = buffer[i] / 32768.0
                        sum += s * s
                    }
                    val rms = sqrt(sum / read).toFloat()
                    onLevel((rms * 6f).coerceIn(0f, 1f))
                }
            }
            record.stop()
            record.release()
        }.also { it.start() }
    }

    /** Stops capturing and returns everything heard as float samples in [-1, 1]. */
    fun stop(): FloatArray {
        recording.set(false)
        thread?.join(2_000)
        thread = null
        val snapshot = synchronized(chunks) { chunks.toList().also { chunks.clear() } }
        val total = snapshot.sumOf { it.size }
        val out = FloatArray(total)
        var i = 0
        for (chunk in snapshot) {
            for (s in chunk) out[i++] = (s / 32768.0f).coerceIn(-1f, 1f)
        }
        return out
    }
}
