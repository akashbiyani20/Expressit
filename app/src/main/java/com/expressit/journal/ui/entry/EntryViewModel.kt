package com.expressit.journal.ui.entry

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expressit.journal.ExpressItApp
import com.expressit.journal.data.JournalEntry
import com.expressit.journal.data.JournalRepository
import com.expressit.journal.speech.PcmRecorder
import com.expressit.journal.speech.SpeechRecognizerManager
import com.expressit.journal.speech.TranscriptionSettings
import com.expressit.journal.speech.WhisperModel
import com.expressit.journal.util.TitleGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Drives the record-and-review screen for both new entries and edits.
 *
 * Two capture engines share one state machine:
 *  - WHISPER (preferred, when the model is downloaded): records raw audio in
 *    memory, then transcribes it on-device with punctuation. States flow
 *    Idle -> Listening -> Transcribing -> Idle.
 *  - SYSTEM (fallback): the platform recognizer with live partial results.
 *    States flow Idle -> Listening -> Idle.
 */
class EntryViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val repository: JournalRepository,
    private val whisperModel: WhisperModel,
    private val settings: TranscriptionSettings
) : ViewModel() {

    enum class Engine { WHISPER, SYSTEM }

    sealed interface CaptureState {
        data object Idle : CaptureState
        data class Listening(val engine: Engine) : CaptureState
        data object Transcribing : CaptureState
    }

    val date: LocalDate = LocalDate.ofEpochDay(
        savedStateHandle.get<Long>("epochDay") ?: LocalDate.now().toEpochDay()
    )
    private val entryId: Long = savedStateHandle.get<Long>("entryId") ?: -1L
    private var existingEntry: JournalEntry? = null

    private val systemSpeech = SpeechRecognizerManager(application)
    private val recorder = PcmRecorder()

    private val _captureState = MutableStateFlow<CaptureState>(CaptureState.Idle)
    val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private var titleEditedManually = false
    private var titleSeed = 0

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        systemSpeech.onSegmentFinalized = { segment -> appendSegment(segment) }
        viewModelScope.launch {
            systemSpeech.state.collect { state ->
                if (_captureState.value is CaptureState.Listening &&
                    (_captureState.value as CaptureState.Listening).engine == Engine.SYSTEM
                ) {
                    _soundLevel.value = state.soundLevel
                    _partialText.value = state.partialText
                    if (state.error != null) {
                        _captureState.value = CaptureState.Idle
                        _error.value = "system"
                    } else if (!state.isListening && state.partialText.isEmpty()) {
                        // Recognizer settled after a requested stop.
                    }
                }
            }
        }
        if (entryId > 0) {
            viewModelScope.launch {
                repository.entryById(entryId)?.let { entry ->
                    existingEntry = entry
                    _text.value = entry.text
                    _title.value = entry.title.orEmpty()
                    titleEditedManually = entry.title != null
                }
            }
        }
    }

    private val activeEngine: Engine
        get() = if (settings.preferWhisper.value && whisperModel.isReady) Engine.WHISPER
        else Engine.SYSTEM

    fun toggleCapture() {
        _error.value = null
        when (val state = _captureState.value) {
            is CaptureState.Idle -> startCapture()
            is CaptureState.Listening -> stopCapture(state.engine)
            is CaptureState.Transcribing -> Unit // busy; ignore taps
        }
    }

    private fun startCapture() {
        when (activeEngine) {
            Engine.WHISPER -> {
                recorder.start(
                    onLevel = { level -> _soundLevel.value = level },
                    onError = {
                        _captureState.value = CaptureState.Idle
                        _error.value = "record"
                    }
                )
                _captureState.value = CaptureState.Listening(Engine.WHISPER)
            }

            Engine.SYSTEM -> {
                if (!systemSpeech.isRecognitionAvailable) {
                    _error.value = "unavailable"
                    return
                }
                systemSpeech.startListening()
                _captureState.value = CaptureState.Listening(Engine.SYSTEM)
            }
        }
    }

    private fun stopCapture(engine: Engine) {
        when (engine) {
            Engine.WHISPER -> {
                _captureState.value = CaptureState.Transcribing
                _soundLevel.value = 0f
                viewModelScope.launch {
                    val samples = recorder.stop()
                    if (samples.size < PcmRecorder.SAMPLE_RATE / 2) {
                        // Under half a second — nothing meaningful was said.
                        _captureState.value = CaptureState.Idle
                        return@launch
                    }
                    try {
                        val result = whisperModel.transcribe(samples)
                        if (result.isNotBlank()) appendSegment(result)
                    } catch (e: Exception) {
                        _error.value = "transcribe"
                    }
                    _captureState.value = CaptureState.Idle
                }
            }

            Engine.SYSTEM -> {
                systemSpeech.stopListening()
                _partialText.value = ""
                _soundLevel.value = 0f
                _captureState.value = CaptureState.Idle
            }
        }
    }

    private fun appendSegment(segment: String) {
        _text.update { current ->
            when {
                current.isBlank() -> segment.replaceFirstChar { it.uppercase() }
                else -> current.trimEnd() + " " + segment
            }
        }
        if (!titleEditedManually) {
            _title.value = TitleGenerator.generate(_text.value, titleSeed)
        }
    }

    fun onTextChanged(value: String) {
        _text.value = value
    }

    fun onTitleChanged(value: String) {
        titleEditedManually = true
        _title.value = value
    }

    fun regenerateTitle() {
        titleSeed += 1
        titleEditedManually = false
        _title.value = TitleGenerator.generate(_text.value, titleSeed)
    }

    fun save() {
        val content = _text.value.trim()
        if (content.isEmpty() || _saved.value) return
        val heading = _title.value.trim()
            .ifEmpty { TitleGenerator.generate(content) }
            .takeIf { it.isNotEmpty() }
        viewModelScope.launch {
            val existing = existingEntry
            if (existing != null) {
                repository.update(existing, content, heading)
            } else {
                repository.create(date, content, heading)
            }
            _saved.value = true
        }
    }

    override fun onCleared() {
        systemSpeech.destroy()
        if (recorder.isRecording) recorder.stop()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ExpressItApp
                EntryViewModel(
                    app,
                    createSavedStateHandle(),
                    app.repository,
                    app.whisperModel,
                    app.transcriptionSettings
                )
            }
        }
    }
}
