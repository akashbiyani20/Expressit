# ExpressIt — a voice-first personal journal

Speak, review, save. ExpressIt turns spoken thoughts into calm, dated journal
entries — no typing, no accounts, no cloud. Everything stays on the device.

## What's new in 1.2

- **On-device AI transcription (Whisper).** Menu → Transcription → download the
  model once (~148 MB, from Hugging Face). After that, recordings are
  transcribed fully offline with proper punctuation and far better accuracy on
  long, natural speech. Without the model, the app falls back to the system
  recognizer as before. Audio is held only in memory and discarded after
  transcription — nothing is ever written to disk.
- **Entry headings.** Every entry gets a short title generated from its own
  words the moment transcription lands — tap it to edit, or tap ↻ to cycle
  suggestions. Titles appear on the day's cards and in exports.
- **Jump anywhere in time.** Tap the month title to open a month + year picker;
  arrows and Today still work for nearby hops.
- **Export & share.** Menu → Export entries → pick a date range → a clean
  Markdown file opens in the share sheet, ready for an AI chat, a therapist,
  email, or Drive.
- **New icon.** The mic is the nose, "Expressit" curves beneath it as the smile.
- **In-place updates.** All builds are now signed with the fixed key in
  `keystore/`, so future APKs install straight over old ones (see below).

## Getting started

1. Open the project folder in **Android Studio** (Ladybug or newer).
2. Let Gradle sync. The `:whisper` module builds native code — Android Studio
   will offer to install the **NDK and CMake** if missing; accept.
3. Run on a device with **Android 8.0 (API 26)** or higher. First recording
   asks for microphone permission.
4. For best transcription, open **⋮ → Transcription** and download the AI model.

### Installing updates without uninstalling

Android only updates an app in place when the new APK has the same application
id **and the same signing key** as the installed one. Both are now fixed: the
id is `com.expressit.journal` and every build (debug and release) is signed
with `keystore/expressit-release.jks` (alias `expressit`, passwords
`expressit1`, wired up in `app/build.gradle.kts`).

One-time note: if the currently installed copy came from an older, differently
signed build, this first 1.2 install still needs an uninstall. Every version
after that updates in place — just keep `versionCode` increasing.

The keystore committed to the repo is convenient for a personal project; if the
repo ever goes public or the app ships to Play, move the keystore and passwords
out of version control.

## Architecture

Two modules, MVVM, unidirectional data flow:

```
app/  com.expressit.journal
├── data/          Room entity/DAO/db (v2: + title column, migration included),
│                  JournalRepository, JournalExporter (Markdown + share sheet)
├── speech/        PcmRecorder (16 kHz mono, in-memory), WhisperModel
│                  (download/own/transcribe), SpeechRecognizerManager (fallback),
│                  TranscriptionSettings
├── util/          TitleGenerator — instant offline heading suggestions
├── ui/theme|home|entry
└── MainActivity   Navigation Compose graph

whisper/  thin Kotlin+JNI wrapper around the vendored whisper.cpp
└── src/main/jni/whisper.cpp — upstream sources (MIT, see LICENSE inside)
```

Titles today are extractive (lifted from the entry's own words — instant,
offline, truthful). The `TitleGenerator` seam is where a generative model, local
or cloud, can slot in later.

## Publishing to your GitHub repo

From this folder:

```bash
git remote add origin <your-repo-url>   # once
git add -A
git commit -m "v1.2: Whisper transcription, titles, export, date jump, new icon"
git push origin main --force-with-lease  # or merge onto your existing history
```
