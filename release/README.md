# Releases

Prebuilt APKs you can sideload onto an Android phone.

| Version | File | Size | Notes |
|---------|------|------|-------|
| 3.0 (debug) | [micro-learning-v3.0-debug.apk](micro-learning-v3.0-debug.apk) | 22 MB | Today / Library / You tabs, full-screen Now Playing, 16 infographic kinds with an in-app gallery, new icon; episodes written by Gemma 4 |
| 2.0 (debug) | [micro-learning-v2.0-debug.apk](micro-learning-v2.0-debug.apk) | 21 MB | Daily two-host studio episodes, infographics, transcript, background playback, redesigned UI |
| 1.1 (debug) | [micro-learning-v1.1-debug.apk](micro-learning-v1.1-debug.apk) | 4.1 MB | First feed release: curation, device-voice narration, feedback loop, swipeable cards |

**Direct download (latest):** [micro-learning-v3.0-debug.apk](https://github.com/DanBiel2019/micro-learning-app/raw/main/release/micro-learning-v3.0-debug.apk)

SHA-256 (3.0): `6a78d03d9d3cfd7195714e57c10ee46c2fae06adca7f6a7cf6d2351ed2b918bf`

## Installing

1. Open the direct download link above on your phone (or copy the file over).
2. Tap the downloaded file. If prompted, allow your browser or file manager to
   **Install unknown apps** (Settings → Apps → Special app access).
3. Open **Micro Learning**. Allow notifications when asked, so playback controls show on
   the lock screen.

3.0 installs over 2.0 and 1.1 directly. Requires Android 7.0 (API 24) or newer. Today's episode
streams on first open and is downloaded for offline listening in the background.

## Notes

- These are **debug builds**, signed with a debug key. Android may warn about
  that. A future version signed with a different key will need this one
  uninstalled first.
- To build one yourself: `./gradlew assembleDebug`, then copy
  `app/build/outputs/apk/debug/app-debug.apk` here as
  `micro-learning-v<version>-debug.apk` and add a row to the table above.
