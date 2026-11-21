# Sum Keyboard — Module & Architecture Summary

Repository: [`checkot/dev-Avro`](https://github.com/checkot/dev-Avro)  
Maintainer: **Md. Ariful Islam**  
Stack: Pure Kotlin 1.9.24, Android SDK 34 (`minSdk = 26`, `targetSdk = 34`), zero external app dependencies.

## Core Modules
- `DeveloperKeyboard.kt` — 2-page PC Developer layout (`PC` & `Fn` modes) with real Android `KeyEvent` dispatch.
- `GeminiVoiceEngine.kt` — Continuous `AudioRecord` auto-pause live voice typing (`gemini-3.5-flash-lite`, `MINIMAL` thinking, RAM-only WAV) + `MicIconView`.
- `VoiceSettingsActivity.kt` — AI Voice Typing configuration & connection test UI.
- `KeymapGuideActivity.kt` — Avro phonetic typing reference inside Settings.
- `ApkFileProvider.kt` & `ApkUpdater.kt` — Zero-dependency in-app APK downloader & installer.
- `Shortcut.kt`, `ShortcutRepository.kt`, `ShortcutManager.kt`, `MacroExecutor.kt` — Custom hardware shortcut storage, validation, and password-safe macro execution.
- `DeveloperSettingsActivity.kt` & `ShortcutCaptureActivity.kt` — Developer Mode & shortcut capture screens.
- `Theme.kt` — 12 built-in themes (including Twilight Blossom, Midnight Aqua, Mystic Magenta, Black Pearl) + custom colors.
- `Prefs.kt` — Local settings (typing preferences, long-press delay, cursor controls, Developer Mode).
- `scripts/apply_to_openridmik.py` — Main Sum Keyboard build integrator (1-tap toolbar toggles, symbol hints, cursor controls, top-bar `</>` selector, in-app updater).
- `scripts/apply_nusai_edition.py` — Optional offline one-time **Nusai Keyboard** ABCD edition builder.
- `signing/myavro-debug.keystore` — Consistent signing keystore for seamless in-place updates.
