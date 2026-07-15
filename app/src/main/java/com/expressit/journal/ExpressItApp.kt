package com.expressit.journal

import android.app.Application
import com.expressit.journal.data.JournalDatabase
import com.expressit.journal.data.JournalRepository

/**
 * Lightweight service locator. Deliberate choice for an app this size —
 * one repository, no DI framework overhead. Swap for Hilt if the graph grows.
 */
class ExpressItApp : Application() {

    val repository: JournalRepository by lazy {
        JournalRepository(JournalDatabase.get(this).journalDao())
    }
}
