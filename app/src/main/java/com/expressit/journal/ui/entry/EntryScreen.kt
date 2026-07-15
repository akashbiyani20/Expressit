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
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.expressit.journal.speech.SpeechRecognizerManager.SpeechError
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EntryScreen(
    viewModel: EntryViewModel,
    onClose: () -> Unit
) {
    val text by viewModel.text.collectAsStateWithLifecycle()
    val speechState by viewModel.speech.state.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionDenied = !granted
        if (granted) viewModel.toggleListening()
    }

    LaunchedEffect(saved) {
        if (saved) onClose()
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault()) }

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

            // Transcript / editor
            TranscriptArea(
                text = text,
                partialText = speechState.partialText,
                isListening = speechState.isListening,
                onTextChanged = viewModel::onTextChanged,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 16.dp)
            )

            // Status / error line
            val errorText = when {
                permissionDenied -> stringResource(R.string.mic_permission_needed)
                speechState.error == SpeechError.UNAVAILABLE -> stringResource(R.string.speech_unavailable)
                speechState.error == SpeechError.GENERIC -> stringResource(R.string.speech_error_generic)
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
                isListening = speechState.isListening,
                soundLevel = speechState.soundLevel,
                onTap = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    permissionDenied = false
                    if (speechState.isListening) {
                        viewModel.toggleListening()
                    } else {
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) viewModel.toggleListening()
                        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            AnimatedContent(
                targetState = speechState.isListening,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "micLabel",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { listening ->
                Text(
                    text = if (listening) {
                        stringResource(R.string.listening) + "  ·  " + stringResource(R.string.tap_to_stop)
                    } else {
                        stringResource(R.string.tap_to_speak)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
            }

            // Save
            AnimatedVisibility(
                visible = text.isNotBlank() && !speechState.isListening,
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
private fun TranscriptArea(
    text: String,
    partialText: String,
    isListening: Boolean,
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
            // hypothesis is rendered softer than confirmed words.
            isListening -> {
                Text(
                    text = liveTranscript(text, partialText),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                )
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
    isListening: Boolean,
    soundLevel: Float,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    targetState = isListening,
                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
                    label = "micIcon"
                ) { listening ->
                    Icon(
                        imageVector = if (listening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                        contentDescription = if (listening) {
                            stringResource(R.string.tap_to_stop)
                        } else {
                            stringResource(R.string.tap_to_speak)
                        },
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}
