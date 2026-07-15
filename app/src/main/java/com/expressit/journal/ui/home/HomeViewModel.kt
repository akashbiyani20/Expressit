package com.expressit.journal.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.expressit.journal.ExpressItApp
import com.expressit.journal.data.JournalEntry
import com.expressit.journal.data.JournalRepository
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
class HomeViewModel(private val repository: JournalRepository) : ViewModel() {

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

    fun goToToday() = selectDate(LocalDate.now())

    fun delete(entry: JournalEntry) {
        viewModelScope.launch { repository.delete(entry) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ExpressItApp
                HomeViewModel(app.repository)
            }
        }
    }
}
