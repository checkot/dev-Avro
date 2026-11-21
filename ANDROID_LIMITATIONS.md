# Known Android Hardware Keyboard Limitations

Android Input Method Editors (IMEs) do **not** have unrestricted global hotkey access like desktop Windows applications. Key events pass through Android's system framework and often through the active foreground app before reaching the keyboard.

## Combinations That Cannot Be Captured Reliably
- **System / Meta (Windows / Command / Search) shortcuts:** Intercepted by Android OS (e.g., `Meta + H` for Home, `Meta + Tab` for Recents, `Meta + Enter`).
- **App & Window switching:** `Alt + Tab`, `Alt + Shift + Tab`, `Alt + F4`, `Ctrl + Alt + Delete`, `Ctrl + Esc`.
- **System Language Switching:** `Ctrl + Space`, `Shift + Space`, `Alt + Space` are intercepted by Android's input switcher.
- **Standard Text Editing:** `Ctrl + C`, `Ctrl + V`, `Ctrl + X`, `Ctrl + A`, `Ctrl + Z`, `Ctrl + Y` are intentionally reserved so normal copy/paste/undo/select-all keep working in every app.
- **App-Specific Overrides:** Some apps (such as web browsers, remote desktop clients, or terminal emulators like Termux) may consume specific `Ctrl + Letter` combinations before Android forwards them to the IME.

## Most Reliable Combinations to Use
- **`Ctrl + Alt + Letter / Number`** (e.g., `Ctrl + Alt + M`, `Ctrl + Alt + 1`)
- **`Ctrl + Shift + Letter / Number`** (e.g., `Ctrl + Shift + B`, `Ctrl + Shift + E`)
- **`Alt + Shift + Letter / Number`** (e.g., `Alt + Shift + 1`)
- **`Ctrl + Alt + F1..F12`**

## Other Platform Notes
- **Active IME Requirement:** Shortcuts only run while **Sum Keyboard** is selected as the active Android keyboard and a text input field is focused.
- **Password & PIN Fields:** Macros are intentionally blocked in password and numeric-PIN fields for security.
- **Text Selection:** Standard Android text fields replace selected text when `commitText` is called; a few custom non-standard editors (such as canvas-based games or webview editors) may append instead.
