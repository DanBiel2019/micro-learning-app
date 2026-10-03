---
description: Orchestrate the ui-polisher, infographic-designer and feed-model-scout agents to update the app and feed
---

Orchestrate an update of the micro-learning app:

1. Start `feed-model-scout` in its own worktree (it only touches pipeline/model files and
   needs long waits for GitHub dry runs, so run it in the background).
2. Run `ui-polisher` in its own worktree. Only one Gradle build may run at a time on this
   machine, so do not run it alongside `infographic-designer`.
3. When `ui-polisher` finishes, merge its branch, then run `infographic-designer` in a
   worktree based on the merged result.
4. Merge every branch into one integration branch, resolve conflicts, bump the app version,
   run unit tests and assemble the APK, and review on the emulator (screenshots of Today,
   a segment page, Now Playing, the infographic gallery, dark mode).
5. Ship: merge to main, publish the APK as a GitHub Release marked latest (with the APK in
   release/ too), and confirm the daily-feed release serves the newest episode.
