package com.devfahim00.openridmik

import android.view.KeyEvent
import org.json.JSONObject
import java.util.UUID

/**
 * Represents a single user-configured hardware keyboard shortcut and its text macro.
 *
 * Stored locally on-device via [ShortcutRepository]. Never transmitted externally.
 */
data class Shortcut(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ctrl: Boolean,
    val shift: Boolean,
    val alt: Boolean,
    val keyCode: Int,
    val macroText: String,
    val enabled: Boolean = true,
) {
    /** Canonical key-combination signature used for O(1) lookup and duplicate detection. */
    fun comboKey(): String = comboKey(ctrl, shift, alt, keyCode)

    /** Human-readable shortcut label such as "Ctrl + Alt + M" or "Alt + Shift + 1". */
    fun displayCombo(): String = formatCombo(ctrl, shift, alt, keyCode)

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("ctrl", ctrl)
        put("shift", shift)
        put("alt", alt)
        put("keyCode", keyCode)
        put("macroText", macroText)
        put("enabled", enabled)
    }

    companion object {
        fun comboKey(ctrl: Boolean, shift: Boolean, alt: Boolean, keyCode: Int): String {
            val c = if (ctrl) 1 else 0
            val s = if (shift) 1 else 0
            val a = if (alt) 1 else 0
            return "C:$c|S:$s|A:$a|K:$keyCode"
        }

        fun formatCombo(ctrl: Boolean, shift: Boolean, alt: Boolean, keyCode: Int): String {
            val parts = ArrayList<String>(4)
            if (ctrl) parts.add("Ctrl")
            if (alt) parts.add("Alt")
            if (shift) parts.add("Shift")
            parts.add(keyCodeToLabel(keyCode))
            return parts.joinToString(" + ")
        }

        fun keyCodeToLabel(keyCode: Int): String = when (keyCode) {
            in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z ->
                ('A' + (keyCode - KeyEvent.KEYCODE_A)).toString()
            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 ->
                ('0' + (keyCode - KeyEvent.KEYCODE_0)).toString()
            in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12 ->
                "F${keyCode - KeyEvent.KEYCODE_F1 + 1}"
            KeyEvent.KEYCODE_MINUS -> "-"
            KeyEvent.KEYCODE_EQUALS -> "="
            KeyEvent.KEYCODE_LEFT_BRACKET -> "["
            KeyEvent.KEYCODE_RIGHT_BRACKET -> "]"
            KeyEvent.KEYCODE_BACKSLASH -> "\\"
            KeyEvent.KEYCODE_SEMICOLON -> ";"
            KeyEvent.KEYCODE_APOSTROPHE -> "'"
            KeyEvent.KEYCODE_COMMA -> ","
            KeyEvent.KEYCODE_PERIOD -> "."
            KeyEvent.KEYCODE_SLASH -> "/"
            KeyEvent.KEYCODE_GRAVE -> "`"
            KeyEvent.KEYCODE_SPACE -> "Space"
            KeyEvent.KEYCODE_ENTER -> "Enter"
            KeyEvent.KEYCODE_TAB -> "Tab"
            KeyEvent.KEYCODE_ESCAPE -> "Esc"
            KeyEvent.KEYCODE_DEL -> "Backspace"
            KeyEvent.KEYCODE_FORWARD_DEL -> "Delete"
            else -> KeyEvent.keyCodeToString(keyCode)
                .removePrefix("KEYCODE_")
                .replace('_', ' ')
        }

        fun fromJson(obj: JSONObject): Shortcut? {
            val id = obj.optString("id", "").trim()
            val name = obj.optString("name", "").trim()
            val keyCode = obj.optInt("keyCode", KeyEvent.KEYCODE_UNKNOWN)
            val macroText = obj.optString("macroText", "")
            if (name.isEmpty() || keyCode == KeyEvent.KEYCODE_UNKNOWN || macroText.isEmpty()) {
                return null
            }
            return Shortcut(
                id = if (id.isNotEmpty()) id else UUID.randomUUID().toString(),
                name = name,
                ctrl = obj.optBoolean("ctrl", false),
                shift = obj.optBoolean("shift", false),
                alt = obj.optBoolean("alt", false),
                keyCode = keyCode,
                macroText = macroText,
                enabled = obj.optBoolean("enabled", true),
            )
        }
    }
}
