package com.expressit.journal.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Exports a date range of entries as a clean Markdown file and hands it to the
 * system share sheet — ready to paste into an AI chat, email to a therapist,
 * or save anywhere.
 */
object JournalExporter {

    suspend fun export(
        context: Context,
        repository: JournalRepository,
        start: LocalDate,
        end: LocalDate
    ): Boolean = withContext(Dispatchers.IO) {
        val entries = repository.entriesBetween(start, end)
        if (entries.isEmpty()) return@withContext false

        val locale = Locale.getDefault()
        val rangeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", locale)
        val dayFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale)
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", locale)

        val content = buildString {
            appendLine("# ExpressIt Journal")
            appendLine()
            appendLine("_${start.format(rangeFormatter)} — ${end.format(rangeFormatter)}_")
            appendLine()

            var currentDay: LocalDate? = null
            for (entry in entries) {
                if (entry.date != currentDay) {
                    currentDay = entry.date
                    appendLine("## ${entry.date.format(dayFormatter)}")
                    appendLine()
                }
                val heading = entry.title
                    ?.takeIf { it.isNotBlank() }
                    ?.let { "${entry.time.format(timeFormatter)} — $it" }
                    ?: entry.time.format(timeFormatter)
                appendLine("### $heading")
                appendLine()
                appendLine(entry.text.trim())
                appendLine()
            }
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "ExpressIt_${start}_$end.md"
        val file = File(exportDir, fileName)
        file.writeText(content)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "ExpressIt journal $start to $end")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        true
    }
}
