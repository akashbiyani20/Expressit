package com.expressit.journal.speech

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tiny preference store for the transcription engine choice. */
class TranscriptionSettings(context: Context) {

    private val prefs = context.getSharedPreferences("expressit_settings", Context.MODE_PRIVATE)

    private val _preferWhisper = MutableStateFlow(prefs.getBoolean(KEY_PREFER_WHISPER, true))
    val preferWhisper: StateFlow<Boolean> = _preferWhisper.asStateFlow()

    fun setPreferWhisper(value: Boolean) {
        prefs.edit().putBoolean(KEY_PREFER_WHISPER, value).apply()
        _preferWhisper.value = value
    }

    private companion object {
        const val KEY_PREFER_WHISPER = "prefer_whisper"
    }
}
