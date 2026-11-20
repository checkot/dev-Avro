# Summary of Added and Modified Files

Base repository: [devfahim00/OpenRidmik](https://github.com/devfahim00/OpenRidmik) (MIT License, Kotlin 1.9.24, AGP 8.5.2, `minSdk = 26`, `targetSdk = 34`, zero external app dependencies).

## Added Files
1. `app/src/main/java/com/devfahim00/openridmik/Shortcut.kt` — Shortcut data model, canonical combo key, human-readable formatting, key-code label mapper, and JSON serialization.
2. `app/src/main/java/com/devfahim00/openridmik/ShortcutRepository.kt` — Local `SharedPreferences` JSON persistence with revision tracking and duplicate lookup.
3. `app/src/main/java/com/devfahim00/openridmik/ShortcutManager.kt` — Cached in-memory shortcut matching and validation (empty checks, modifier checks, reserved combo checks, macro length limit, duplicate prevention).
4. `app/src/main/java/com/devfahim00/openridmik/MacroExecutor.kt` — Safe macro insertion with dynamic password/PIN field blocking, null `InputConnection` guard, batch-edit error safety, and Avro composition flushing.
5. `app/src/main/java/com/devfahim00/openridmik/DeveloperSettingsActivity.kt` — Dedicated Developer Mode settings screen (master ON/OFF switch, shortcut list, individual enable/disable switches, edit/delete dialogs, Android limitations card).
6. `app/src/main/java/com/devfahim00/openridmik/ShortcutCaptureActivity.kt` — Add/Edit shortcut screen with live hardware key capture (`dispatchKeyEvent`), manual modifier/key picker fallback, Bangla/English multiline macro editor, and live validation errors.
7. `app/src/test/java/com/devfahim00/openridmik/ShortcutLogicTest.kt` — JUnit 4 unit test suite for formatting, validation rules, password-field detection, and null `InputConnection` handling.
8. `scripts/apply_to_openridmik.py` — Automated integration script that merges all files and patches cleanly onto upstream OpenRidmik.
9. `.github/workflows/build-my-avro.yml` — Automated cloud workflow to run unit tests and build `MyAvroKeyboard-debug.apk`.
10. `LICENSE`, `README.md`, `USER_GUIDE.md`, `ANDROID_LIMITATIONS.md`, `CHANGES_SUMMARY.md` — License compliance and documentation.

## Modified Existing OpenRidmik Files
1. `app/src/main/java/com/devfahim00/openridmik/Prefs.kt` — Added `"developer_mode" to false` default plus `isDeveloperMode()` / `setDeveloperMode()`.
2. `app/src/main/java/com/devfahim00/openridmik/OpenRidmikService.kt` — Patched cleanly via `apply_to_openridmik.py` to add `consumedShortcutKeyCode`, `onStartInput`, `onKeyDown`, and `onKeyUp` without altering existing Avro phonetic, soft keyboard, or layout logic.
3. `app/src/main/java/com/devfahim00/openridmik/SettingsActivity.kt` — Patched cleanly via `apply_to_openridmik.py` to display `My Avro Keyboard` and add the separated **Developer Mode** entry row with live ON/OFF status.
4. `app/src/main/AndroidManifest.xml` — Registered `.DeveloperSettingsActivity` and `.ShortcutCaptureActivity` with `android:exported="false"`. No new permissions added.
5. `app/build.gradle.kts` — Set `applicationId = "com.myavro.keyboard"` (installs side-by-side with OpenRidmik), kept `namespace = "com.devfahim00.openridmik"`, and added `testImplementation("junit:junit:4.13.2")`.
6. `settings.gradle.kts` — Set `rootProject.name = "MyAvroKeyboard"`.
7. `app/src/main/res/values/strings.xml` — Set `app_name` to `My Avro Keyboard`.
