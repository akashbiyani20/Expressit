package com.expressit.journal.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Query("SELECT * FROM journal_entries WHERE epochDay = :epochDay ORDER BY createdAt ASC")
    fun entriesForDay(epochDay: Long): Flow<List<JournalEntry>>

    @Query(
        "SELECT DISTINCT epochDay FROM journal_entries " +
            "WHERE epochDay BETWEEN :startDay AND :endDay"
    )
    fun daysWithEntries(startDay: Long, endDay: Long): Flow<List<Long>>

    @Query(
        "SELECT * FROM journal_entries WHERE epochDay BETWEEN :startDay AND :endDay " +
            "ORDER BY epochDay ASC, createdAt ASC"
    )
    suspend fun entriesBetween(startDay: Long, endDay: Long): List<JournalEntry>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun entryById(id: Long): JournalEntry?

    @Insert
    suspend fun insert(entry: JournalEntry): Long

    @Update
    suspend fun update(entry: JournalEntry)

    @Delete
    suspend fun delete(entry: JournalEntry)
}
