# ExpressIt — a voice-first personal journal

Speak, review, save. ExpressIt turns spoken thoughts into calm, dated journal
entries — no typing, no accounts, no cloud. Everything stays on the device.

## Getting started

1. Open the project folder in **Android Studio** (Ladybug or newer).
2. Let Gradle sync (it downloads dependencies on first open).
3. Run on a device or emulator with **Android 8.0 (API 26)** or higher.

Speech recognition uses the device's built-in recognition service (Google's on
most phones), so a **physical device** gives the best experience. On first
recording, the app asks for microphone permission.

## How it works

- **Home** is a month calendar. Today is highlighted; days with entries carry a
  small dot. Tap a day to read its entries, or the **+** button to add one.
- **Recording**: tap the mic and talk naturally — the transcript appears live,
  with the in-flight guess shown softer than confirmed words. Pauses are fine;
  listening continues until you tap stop. Then edit the text if you like and
  press **Save entry**. Tapping an existing entry reopens it for edits or to
  dictate more.
- Only transcribed text is stored (in a local Room database). No audio is kept.

## Architecture

Single-module MVVM with unidirectional data flow:

```
app/src/main/java/com/expressit/journal/
├── data/          Room entity, DAO, database, JournalRepository
├── speech/        SpeechRecognizerManager — start/stop dictation session that
│                  auto-restarts across pauses and streams partial results
├── ui/
│   ├── theme/     Palette ("eucalyptus ink on paper"), Lora + Inter type scale,
│   │              rounded shape system, light & dark schemes
│   ├── home/      HomeViewModel + calendar / entry-list screen
│   └── entry/     EntryViewModel + record-review-save screen
├── ExpressItApp   Application-level service locator (repository)
└── MainActivity   Edge-to-edge host + Navigation Compose graph
```

- **UI**: Jetpack Compose, Material 3, Navigation Compose.
- **State**: `StateFlow` in ViewModels, collected with lifecycle awareness.
- **Storage**: Room. An entry is `(id, epochDay, createdAt, text)` — indexed by
  day for the calendar dots and per-day lists.
- **Fonts**: Lora (display / journal text) and Inter (UI) are bundled as
  variable fonts in `res/font`, so typography is identical offline and on every
  device.

## Extending

The seams for v2 are already in place: `JournalRepository` is the single data
gateway (swap in export, search, or sync), the speech layer is isolated behind
one class (swap in an on-device or streaming model), and the theme is entirely
token-driven in `ui/theme`.
