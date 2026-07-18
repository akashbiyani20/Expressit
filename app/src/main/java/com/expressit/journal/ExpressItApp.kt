package com.expressit.journal

import android.app.Application
import com.expressit.journal.data.JournalDatabase
import com.expressit.journal.data.JournalRepository
import com.expressit.journal.speech.TranscriptionSettings
import com.expressit.journal.speech.WhisperModel

/**
 * Lightweight service locator. Deliberate choice for an app this size —
 * a handful of singletons, no DI framework overhead. Swap for Hilt if the
 * graph grows.
 */
class ExpressItApp : Application() {

    val repository: JournalRepository by lazy {
        JournalRepository(JournalDatabase.get(this).journalDao())
    }

    val whisperModel: WhisperModel by lazy { WhisperModel(this) }

    val transcriptionSettings: TranscriptionSettings by lazy { TranscriptionSettings(this) }
}
