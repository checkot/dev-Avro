# Sum Keyboard

**Sum Keyboard** is a fast, private, ad-free Bangla & English Android keyboard developed and maintained by **Md. Ariful Islam** ([`checkot/dev-Avro`](https://github.com/checkot/dev-Avro)).

It combines smooth Avro phonetic Bangla typing with an on-screen PC Developer keyboard, Gemini AI Live Voice Typing, custom hardware keyboard shortcuts, pro cursor controls, and direct in-app updates.

## Key Features

- **Avro Phonetic & Bangla Layouts:** Full Avro phonetic Bangla, English, Probhat, and Jatiyo layouts with word suggestions, emoji panel, clipboard history, and number pad.
- **Quick Symbol Hints:** Long-press any letter key for fast symbols (`৳`, `%`, `_`, `;`, `<`, `>`, `+`, `=`, `[`, `]`, `@`, `#`, `&`, `*`, `-`, `!`, `?`, `(`, `)`, `"`, `'`, `:`, `/`), plus `x`/`c`/`v` long-press cut/copy/paste.
- **1-Tap Open / 2nd-Tap Close Toolbar:** Tap Numpad, Clipboard, or Emoji once to open and tap again to close immediately.
- **Top-Bar PC Developer Keyboard (`</>` → `PC` → `Fn`):**
  - Tap **`</>`** in the top toolbar to open **PC Mode** (`Esc`, `Tab`, `Shift`, `Alt`, `⌘`, `Ctrl`, number/symbol row, coding hints).
  - Tap **`PC`** again to open **Fn & Navigation Mode** (`F1–F12`, `↑ ↓ ← →`, `Home`, `End`, `PgUp`, `PgDn`, `Ins`, `Del`, `Cut`/`Copy`/`Paste`).
  - Tap **`Fn`** again to return to normal typing.
- **Gemini AI Live Auto-Pause Voice Typing:**
  - Always-available **1-tap Microphone icon** in the top-right corner.
  - Continuous `AudioRecord` capture with ~1.1s pause detection, `gemini-3.5-flash-lite` (`MINIMAL` thinking), smart auto-space, and Bangla + English mixed speech support.
  - Uses your own free [Google AI Studio](https://aistudio.google.com/apikey) API key stored strictly on-device; audio stays in RAM only.
- **Pro Cursor Controls:**
  - **Spacebar Hold Cursor Move:** Hold Spacebar (~0.8s) and slide left/right to move the cursor accurately (adjustable delay & speed).
  - **Volume Button Cursor:** Use Volume Up/Down keys to move the cursor while typing (automatically pauses during media playback).
- **Custom Hardware Shortcuts & Text Macros:** Dedicated **Developer Mode** (OFF by default) to map physical keyboard combos (`Ctrl`/`Alt`/`Shift`) to multiline Bangla/English text macros, blocked automatically in password/PIN fields.
- **12 Themes + Custom Colors:** Includes **Twilight Blossom**, **Midnight Aqua**, **Mystic Magenta**, **Black Pearl**, AMOLED, Ocean, Forest, Rose, Lavender, Sunset, Light, Dark, and System.
- **Direct In-App Updates:** Checks [`checkot/dev-Avro`](https://github.com/checkot/dev-Avro/releases) Releases, downloads the APK in-app with live progress, and installs seamlessly using a consistent signing keystore.

## Maintenance & Build Workflows

- **Main Release Build (`.github/workflows/extract-zip.yml`):** Automatically builds and publishes versioned `v1.0.x` APKs to [`checkot/dev-Avro/releases`](https://github.com/checkot/dev-Avro/releases).
- **Nusai Keyboard Limited Edition (`.github/workflows/build-nusai-edition.yml`):** Optional manual workflow (`workflow_dispatch`) that builds an offline, one-time alphabetical **ABCD** edition (`Nusai Keyboard`, Avro + English only).
- **Mobile Maintenance (`maintenance.html`):** Browser-based uploader and live build status dashboard for managing releases directly from Android.

## Author & License

Maintained by **Md. Ariful Islam** — [`github.com/checkot/dev-Avro`](https://github.com/checkot/dev-Avro).  
Licensed under the **MIT License** (includes open-source MIT keyboard engine components; see [`LICENSE`](LICENSE) for full copyright notices).
