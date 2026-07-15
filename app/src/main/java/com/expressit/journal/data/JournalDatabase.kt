package com.expressit.journal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [JournalEntry::class], version = 1, exportSchema = false)
abstract class JournalDatabase : RoomDatabase() {

    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var instance: JournalDatabase? = null

        fun get(context: Context): JournalDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    JournalDatabase::class.java,
                    "expressit.db"
                ).build().also { instance = it }
            }
    }
}
