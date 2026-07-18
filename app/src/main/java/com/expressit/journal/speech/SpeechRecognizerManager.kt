package com.expressit.journal.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

/**
 * Wraps the platform [SpeechRecognizer] and turns it into a simple
 * start / stop dictation session.
 *
 * The platform recognizer finalizes a result after every pause in speech.
 * To let people talk naturally (with pauses to think), this manager
 * automatically restarts listening after each finalized segment until the
 * user explicitly stops. Finalized segments are delivered through
 * [onSegmentFinalized]; the in-flight hypothesis is exposed via [state].
 */
class SpeechRecognizerManager(private val context: Context) {

    data class SpeechState(
        val isListening: Boolean = false,
        val partialText: String = "",
        val soundLevel: Float = 0f, // normalized 0..1, for the mic animation
        val error: SpeechError? = null
    )

    enum class SpeechError { UNAVAILABLE, GENERIC }

    private val _state = MutableStateFlow(SpeechState())
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    /** Called with each finalized chunk of recognized speech. */
    var onSegmentFinalized: ((String) -> Unit)? = null

    private var recognizer: SpeechRecognizer? = null
    private var stopRequested = false

    val isRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening() {
        if (!isRecognitionAvailable) {
            _state.update { it.copy(error = SpeechError.UNAVAILABLE) }
            return
        }
        stopRequested = false
        _state.update { it.copy(error = null, partialText = "") }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(listener)
            }
        }
        recognizer?.startListening(recognitionIntent())
    }

    /** Gracefully finish: the recognizer will still deliver its final result. */
    fun stopListening() {
        stopRequested = true
        recognizer?.stopListening()
    }

    fun destroy() {
        stopRequested = true
        recognizer?.destroy()
        recognizer = null
        _state.update { SpeechState() }
    }

    private fun recognitionIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Automatic punctuation where the device's recognizer supports it.
            putExtra(
                RecognizerIntent.EXTRA_ENABLE_FORMATTING,
                RecognizerIntent.FORMATTING_OPTIMIZE_QUALITY
            )
        }
    }

    private fun restartIfNeeded() {
        if (stopRequested) {
            _state.update { it.copy(isListening = false, partialText = "", soundLevel = 0f) }
        } else {
            recognizer?.startListening(recognitionIntent())
        }
    }

    private val listener = object : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) {
            _state.update { it.copy(isListening = true, error = null) }
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) {
            // Typical range is roughly -2..10 dB; normalize for the UI.
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _state.update { it.copy(soundLevel = normalized) }
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            when (error) {
                // Silence or nothing intelligible — keep the session alive.
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> restartIfNeeded()

                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> Unit // transient; ignore

                else -> {
                    _state.update {
                        it.copy(
                            isListening = false,
                            partialText = "",
                            soundLevel = 0f,
                            error = SpeechError.GENERIC
                        )
                    }
                    stopRequested = true
                }
            }
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
                .trim()
            if (text.isNotEmpty()) onSegmentFinalized?.invoke(text)
            _state.update { it.copy(partialText = "") }
            restartIfNeeded()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            _state.update { it.copy(partialText = text) }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }
}
