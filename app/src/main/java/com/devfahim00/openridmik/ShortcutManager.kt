package com.devfahim00.openridmik

import android.content.Context
import android.view.KeyEvent

/**
 * Coordinates hardware keyboard shortcut matching, caching, and validation.
 *
 * Designed for minimal overhead inside [OpenRidmikService.onKeyDown]:
 * - Returns immediately when Developer Mode is OFF.
 * - Uses an in-memory map keyed by [Shortcut.comboKey], refreshed automatically
 *   whenever [ShortcutRepository.revision] changes.
 */
object ShortcutManager {

    const val MAX_NAME_LENGTH = 80
    const val MAX_MACRO_LENGTH = 5000

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
    }

    @Volatile
    private var cachedRev: Long = -1L

    @Volatile
    private var enabledByCombo: Map<String, Shortcut> = emptyMap()

    fun invalidate() {
        cachedRev = -1L
    }

    private fun ensureLoaded(c: Context) {
        val rev = ShortcutRepository.revision(c)
        if (rev == cachedRev) return
        synchronized(this) {
            val currentRev = ShortcutRepository.revision(c)
            if (currentRev == cachedRev) return
            val map = HashMap<String, Shortcut>()
            for (s in ShortcutRepository.loadAll(c)) {
                if (s.enabled) {
                    map[s.comboKey()] = s
                }
            }
            enabledByCombo = map
            cachedRev = currentRev
        }
    }

    /**
     * Finds an enabled shortcut matching the given hardware [KeyEvent].
     * Returns null if Developer Mode is OFF, modifiers do not qualify, or no shortcut matches.
     */
    fun findMatch(c: Context, event: KeyEvent): Shortcut? {
        if (!Prefs.isDeveloperMode(c)) return null
        val keyCode = event.keyCode
        if (isModifierKey(keyCode)) return null
        if (event.isMetaPressed) return null

        val ctrl = event.isCtrlPressed
        val alt = event.isAltPressed
        val shift = event.isShiftPressed
        if (!ctrl && !alt) return null

        ensureLoaded(c)
        return enabledByCombo[Shortcut.comboKey(ctrl, shift, alt, keyCode)]
    }

    fun isModifierKey(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_CTRL_LEFT,
        KeyEvent.KEYCODE_CTRL_RIGHT,
        KeyEvent.KEYCODE_ALT_LEFT,
        KeyEvent.KEYCODE_ALT_RIGHT,
        KeyEvent.KEYCODE_SHIFT_LEFT,
        KeyEvent.KEYCODE_SHIFT_RIGHT,
        KeyEvent.KEYCODE_META_LEFT,
        KeyEvent.KEYCODE_META_RIGHT,
        KeyEvent.KEYCODE_SYM,
        KeyEvent.KEYCODE_FUNCTION,
        KeyEvent.KEYCODE_CAPS_LOCK,
        KeyEvent.KEYCODE_NUM_LOCK,
        KeyEvent.KEYCODE_SCROLL_LOCK -> true
        else -> false
    }

    /**
     * Supported trigger keys for custom hardware shortcuts:
     * A-Z, 0-9, F1-F12, and standard punctuation keys.
     */
    fun isSupportedTriggerKey(keyCode: Int): Boolean = when (keyCode) {
        in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z,
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9,
        in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12,
        KeyEvent.KEYCODE_MINUS,
        KeyEvent.KEYCODE_EQUALS,
        KeyEvent.KEYCODE_LEFT_BRACKET,
        KeyEvent.KEYCODE_RIGHT_BRACKET,
        KeyEvent.KEYCODE_BACKSLASH,
        KeyEvent.KEYCODE_SEMICOLON,
        KeyEvent.KEYCODE_APOSTROPHE,
        KeyEvent.KEYCODE_COMMA,
        KeyEvent.KEYCODE_PERIOD,
        KeyEvent.KEYCODE_SLASH,
        KeyEvent.KEYCODE_GRAVE -> true
        else -> false
    }

    /**
     * Returns a plain-language reason if a combination is reserved by Android or
     * standard text editing, or null if the combination is safe to assign.
     */
    fun reservedReason(ctrl: Boolean, shift: Boolean, alt: Boolean, keyCode: Int): String? {
        // Standard single-Ctrl text editing combos used across almost all Android apps
        if (ctrl && !alt && !shift) {
            when (keyCode) {
                KeyEvent.KEYCODE_C -> return "Ctrl + C is reserved for Copy."
                KeyEvent.KEYCODE_V -> return "Ctrl + V is reserved for Paste."
                KeyEvent.KEYCODE_X -> return "Ctrl + X is reserved for Cut."
                KeyEvent.KEYCODE_A -> return "Ctrl + A is reserved for Select All."
                KeyEvent.KEYCODE_Z -> return "Ctrl + Z is reserved for Undo."
                KeyEvent.KEYCODE_Y -> return "Ctrl + Y is reserved for Redo."
            }
        }
        // System-level Android / window-switching combinations
        if (keyCode == KeyEvent.KEYCODE_TAB && (alt || ctrl)) {
            return "Tab combinations are reserved by Android for switching apps or tabs."
        }
        if (keyCode == KeyEvent.KEYCODE_SPACE && (ctrl || alt || shift)) {
            return "Spacebar modifier combinations are reserved by Android for language switching."
        }
        if (alt && keyCode == KeyEvent.KEYCODE_F4) {
            return "Alt + F4 is reserved by the system."
        }
        if (ctrl && alt && (keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_FORWARD_DEL)) {
            return "Ctrl + Alt + Delete is reserved by the system."
        }
        if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            return "Escape combinations are reserved for navigation."
        }
        return null
    }

    /**
     * Validates a candidate shortcut before saving.
     * Prevents empty fields, unsupported/reserved combos, oversized macros, and duplicates.
     */
    fun validate(
        c: Context,
        name: String,
        ctrl: Boolean,
        shift: Boolean,
        alt: Boolean,
        keyCode: Int,
        macroText: String,
        editingId: String? = null,
    ): ValidationResult {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return ValidationResult.Invalid("Please enter a readable name for this shortcut.")
        }
        if (trimmedName.length > MAX_NAME_LENGTH) {
            return ValidationResult.Invalid("Shortcut name must be $MAX_NAME_LENGTH characters or fewer.")
        }
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            return ValidationResult.Invalid("Please capture or choose a key combination first.")
        }
        if (isModifierKey(keyCode)) {
            return ValidationResult.Invalid("A modifier key alone cannot be a shortcut. Press a letter, number, or function key with Ctrl or Alt.")
        }
        if (!ctrl && !alt) {
            return ValidationResult.Invalid("Shortcuts must include Ctrl or Alt (for example, Ctrl + Alt + M) so normal typing is not disrupted.")
        }
        if (!isSupportedTriggerKey(keyCode)) {
            return ValidationResult.Invalid("Unsupported key '${Shortcut.keyCodeToLabel(keyCode)}'. Use letters (A–Z), numbers (0–9), function keys (F1–F12), or punctuation.")
        }
        reservedReason(ctrl, shift, alt, keyCode)?.let { reason ->
            return ValidationResult.Invalid("Reserved combination: $reason")
        }
        if (macroText.isEmpty()) {
            return ValidationResult.Invalid("Please enter the text macro to insert.")
        }
        if (macroText.length > MAX_MACRO_LENGTH) {
            return ValidationResult.Invalid("Text macro is too long (${macroText.length} characters). Maximum allowed is $MAX_MACRO_LENGTH characters.")
        }
        val existing = ShortcutRepository.findDuplicate(c, ctrl, shift, alt, keyCode, excludeId = editingId)
        if (existing != null) {
            return ValidationResult.Invalid(
                "Duplicate shortcut: ${Shortcut.formatCombo(ctrl, shift, alt, keyCode)} is already assigned to \"${existing.name}\".",
            )
        }
        return ValidationResult.Valid
    }
}
