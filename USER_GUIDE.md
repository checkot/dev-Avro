# My Avro Keyboard — User Guide

## 1. Getting Started & Normal Typing
1. Install `MyAvroKeyboard-debug.apk` from Releases (or tap **Update available** inside the app for future versions).
2. Open **My Avro Keyboard** → tap **Enable My Avro Keyboard** → turn it ON → tap **Switch keyboard** and pick **My Avro Keyboard**.
3. **Switch layouts:** Hold and slide the **Spacebar** left or right to switch between **English**, **Avro**, **Probhat**, **Jatiyo**, and **Developer**.
4. **Quick symbols:** Long-press any letter key to type its corner symbol (`৳`, `%`, `_`, `;`, `<`, `>`, `+`, `=`, `[`, `]`, `@`, `#`, `&`, `*`, `-`, `!`, `?`, `(`, `)`, `"`, `'`, `:`, `/`). Long-press `x` / `c` / `v` for Cut / Copy / Paste.

## 2. Using the On-Screen PC Developer Layout
1. Tap the **`</>`** button in the top toolbar (where the book icon used to be; the Avro Typing Guide is now inside **Settings → Avro Typing Guide**).
2. **Page 1 (PC Coding Layout):** Includes `Esc`, `Tab`, `Shift`, `Alt`, `⌘`, `Ctrl`, number/symbol row, and coding symbols on every letter key.
   - Tap `Ctrl`, `Alt`, or `⌘` once to highlight it (sticky modifier), then tap the next key (e.g., `Ctrl` → `C`, `Ctrl` → `V`, `Ctrl` → `Z`, `Ctrl` → `L` in Termux).
3. **Page 2 (Function Keys & Navigation):** Tap **`PC`** in the top toolbar again to switch to **`Fn` (Developer · Fn)** (`F1–F12`, `↑ ↓ ← →`, `Home`, `End`, `PgUp`, `PgDn`, `Ins`, `Del`, `Cut`, `Copy`, `Paste`). Tap **`Fn`** again to return to your normal keyboard, or tap **ABC** to return to PC Page 1.

## 3. Setting Up Gemini AI Live Voice Typing
1. Open **My Avro Keyboard** → scroll to **Developer Mode** → tap **AI Voice Typing (Gemini)**.
2. Tap **Grant Microphone Permission** and select **Allow**.
3. Tap **Get Free Key ↗** to create a free API key at [Google AI Studio](https://aistudio.google.com/apikey), paste it into the **Gemini API Key** box, and tap **Save**.
4. Keep **Gemini Model ID** as `gemini-3.5-flash-lite` (fastest free-tier model; you can also use `gemini-3.6-flash` or `gemini-3.8-flash`).
5. Choose your language mode (**Auto (BN+EN)**, **বাংলা**, or **English**) and tap **Save & Test Gemini Connection** to confirm `"Connected!"`.
6. **How to dictate:** Tap the **Microphone icon** in the top-right corner of the keyboard:
   - **Red + dot:** Live listening ON — speak naturally; pausing ~1 second after a sentence automatically types it while keeping the mic open.
   - **Accent color (blue):** Finishing transcription of the last spoken phrase.
   - **Tap Mic again:** Stops listening (icon returns to normal).

## 4. Custom Hardware Keyboard Shortcuts & Text Macros
1. Open **My Avro Keyboard** → **Developer Mode** → **Hardware Shortcuts & Macros**.
2. Turn **Enable Developer Mode** ON (default is OFF).
3. Tap **+ Add Shortcut**:
   - Capture a physical keyboard combo (e.g., `Ctrl + Alt + M`) or choose modifiers + key manually.
   - Enter a readable **Shortcut Name** and your **Text Macro** (Bangla, English, multiline, up to 5,000 characters).
   - Tap **Save Shortcut**.
4. Shortcuts also work from the on-screen **Developer** keyboard when `Ctrl`/`Alt` is toggled ON.

## 5. In-App Updates
- Open **My Avro Keyboard** → tap **Check for updates** (or tap the **Update available** banner at the top).
- The new APK downloads right inside the app with live percentage progress and opens Android's **Update** popup automatically (allow *"Install unknown apps"* once if prompted).

## 6. Privacy & Safety
- All shortcuts, macros, and your Gemini API key are stored locally on your device only.
- Voice audio stays in memory only (never saved to storage) and is sent only to Google's Gemini API when you tap the Mic.
- Both macros and voice typing are automatically blocked inside password and PIN fields.
