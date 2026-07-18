package com.expressit.journal.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.expressit.journal.R
import com.expressit.journal.speech.WhisperModel
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.Locale
import java.time.format.TextStyle as JavaTextStyle

/**
 * Compact month + year jumper. Tap the year arrows to change year, tap a
 * month to go straight there.
 */
@Composable
fun MonthYearPickerDialog(
    initial: YearMonth,
    onPick: (YearMonth) -> Unit,
    onDismiss: () -> Unit
) {
    var year by remember { mutableStateOf(initial.year) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        title = null,
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { year-- }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.previous_year),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { year++ }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.next_year),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                val today = YearMonth.now()
                for (rowIndex in 0 until 4) {
                    Row(Modifier.fillMaxWidth()) {
                        for (columnIndex in 0 until 3) {
                            val month = Month.of(rowIndex * 3 + columnIndex + 1)
                            val target = YearMonth.of(year, month)
                            val isCurrent = target == today
                            val isInitial = target == initial
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(
                                        when {
                                            isInitial -> MaterialTheme.colorScheme.primary
                                            isCurrent -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    )
                                    .clickable { onPick(target) }
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = month.getDisplayName(
                                        JavaTextStyle.SHORT, Locale.getDefault()
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = when {
                                        isInitial -> MaterialTheme.colorScheme.onPrimary
                                        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

/** Date-range picker feeding the Markdown export + share sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportRangeDialog(
    onExport: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDateRangePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                enabled = state.selectedStartDateMillis != null,
                onClick = {
                    val startMillis = state.selectedStartDateMillis ?: return@Button
                    val endMillis = state.selectedEndDateMillis ?: startMillis
                    val start = Instant.ofEpochMilli(startMillis)
                        .atZone(ZoneOffset.UTC).toLocalDate()
                    val end = Instant.ofEpochMilli(endMillis)
                        .atZone(ZoneOffset.UTC).toLocalDate()
                    onExport(start, end)
                }
            ) {
                Text(stringResource(R.string.share_export))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    ) {
        DateRangePicker(
            state = state,
            title = {
                Text(
                    text = stringResource(R.string.export_title),
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                )
            },
            showModeToggle = true,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Whisper model management + engine preference. */
@Composable
fun TranscriptionDialog(
    downloadState: WhisperModel.DownloadState,
    preferWhisper: Boolean,
    onDownload: () -> Unit,
    onDeleteModel: () -> Unit,
    onPreferWhisperChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(R.string.transcription_title)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.transcription_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                when (downloadState) {
                    is WhisperModel.DownloadState.NotDownloaded -> {
                        Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(
                                    R.string.download_model,
                                    WhisperModel.APPROX_SIZE_LABEL
                                )
                            )
                        }
                    }

                    is WhisperModel.DownloadState.Downloading -> {
                        Text(
                            text = stringResource(
                                R.string.downloading_model,
                                (downloadState.progress * 100).toInt()
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadState.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    is WhisperModel.DownloadState.Ready -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.model_ready),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onDeleteModel) {
                                Text(
                                    stringResource(R.string.remove_model),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    is WhisperModel.DownloadState.Failed -> {
                        Text(
                            text = stringResource(R.string.download_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.retry_download))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.use_whisper),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = preferWhisper,
                        onCheckedChange = onPreferWhisperChanged,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    )
}
