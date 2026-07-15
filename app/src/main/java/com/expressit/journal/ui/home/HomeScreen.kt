package com.expressit.journal.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expressit.journal.R
import com.expressit.journal.data.JournalEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import java.time.format.TextStyle as JavaTextStyle

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNewEntry: (LocalDate) -> Unit,
    onOpenEntry: (LocalDate, Long) -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val visibleMonth by viewModel.visibleMonth.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val daysWithEntries by viewModel.daysWithEntries.collectAsStateWithLifecycle()

    val haptics = LocalHapticFeedback.current
    var entryPendingDelete by remember { mutableStateOf<JournalEntry?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNewEntry(selectedDate)
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.new_entry)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MonthHeader(
                month = visibleMonth,
                showTodayButton = selectedDate != LocalDate.now(),
                onPrevious = viewModel::showPreviousMonth,
                onNext = viewModel::showNextMonth,
                onToday = viewModel::goToToday
            )

            MonthCalendar(
                month = visibleMonth,
                selectedDate = selectedDate,
                daysWithEntries = daysWithEntries,
                onSelect = viewModel::selectDate,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            DayHeader(date = selectedDate, entryCount = entries.size)

            if (entries.isEmpty()) {
                EmptyDayState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        EntryCard(
                            entry = entry,
                            onClick = { onOpenEntry(selectedDate, entry.id) },
                            onDelete = { entryPendingDelete = entry }
                        )
                    }
                }
            }
        }
    }

    entryPendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryPendingDelete = null },
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.delete_dialog_title)) },
            text = { Text(stringResource(R.string.delete_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(entry)
                    entryPendingDelete = null
                }) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { entryPendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    showTodayButton: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 12.dp, top = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = month,
            transitionSpec = {
                (fadeIn(tween(220)) togetherWith fadeOut(tween(120)))
            },
            label = "monthTitle",
            modifier = Modifier.weight(1f)
        ) { current ->
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = current.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = current.year.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }

        if (showTodayButton) {
            TextButton(onClick = onToday) {
                Text(
                    stringResource(R.string.today),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "Previous month",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onNext) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Next month",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    selectedDate: LocalDate,
    daysWithEntries: Set<LocalDate>,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstDayOfWeek = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    val weekDays = remember(firstDayOfWeek) {
        (0L..6L).map { firstDayOfWeek.plus(it) }
    }

    Column(modifier = modifier) {
        Row(Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day.getDisplayName(JavaTextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                )
            }
        }

        AnimatedContent(
            targetState = month,
            transitionSpec = {
                val forward = targetState > initialState
                val slideIn = slideInHorizontally(tween(280)) { full ->
                    if (forward) full / 3 else -full / 3
                } + fadeIn(tween(280))
                val slideOut = slideOutHorizontally(tween(200)) { full ->
                    if (forward) -full / 3 else full / 3
                } + fadeOut(tween(200))
                slideIn togetherWith slideOut
            },
            label = "monthGrid"
        ) { currentMonth ->
            val weeks = remember(currentMonth, firstDayOfWeek) {
                monthGrid(currentMonth, firstDayOfWeek)
            }
            Column {
                weeks.forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (date != null) {
                                    DayCell(
                                        date = date,
                                        isSelected = date == selectedDate,
                                        isToday = date == LocalDate.now(),
                                        hasEntries = date in daysWithEntries,
                                        onClick = { onSelect(date) }
                                    )
                                } else {
                                    Spacer(Modifier.size(44.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasEntries: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "dayScale"
    )
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onBackground
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(background)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = textColor
            )
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            !hasEntries -> androidx.compose.ui.graphics.Color.Transparent
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
            )
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, entryCount: Int) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = date.format(formatter),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (entryCount > 0) {
            Text(
                text = if (entryCount == 1) {
                    stringResource(R.string.entry_count_one)
                } else {
                    stringResource(R.string.entry_count_many, entryCount)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EntryCard(
    entry: JournalEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()) }
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.time.format(timeFormatter).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.delete_entry),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer(alpha = 0.7f)
                    )
                }
            }
            Text(
                text = entry.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
    }
}

@Composable
private fun EmptyDayState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp, vertical = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.empty_day_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.empty_day_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun monthGrid(month: YearMonth, firstDayOfWeek: DayOfWeek): List<List<LocalDate?>> {
    val offset = ((month.atDay(1).dayOfWeek.value - firstDayOfWeek.value) + 7) % 7
    val cells = buildList {
        repeat(offset) { add(null) }
        for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
        while (size % 7 != 0) add(null)
    }
    return cells.chunked(7)
}
