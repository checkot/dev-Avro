# Summary of Added and Modified Files

Base repository: [devfahim00/OpenRidmik](https://github.com/devfahim00/OpenRidmik) (MIT License, Kotlin 1.9.24, AGP 8.5.2, `minSdk = 26`, `targetSdk = 34`, zero external app dependencies).

## Added Files
1. `app/src/main/java/com/devfahim00/openridmik/DeveloperKeyboard.kt` — 2-page on-screen PC Developer layout (`Esc`/`Tab`/`Ctrl`/`Alt`/`Shift`/`⌘` + `F1–F12`/arrows/navigation) and real Android `KeyEvent` dispatch.
2. `app/src/main/java/com/devfahim00/openridmik/GeminiVoiceEngine.kt` — Continuous `AudioRecord` auto-pause live voice typing engine (`gemini-3.5-flash-lite`, `MINIMAL` thinking, sequential WAV queue) + `MicIconView` vector icon.
3. `app/src/main/java/com/devfahim00/openridmik/VoiceSettingsActivity.kt` — Settings UI for microphone permission, local Gemini API key, model ID, language mode, smart punctuation, and connection test.
4. `app/src/main/java/com/devfahim00/openridmik/ApkFileProvider.kt` — Zero-dependency read-only `ContentProvider` serving cached update APKs.
5. `app/src/main/java/com/devfahim00/openridmik/ApkUpdater.kt` — In-app APK downloader with redirect handling, live percentage progress, and direct Android package installer launch.
6. `app/src/main/java/com/devfahim00/openridmik/Shortcut.kt` — Shortcut data model, canonical combo key, labels, and JSON serialization.
7. `app/src/main/java/com/devfahim00/openridmik/ShortcutRepository.kt` — Local `SharedPreferences` JSON persistence with revision counter.
8. `app/src/main/java/com/devfahim00/openridmik/ShortcutManager.kt` — Cached shortcut lookup and validation (duplicates, reserved combos, length limits).
9. `app/src/main/java/com/devfahim00/openridmik/MacroExecutor.kt` — Safe macro insertion with password/PIN protection, null `InputConnection` guard, and Avro composition flush.
10. `app/src/main/java/com/devfahim00/openridmik/DeveloperSettingsActivity.kt` — Dedicated Developer Mode settings screen.
11. `app/src/main/java/com/devfahim00/openridmik/ShortcutCaptureActivity.kt` — Add/Edit shortcut screen with live hardware key capture and manual picker fallback.
12. `app/src/test/java/com/devfahim00/openridmik/ShortcutLogicTest.kt` — JUnit 4 unit test suite.
13. `signing/myavro-debug.keystore` — Consistent debug signing keystore for seamless in-place Android updates.
14. `scripts/apply_to_openridmik.py` — Automated integration script applying all overlay files and surgical patches onto upstream OpenRidmik.
15. `LICENSE`, `README.md`, `USER_GUIDE.md`, `ANDROID_LIMITATIONS.md`, `CHANGES_SUMMARY.md` — License compliance and documentation.

## Modified Existing OpenRidmik Files
1. `Prefs.kt` — Added `developer_mode` (default `false`) and `lang_dev` (Developer layout index `4` in spacebar switcher).
2. `KeyboardView.kt` — Added horizontal board swipe (`onBoardSwipe`) for Developer pages and active sticky-modifier highlight.
3. `Layouts.kt` — Replaced Bangla corner hints with Ridmik-style symbol hints on letter keys.
4. `OpenRidmikService.kt` — Integrated hardware shortcut handling, Developer layout + swipeable Fn page, 1-tap Gemini live mic button, and smart auto-space.
5. `SettingsActivity.kt` — Rebranded to `My Avro Keyboard`, removed Telegram button, upgraded update banner to in-app `ApkUpdater`, added `Developer` layout toggle, **Developer Mode**, and **AI Voice Typing (Gemini)** entries.
6. `Updater.kt` — Pointed update checks to `checkot/dev-Avro` (`MYAVRO_UPDATE_REPO`).
7. `AndroidManifest.xml` — Added `RECORD_AUDIO` and `REQUEST_INSTALL_PACKAGES` permissions, registered `DeveloperSettingsActivity`, `ShortcutCaptureActivity`, `VoiceSettingsActivity`, and `ApkFileProvider`.
8. `app/build.gradle.kts`, `settings.gradle.kts`, `strings.xml` — Configured `applicationId = "com.myavro.keyboard"`, fixed debug signingConfig, `My Avro Keyboard` branding, and JUnit test dependency.
