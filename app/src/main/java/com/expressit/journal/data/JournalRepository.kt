package com.expressit.journal.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

/**
 * Single source of truth for journal entries. Wraps the DAO and translates
 * between java.time types and their persisted representations.
 */
class JournalRepository(private val dao: JournalDao) {

    fun entriesForDay(date: LocalDate): Flow<List<JournalEntry>> =
        dao.entriesForDay(date.toEpochDay())

    fun daysWithEntries(month: YearMonth): Flow<Set<LocalDate>> =
        dao.daysWithEntries(
            month.atDay(1).toEpochDay(),
            month.atEndOfMonth().toEpochDay()
        ).map { days -> days.map(LocalDate::ofEpochDay).toSet() }

    suspend fun entryById(id: Long): JournalEntry? = dao.entryById(id)

    /**
     * Creates an entry for [date]. The entry keeps the current wall-clock time so
     * back-dated entries still read naturally ("9:41 PM").
     */
    suspend fun create(date: LocalDate, text: String) {
        val createdAt = LocalDateTime.of(date, LocalTime.now())
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        dao.insert(JournalEntry(epochDay = date.toEpochDay(), createdAt = createdAt, text = text))
    }

    suspend fun updateText(entry: JournalEntry, text: String) =
        dao.update(entry.copy(text = text))

    suspend fun delete(entry: JournalEntry) = dao.delete(entry)
}
