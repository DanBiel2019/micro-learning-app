# Releases

Prebuilt APKs you can sideload onto an Android phone.

| Version | File | Size | Notes |
|---------|------|------|-------|
| 1.1 (debug) | [micro-learning-v1.1-debug.apk](micro-learning-v1.1-debug.apk) | 4.1 MB | First feed release: curation, narration, feedback loop, swipeable UI |

**Direct download:** [micro-learning-v1.1-debug.apk](https://github.com/DanBiel2019/micro-learning-app/raw/main/release/micro-learning-v1.1-debug.apk)

SHA-256: `fdb46117dfec6f683a4a0c6ed7ad8a9f2d52d8674a29de8bfea0b52daad409fd`

## Installing

1. Open the direct download link above on your phone (or copy the file over).
2. Tap the downloaded file. If prompted, allow your browser or file manager to
   **Install unknown apps** (Settings → Apps → Special app access).
3. Open **Micro Learning**.

Requires Android 7.0 (API 24) or newer. Narration uses the phone's built-in
text-to-speech engine. If you hear nothing, check Settings → Accessibility →
Text-to-speech output.

## Notes

- These are **debug builds**, signed with a debug key. Android may warn about
  that. A future version signed with a different key will need this one
  uninstalled first.
- To build one yourself: `./gradlew assembleDebug`, then copy
  `app/build/outputs/apk/debug/app-debug.apk` here as
  `micro-learning-v<version>-debug.apk` and add a row to the table above.
