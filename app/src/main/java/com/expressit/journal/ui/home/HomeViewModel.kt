package com.expressit.journal.ui.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expressit.journal.ExpressItApp
import com.expressit.journal.data.JournalEntry
import com.expressit.journal.data.JournalExporter
import com.expressit.journal.data.JournalRepository
import com.expressit.journal.speech.TranscriptionSettings
import com.expressit.journal.speech.WhisperModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val application: Application,
    private val repository: JournalRepository,
    val whisperModel: WhisperModel,
    private val settings: TranscriptionSettings
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _visibleMonth = MutableStateFlow(YearMonth.now())
    val visibleMonth: StateFlow<YearMonth> = _visibleMonth.asStateFlow()

    /** Entries for the selected day, oldest first. */
    val entries: StateFlow<List<JournalEntry>> = _selectedDate
        .flatMapLatest { repository.entriesForDay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Days in the visible month that have at least one entry (calendar dots). */
    val daysWithEntries: StateFlow<Set<LocalDate>> = _visibleMonth
        .flatMapLatest { repository.daysWithEntries(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val preferWhisper: StateFlow<Boolean> = settings.preferWhisper
    val modelDownloadState: StateFlow<WhisperModel.DownloadState> = whisperModel.downloadState

    /** null = idle, true = shared, false = no entries in range. */
    private val _exportResult = MutableStateFlow<Boolean?>(null)
    val exportResult: StateFlow<Boolean?> = _exportResult.asStateFlow()

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _visibleMonth.value = YearMonth.from(date)
    }

    fun showPreviousMonth() {
        _visibleMonth.value = _visibleMonth.value.minusMonths(1)
    }

    fun showNextMonth() {
        _visibleMonth.value = _visibleMonth.value.plusMonths(1)
    }

    fun showMonth(month: YearMonth) {
        _visibleMonth.value = month
    }

    fun goToToday() = selectDate(LocalDate.now())

    fun delete(entry: JournalEntry) {
        viewModelScope.launch { repository.delete(entry) }
    }

    fun export(start: LocalDate, end: LocalDate) {
        viewModelScope.launch {
            _exportResult.value = JournalExporter.export(application, repository, start, end)
        }
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    fun setPreferWhisper(value: Boolean) = settings.setPreferWhisper(value)

    fun downloadModel() {
        viewModelScope.launch { whisperModel.download() }
    }

    fun deleteModel() = whisperModel.delete()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ExpressItApp
                HomeViewModel(app, app.repository, app.whisperModel, app.transcriptionSettings)
            }
        }
    }
}
