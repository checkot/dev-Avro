# My Avro Keyboard

A custom Android keyboard built on top of the open-source [OpenRidmik](https://github.com/devfahim00/OpenRidmik) project (MIT License). It preserves the complete Avro phonetic Bangla typing engine, Probhat/Jatiyo layouts, suggestions, emoji, clipboard history, and themes while adding a dedicated **Developer Mode** for custom hardware keyboard shortcuts and Bangla/English text macros.

## How to Build the Installable APK (No PC Needed — Using GitHub)

Because this package merges cleanly onto the official OpenRidmik source (including its 75,000-word dictionaries and Avro rules) and includes a pre-configured GitHub Actions workflow, you can build `MyAvroKeyboard-debug.apk` directly in the cloud from your Android phone:

1. Create a new repository on [GitHub](https://github.com/new) (for example, `MyAvroKeyboard`).
2. Extract this ZIP file and upload all files (including `.github/workflows/build-my-avro.yml`, `scripts/`, `app/`, `settings.gradle.kts`, and `LICENSE`) to your new repository's `main` branch.
3. Open the **Actions** tab in your GitHub repository. The **Build My Avro Keyboard Debug APK** workflow will run automatically (or tap **Run workflow**).
4. Wait ~2–3 minutes for the green checkmark.
5. Open **Releases** in your repository and download **`MyAvroKeyboard-debug.apk`**.

## How to Build in Android Studio (PC Option)

1. Clone the official OpenRidmik repository:
   ```bash
   git clone https://github.com/devfahim00/OpenRidmik.git OpenRidmik
   ```
2. Apply the My Avro Keyboard modules and patches:
   ```bash
   python3 scripts/apply_to_openridmik.py OpenRidmik
   ```
3. Open the `OpenRidmik` folder in **Android Studio** (JDK 17, Android SDK 34, Gradle 8.9), or run:
   ```bash
   cd OpenRidmik
   gradle testDebugUnitTest assembleDebug
   ```
4. Your APK will be created at `OpenRidmik/app/build/outputs/apk/debug/app-debug.apk`.

## Installing on Your Android Phone

1. Tap the downloaded `MyAvroKeyboard-debug.apk` file on your phone.
2. If prompted, allow **Install unknown apps** for your browser or file manager.
3. Open **My Avro Keyboard** from your app drawer.
4. Tap **Enable OpenRidmik / My Avro Keyboard** to turn it ON in Android System Settings.
5. Tap **Switch keyboard** and select **My Avro Keyboard**.

## License & Attribution

Based on [OpenRidmik](https://github.com/devfahim00/OpenRidmik) © 2026 `devfahim00`, licensed under the MIT License. See `LICENSE` for full terms.
