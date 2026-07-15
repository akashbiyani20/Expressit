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
import com.expressit.journal.speech.SpeechRecognizerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Drives the record-and-review screen for both new entries and edits.
 *
 * Finalized speech segments are appended to [text]; the live hypothesis stays
 * in [SpeechRecognizerManager.state] so the user's saved words are never
 * mutated by an unconfirmed guess.
 */
class EntryViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val repository: JournalRepository
) : ViewModel() {

    val date: LocalDate = LocalDate.ofEpochDay(
        savedStateHandle.get<Long>("epochDay") ?: LocalDate.now().toEpochDay()
    )
    private val entryId: Long = savedStateHandle.get<Long>("entryId") ?: -1L
    private var existingEntry: JournalEntry? = null

    val speech = SpeechRecognizerManager(application)

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        speech.onSegmentFinalized = { segment -> appendSegment(segment) }
        if (entryId > 0) {
            viewModelScope.launch {
                repository.entryById(entryId)?.let { entry ->
                    existingEntry = entry
                    _text.value = entry.text
                }
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
    }

    fun onTextChanged(value: String) {
        _text.value = value
    }

    fun toggleListening() {
        if (speech.state.value.isListening) speech.stopListening() else speech.startListening()
    }

    fun save() {
        val content = _text.value.trim()
        if (content.isEmpty() || _saved.value) return
        viewModelScope.launch {
            val existing = existingEntry
            if (existing != null) {
                repository.updateText(existing, content)
            } else {
                repository.create(date, content)
            }
            _saved.value = true
        }
    }

    override fun onCleared() {
        speech.destroy()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ExpressItApp
                EntryViewModel(app, createSavedStateHandle(), app.repository)
            }
        }
    }
}
