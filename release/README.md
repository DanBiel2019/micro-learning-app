# Releases

Prebuilt APKs you can sideload onto an Android phone.

| Version | File | Size | Notes |
|---------|------|------|-------|
| 2.0 (debug) | [micro-learning-v2.0-debug.apk](micro-learning-v2.0-debug.apk) | 21 MB | Daily two-host studio episodes, infographics, transcript, background playback, redesigned UI |
| 1.1 (debug) | [micro-learning-v1.1-debug.apk](micro-learning-v1.1-debug.apk) | 4.1 MB | First feed release: curation, device-voice narration, feedback loop, swipeable cards |

**Direct download (latest):** [micro-learning-v2.0-debug.apk](https://github.com/DanBiel2019/micro-learning-app/raw/main/release/micro-learning-v2.0-debug.apk)

SHA-256 (2.0): `5e3add50c3ebabe60dd97cda572f5af8049531803e9e2dd832d9e6d2ce8df5a1`

## Installing

1. Open the direct download link above on your phone (or copy the file over).
2. Tap the downloaded file. If prompted, allow your browser or file manager to
   **Install unknown apps** (Settings → Apps → Special app access).
3. Open **Micro Learning**. Allow notifications when asked, so playback controls show on
   the lock screen.

2.0 installs over 1.1 directly. Requires Android 7.0 (API 24) or newer. Today's episode
streams on first open and is downloaded for offline listening in the background.

## Notes

- These are **debug builds**, signed with a debug key. Android may warn about
  that. A future version signed with a different key will need this one
  uninstalled first.
- To build one yourself: `./gradlew assembleDebug`, then copy
  `app/build/outputs/apk/debug/app-debug.apk` here as
  `micro-learning-v<version>-debug.apk` and add a row to the table above.
