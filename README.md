# My Avro Keyboard

A fast, ad-free, open-source Android keyboard built on top of [OpenRidmik](https://github.com/devfahim00/OpenRidmik) (MIT License) with a **2-page on-screen PC Developer Layout**, **Gemini AI Live Voice Typing**, **custom hardware keyboard shortcuts & text macros**, **Ridmik-style symbol hints**, and **in-app updates**.

## Key Features

- **Avro Phonetic & Bangla Layouts:** Full Avro phonetic Bangla, English, Probhat, and Jatiyo layouts with 75,000-word suggestions, emoji panel, clipboard history, number pad, and customizable themes.
- **Ridmik-Style Symbol Hints:** Quick long-press symbol hints (`৳`, `%`, `_`, `;`, `<`, `>`, `+`, `=`, `[`, `]`, `@`, `#`, `&`, `*`, `-`, `!`, `?`, `(`, `)`, `"`, `'`, `:`, `/`) across English and Avro layouts, plus `x`/`c`/`v` long-press cut/copy/paste.
- **On-Screen PC Developer Layout (2 Swipeable Pages):**
  - Slide the **Spacebar** left/right to switch to **Developer** layout (`Esc`, `Tab`, `Shift`, `Alt`, `⌘`, `Ctrl`, number/symbol row, and coding symbol hints).
  - Swipe left/right across the main **Developer** keyboard body to flip to **Page 2** (`F1–F12`, `↑ ↓ ← →` arrow keys, `Home`, `End`, `PgUp`, `PgDn`, `Ins`, `Del`, and `Cut`/`Copy`/`Paste`).
  - Every key dispatches real Android `KeyEvent`s with active `Ctrl`/`Alt`/`Shift`/`Meta` modifier states (ideal for Termux, code editors, and terminals).
- **Developer Mode (Custom Hardware Shortcuts & Macros):**
  - Default **OFF** so normal typing is never affected.
  - Create, edit, toggle, and delete custom `Ctrl` / `Alt` / `Shift` shortcuts mapped to multiline Bangla or English text macros (stored locally on-device, blocked automatically in password/PIN fields).
- **Gemini AI Live Auto-Pause Voice Typing:**
  - Always-visible **1-tap Microphone icon** in the top-right corner of the keyboard.
  - Continuous `AudioRecord` capture with **auto-pause detection (~1.1s)** that streams each spoken sentence to the Gemini API (`gemini-3.5-flash-lite` with `MINIMAL` thinking) and types in exact spoken order with smart auto-spacing.
  - Supports **Auto (Bangla + English mixed)**, **বাংলা**, and **English** modes with optional smart punctuation cleanup. Uses your own free [Google AI Studio](https://aistudio.google.com/apikey) key stored strictly on-device; audio stays in RAM and is never saved to disk.
- **In-App Updates & Seamless Signing:**
  - Built-in **Check for updates** checks `checkot/dev-Avro` GitHub Releases, downloads the new APK in-app with live progress, and launches Android's installer directly.
  - Signed with a consistent debug keystore (`signing/myavro-debug.keystore`) so new builds install directly as an **Update** without losing shortcuts or settings.

## Zero-PC Build & Deploy Pipeline

1. **Automated Drive → GitHub Deploy:** Dropping `MyAvroKeyboard-Project.zip` into the Google Drive `MyAvroDeploy` folder triggers the Apps Script bridge to push it to `checkot/dev-Avro` root (or upload manually via `maintenance.html`).
2. **GitHub Actions (`.github/workflows/extract-zip.yml`):** Automatically extracts the ZIP via `extract-zip.js`, clones `devfahim00/OpenRidmik`, applies all modules and patches via `scripts/apply_to_openridmik.py`, runs JUnit tests, builds `MyAvroKeyboard-debug.apk` (`v1.0.x`), and publishes a GitHub Release.

## PC / Android Studio Build (Optional)

```bash
git clone https://github.com/devfahim00/OpenRidmik.git OpenRidmik
python3 scripts/apply_to_openridmik.py OpenRidmik
cd OpenRidmik
gradle testDebugUnitTest assembleDebug
```

## License & Attribution

Based on [OpenRidmik](https://github.com/devfahim00/OpenRidmik) © 2026 `devfahim00`, licensed under the MIT License. See `LICENSE` for full terms.
