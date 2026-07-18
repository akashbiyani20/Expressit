package com.expressit.journal.ui.entry

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expressit.journal.R
import com.expressit.journal.ui.entry.EntryViewModel.CaptureState
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EntryScreen(
    viewModel: EntryViewModel,
    onClose: () -> Unit
) {
    val text by viewModel.text.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val captureState by viewModel.captureState.collectAsStateWithLifecycle()
    val soundLevel by viewModel.soundLevel.collectAsStateWithLifecycle()
    val partialText by viewModel.partialText.collectAsStateWithLifecycle()
    val errorKey by viewModel.error.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionDenied = !granted
        if (granted) viewModel.toggleCapture()
    }

    LaunchedEffect(saved) {
        if (saved) onClose()
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault()) }
    val isListening = captureState is CaptureState.Listening
    val isTranscribing = captureState is CaptureState.Transcribing

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 24.dp, top = 8.dp)
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = viewModel.date.format(dateFormatter),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Title (appears once there is something to title)
            AnimatedVisibility(
                visible = text.isNotBlank() && !isListening,
                enter = fadeIn(tween(220)) + expandVertically(),
                exit = fadeOut(tween(120)) + shrinkVertically()
            ) {
                TitleField(
                    title = title,
                    onTitleChanged = viewModel::onTitleChanged,
                    onRegenerate = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.regenerateTitle()
                    },
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp)
                )
            }

            // Transcript / editor
            TranscriptArea(
                text = text,
                partialText = partialText,
                isListening = isListening,
                isTranscribing = isTranscribing,
                onTextChanged = viewModel::onTextChanged,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 12.dp)
            )

            // Status / error line
            val errorText = when {
                permissionDenied -> stringResource(R.string.mic_permission_needed)
                errorKey == "unavailable" -> stringResource(R.string.speech_unavailable)
                errorKey != null -> stringResource(R.string.speech_error_generic)
                else -> null
            }
            AnimatedVisibility(
                visible = errorText != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = errorText.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 4.dp)
                )
            }

            // Microphone
            MicButton(
                captureState = captureState,
                soundLevel = soundLevel,
                onTap = {
                    if (isTranscribing) return@MicButton
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    permissionDenied = false
                    if (isListening) {
                        viewModel.toggleCapture()
                    } else {
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) viewModel.toggleCapture()
                        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            AnimatedContent(
                targetState = captureState,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "micLabel",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { state ->
                Text(
                    text = when (state) {
                        is CaptureState.Listening ->
                            stringResource(R.string.listening) + "  ·  " + stringResource(R.string.tap_to_stop)
                        is CaptureState.Transcribing -> stringResource(R.string.transcribing)
                        else -> stringResource(R.string.tap_to_speak)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
            }

            // Save
            AnimatedVisibility(
                visible = text.isNotBlank() && captureState is CaptureState.Idle,
                enter = fadeIn(tween(220)) + expandVertically(),
                exit = fadeOut(tween(120)) + shrinkVertically()
            ) {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.save()
                    },
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .height(56.dp)
                ) {
                    Text(
                        stringResource(R.string.save_entry),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TitleField(
    title: String,
    onTitleChanged: (String) -> Unit,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = title,
                onValueChange = onTitleChanged,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) {
                            Text(
                                text = stringResource(R.string.title_placeholder),
                                style = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        inner()
                    }
                }
            )
            IconButton(onClick = onRegenerate) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.regenerate_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun TranscriptArea(
    text: String,
    partialText: String,
    isListening: Boolean,
    isTranscribing: Boolean,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Keep the newest words in view while dictating.
    LaunchedEffect(text, partialText, isListening) {
        if (isListening) scrollState.animateScrollTo(scrollState.maxValue)
    }

    Box(modifier = modifier) {
        when {
            // Live, read-only transcript while listening — the unconfirmed
            // hypothesis (system engine only) renders softer than saved words.
            isListening -> {
                if (text.isBlank() && partialText.isBlank()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.listening_hint),
                            style = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = liveTranscript(text, partialText),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    )
                }
            }

            isTranscribing && text.isBlank() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.transcribing_hint),
                        style = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            text.isBlank() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.transcript_placeholder),
                        style = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Review mode — plain, page-like editing.
            else -> {
                Column(Modifier.fillMaxSize()) {
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChanged,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                    )
                    Text(
                        text = stringResource(R.string.editing_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun liveTranscript(text: String, partialText: String): AnnotatedString =
    buildAnnotatedString {
        append(text)
        if (partialText.isNotBlank()) {
            if (text.isNotBlank()) append(" ")
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )
            ) {
                append(partialText)
            }
        }
    }

@Composable
private fun MicButton(
    captureState: CaptureState,
    soundLevel: Float,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening = captureState is CaptureState.Listening
    val isTranscribing = captureState is CaptureState.Transcribing

    val infinite = rememberInfiniteTransition(label = "micPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    // The mic breathes with the speaker's voice.
    val voiceScale by animateFloatAsState(
        targetValue = if (isListening) 1f + soundLevel * 0.08f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "voiceScale"
    )

    Box(
        modifier = modifier.size(148.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isListening) {
            val ringColor = MaterialTheme.colorScheme.primary
            // Two expanding rings, phase-shifted.
            listOf(0f, 0.5f).forEach { phase ->
                val progress = (pulse + phase) % 1f
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .scale(1f + progress * 0.65f)
                        .clip(CircleShape)
                        .background(ringColor.copy(alpha = (1f - progress) * 0.18f))
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = if (isListening) 8.dp else 3.dp,
            modifier = Modifier
                .size(88.dp)
                .scale(voiceScale)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTap
                )
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = when {
                        isTranscribing -> 2
                        isListening -> 1
                        else -> 0
                    },
                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
                    label = "micIcon"
                ) { state ->
                    when (state) {
                        2 -> CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(30.dp)
                        )
                        1 -> Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = stringResource(R.string.tap_to_stop),
                            modifier = Modifier.size(36.dp)
                        )
                        else -> Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = stringResource(R.string.tap_to_speak),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }
}
