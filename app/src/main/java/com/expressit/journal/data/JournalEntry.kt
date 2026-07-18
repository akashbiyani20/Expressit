package com.expressit.journal.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * A single journal entry.
 *
 * @param epochDay   The calendar day the entry belongs to ([LocalDate.toEpochDay]).
 * @param createdAt  Creation moment in epoch millis — used to order entries within a day
 *                   and to show the entry's time.
 */
@Entity(tableName = "journal_entries", indices = [Index("epochDay")])
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val createdAt: Long,
    val text: String,
    val title: String? = null
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(epochDay)
    val time: LocalTime
        get() = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalTime()
}
