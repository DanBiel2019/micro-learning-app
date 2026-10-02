# micro-learning-app

A personal morning podcast for Android. Every day a new ~10 minute episode arrives: two
hosts walk through five ideas in roughly two-minute segments you can swipe between, pause,
or dig into. Each segment has its own infographic, a live transcript, a challenge for the
day, and cited sources.

Episodes mix your core interests (systems and measurement, technology and AI, leadership,
business, creativity) with adjacent fields (aviation safety, medicine, military history,
behavioral economics, …) plus one fresh item from the last couple of months, so it never
settles into the same rut.

## Download

Grab the latest APK from [`release/`](release/), or download it directly:
[micro-learning-v2.0-debug.apk](https://github.com/DanBiel2019/micro-learning-app/raw/main/release/micro-learning-v2.0-debug.apk).
Install steps are in [release/README.md](release/README.md).

Episodes themselves are published as [GitHub Releases](https://github.com/DanBiel2019/micro-learning-app/releases)
(`ep-YYYY-MM-DD`), one per day.

## How it fits together

```
 GitHub Actions, daily                          Android app
 ┌──────────────────────────────┐               ┌───────────────────────────────────────┐
 │ pipeline/                    │   Release     │ EpisodeRepository  fetch + cache feed │
 │  generate.py  Claude writes  │──feed.json──▶ │ PrefetchWorker     download overnight │
 │  render_audio  Kokoro voices │  seg-N.mp3    │ PlaybackService    Media3, lock screen│
 │  publish.py   GitHub Release │               │ Compose UI         infographics, etc. │
 └──────────────────────────────┘               └───────────────────────────────────────┘
```

See [`pipeline/README.md`](pipeline/README.md) for the content pipeline and its one-time
setup (none: it runs on free, open-weights models).

### App

```
app/src/main/java/com/example/aigeneratedandroid/
  MainActivity.kt          edge-to-edge Compose host
  microlearning/
    model/       Episode/Segment/Visual (mirrors pipeline/schema.py), UserProfile
    data/        EpisodeRepository (GitHub Releases feed, offline cache), PrefetchWorker,
                 FeedbackStore (likes, skips, completions)
    playback/    PlaybackService (Media3 session), EpisodePlayer (studio audio or device TTS)
    curation/    ContentCurationEngine: offline episodes from the bundled library
    narration/   NarrationFormatter + NarrationPlayer: device-voice fallback
    ui/          Home (generated cover art, chapters, history), Segment pages
                 (Infographic, transcript, go deeper), MiniPlayer, theme
app/src/main/assets/library.json   30 classic ideas with infographic specs (offline fallback)
```

- **Infographics** are drawn natively from a small spec (`flow`, `cycle`, `compare`,
  `stats`, `bars`, `venn`, `ladder`, `timeline`, `quote`), so every new episode gets
  illustrations without shipping artwork, and they adapt to dark mode.
- **Playback** keeps going with the screen off, with lock-screen and headphone controls,
  ±15/30 s skips, 0.85–1.5× speed, and swipe-to-skip between segments. The transcript
  highlights the line being spoken and tapping a line jumps to it.
- **Offline**: the newest episode's audio is downloaded in the background on Wi-Fi. With no
  connection at all, the app builds a set from the bundled library and reads it with the
  phone's best available voice.

## Building

Requires JDK 17 and the Android SDK (platform 34). With `local.properties` pointing at
your SDK (`sdk.dir=...`):

```
./gradlew testDebugUnitTest   # curation, narration and feed-format tests
./gradlew assembleDebug
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`.
