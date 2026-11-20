package com.devfahim00.openridmik

import android.os.SystemClock
import android.view.KeyEvent
import android.view.inputmethod.InputConnection

/**
 * Key codes for the on-screen Developer PC keyboard layout (Page 1: QWERTY + Ctrl/Alt/Shift/Esc/Tab/Meta;
 * Page 2: F1-F12 + Arrow navigation + Home/End/PgUp/PgDn/Ins/Del + Cut/Copy/Paste).
 *
 * All codes are negative values <= -100 so they never collide with OpenRidmik's [Codes] (-1..-12)
 * or Unicode character codes (> 0).
 */
object DevCodes {
    const val ESC = -101
    const val TAB = -102
    const val CTRL = -103
    const val ALT = -104
    const val META = -105

    const val UP = -110
    const val DOWN = -111
    const val LEFT = -112
    const val RIGHT = -113
    const val HOME = -114
    const val END = -115
    const val PGUP = -116
    const val PGDN = -117
    const val INS = -118
    const val FWD_DEL = -119

    const val PAGE_TOGGLE = -120

    const val F1 = -131
    const val F2 = -132
    const val F3 = -133
    const val F4 = -134
    const val F5 = -135
    const val F6 = -136
    const val F7 = -137
    const val F8 = -138
    const val F9 = -139
    const val F10 = -140
    const val F11 = -141
    const val F12 = -142

    // Bitmask flags for KeyboardView.activeModifiers highlight
    const val MOD_CTRL = 1
    const val MOD_ALT = 2
    const val MOD_META = 4

    fun isDevCode(code: Int): Boolean = code <= -100
}

/**
 * Builds the 2-page on-screen Developer PC keyboard and sends real Android [KeyEvent]s
 * with full modifier meta-state (Ctrl / Alt / Shift / Meta).
 */
object DeveloperKeyboard {

    private fun spacer(w: Float) = Key("", 0, "", weight = w, spacer = true)

    private fun devKey(
        label: String,
        code: Int,
        weight: Float = 1f,
        special: Boolean = true,
        repeat: Boolean = false,
    ) = Key(label = label, code = code, out = "", weight = weight, special = special, repeat = repeat)

    private fun charKey(main: Char, hintSym: Char, shift: Boolean): Key {
        val label = if (shift && main.isLetter()) main.uppercaseChar().toString() else main.toString()
        val out = label
        return Key(
            label = label,
            code = out[0].code,
            out = out,
            hint = hintSym.toString(),
            alt = hintSym.toString(),
        )
    }

    private fun digitKey(digit: Char, shiftedSym: Char, shift: Boolean): Key {
        val label = if (shift) shiftedSym.toString() else digit.toString()
        val hint = if (shift) digit.toString() else shiftedSym.toString()
        return Key(
            label = label,
            code = label[0].code,
            out = label,
            hint = hint,
            alt = hint,
        )
    }

    fun buildPage(page: Int, shift: Boolean): List<List<Key>> =
        if (page == 0) buildPage1(shift) else buildPage2()

    /**
     * Page 1: Full PC Coding Layout
     * Row 1: Esc | Tab | Shift | Alt | ⌘ | ⌫
     * Row 2: 1! 2@ 3# 4$ 5% 6^ 7& 8* 9( 0)
     * Row 3: q` w~ e= r+ t- y_ u< i> o[ p]
     * Row 4: a@ s# d$ f% g| h& j, k. l;
     * Row 5: Shift z{ x} c: v" b' n/ m\ ⌫
     * Row 6: Ctrl | Sym | Space (Developer) | Enter
     */
    fun buildPage1(shift: Boolean): List<List<Key>> {
        val row1 = listOf(
            devKey("Esc", DevCodes.ESC, weight = 1.5f),
            devKey("Tab", DevCodes.TAB, weight = 1.5f),
            Key("Shift", Codes.SHIFT, "", weight = 1.8f, special = true),
            devKey("Alt", DevCodes.ALT, weight = 1.5f),
            devKey("⌘", DevCodes.META, weight = 1.5f),
            Key("", Codes.DELETE, "", weight = 1.5f, special = true, repeat = true),
        )

        val digits = "1234567890"
        val syms = "!@#$%^&*()"
        val row2 = digits.indices.map { i -> digitKey(digits[i], syms[i], shift) }

        val r3Chars = "qwertyuiop"
        val r3Hints = "`~=+-_<>[]"
        val row3 = r3Chars.indices.map { i -> charKey(r3Chars[i], r3Hints[i], shift) }

        val r4Chars = "asdfghjkl"
        val r4Hints = "@#$%|&,.;"
        val row4 = listOf(spacer(0.5f)) +
            r4Chars.indices.map { i -> charKey(r4Chars[i], r4Hints[i], shift) } +
            listOf(spacer(0.5f))

        val r5Chars = "zxcvbnm"
        val r5Hints = "{}:\"'/\\"
        val row5 = listOf(Key("", Codes.SHIFT, "", weight = 1.5f, special = true)) +
            r5Chars.indices.map { i -> charKey(r5Chars[i], r5Hints[i], shift) } +
            listOf(Key("", Codes.DELETE, "", weight = 1.5f, special = true, repeat = true))

        val row6 = listOf(
            devKey("Ctrl", DevCodes.CTRL, weight = 1.5f),
            Key("Sym", Codes.SYMBOLS, "", weight = 1.3f, special = true),
            Key("", Codes.SPACE, " ", weight = 5.2f),
            Key("", Codes.ENTER, "\n", weight = 1.8f, special = true),
        )

        return listOf(row1, row2, row3, row4, row5, row6)
    }

    /**
     * Page 2: Function Keys (F1-F12) + Complete PC Navigation (Arrows, Home, End, PgUp, PgDn, Ins, Del)
     * Accessed by swiping left/right across the Developer keyboard body (or tapping ABC to return).
     */
    fun buildPage2(): List<List<Key>> {
        val row1 = listOf(
            devKey("F1", DevCodes.F1, special = false),
            devKey("F2", DevCodes.F2, special = false),
            devKey("F3", DevCodes.F3, special = false),
            devKey("F4", DevCodes.F4, special = false),
            devKey("F5", DevCodes.F5, special = false),
            devKey("F6", DevCodes.F6, special = false),
        )
        val row2 = listOf(
            devKey("F7", DevCodes.F7, special = false),
            devKey("F8", DevCodes.F8, special = false),
            devKey("F9", DevCodes.F9, special = false),
            devKey("F10", DevCodes.F10, special = false),
            devKey("F11", DevCodes.F11, special = false),
            devKey("F12", DevCodes.F12, special = false),
        )
        val row3 = listOf(
            devKey("Esc", DevCodes.ESC),
            devKey("Home", DevCodes.HOME),
            devKey("↑", DevCodes.UP, special = false, repeat = true),
            devKey("End", DevCodes.END),
            devKey("PgUp", DevCodes.PGUP),
            devKey("Ins", DevCodes.INS),
        )
        val row4 = listOf(
            devKey("Tab", DevCodes.TAB),
            devKey("←", DevCodes.LEFT, special = false, repeat = true),
            devKey("↓", DevCodes.DOWN, special = false, repeat = true),
            devKey("→", DevCodes.RIGHT, special = false, repeat = true),
            devKey("PgDn", DevCodes.PGDN),
            devKey("Del", DevCodes.FWD_DEL, repeat = true),
        )
        val row5 = listOf(
            devKey("Ctrl", DevCodes.CTRL, weight = 1.2f),
            devKey("Alt", DevCodes.ALT, weight = 1.1f),
            Key("", Codes.SHIFT, "", weight = 1.2f, special = true),
            devKey("⌘", DevCodes.META, weight = 1.0f),
            Key("Cut", Codes.CUT, "", weight = 1.1f, special = true),
            Key("Copy", Codes.COPY, "", weight = 1.2f, special = true),
            Key("Paste", Codes.PASTE, "", weight = 1.3f, special = true),
            Key("", Codes.DELETE, "", weight = 1.2f, special = true, repeat = true),
        )
        val row6 = listOf(
            devKey("ABC", DevCodes.PAGE_TOGGLE, weight = 1.5f),
            Key("Sym", Codes.SYMBOLS, "", weight = 1.3f, special = true),
            Key("", Codes.SPACE, " ", weight = 5.2f),
            Key("", Codes.ENTER, "\n", weight = 1.8f, special = true),
        )
        return listOf(row1, row2, row3, row4, row5, row6)
    }

    /** Maps a [DevCodes] constant to the corresponding Android [KeyEvent] keyCode. */
    fun toAndroidKeyCode(devCode: Int): Int = when (devCode) {
        DevCodes.ESC -> KeyEvent.KEYCODE_ESCAPE
        DevCodes.TAB -> KeyEvent.KEYCODE_TAB
        DevCodes.UP -> KeyEvent.KEYCODE_DPAD_UP
        DevCodes.DOWN -> KeyEvent.KEYCODE_DPAD_DOWN
        DevCodes.LEFT -> KeyEvent.KEYCODE_DPAD_LEFT
        DevCodes.RIGHT -> KeyEvent.KEYCODE_DPAD_RIGHT
        DevCodes.HOME -> KeyEvent.KEYCODE_MOVE_HOME
        DevCodes.END -> KeyEvent.KEYCODE_MOVE_END
        DevCodes.PGUP -> KeyEvent.KEYCODE_PAGE_UP
        DevCodes.PGDN -> KeyEvent.KEYCODE_PAGE_DOWN
        DevCodes.INS -> KeyEvent.KEYCODE_INSERT
        DevCodes.FWD_DEL -> KeyEvent.KEYCODE_FORWARD_DEL
        DevCodes.F1 -> KeyEvent.KEYCODE_F1
        DevCodes.F2 -> KeyEvent.KEYCODE_F2
        DevCodes.F3 -> KeyEvent.KEYCODE_F3
        DevCodes.F4 -> KeyEvent.KEYCODE_F4
        DevCodes.F5 -> KeyEvent.KEYCODE_F5
        DevCodes.F6 -> KeyEvent.KEYCODE_F6
        DevCodes.F7 -> KeyEvent.KEYCODE_F7
        DevCodes.F8 -> KeyEvent.KEYCODE_F8
        DevCodes.F9 -> KeyEvent.KEYCODE_F9
        DevCodes.F10 -> KeyEvent.KEYCODE_F10
        DevCodes.F11 -> KeyEvent.KEYCODE_F11
        DevCodes.F12 -> KeyEvent.KEYCODE_F12
        else -> KeyEvent.KEYCODE_UNKNOWN
    }

    /** Maps a printable character from the Developer QWERTY layout to its Android [KeyEvent] keyCode. */
    fun charToKeyCode(ch: Char): Int = when (ch.lowercaseChar()) {
        in 'a'..'z' -> KeyEvent.KEYCODE_A + (ch.lowercaseChar() - 'a')
        in '0'..'9' -> KeyEvent.KEYCODE_0 + (ch - '0')
        ' ' -> KeyEvent.KEYCODE_SPACE
        '-', '_' -> KeyEvent.KEYCODE_MINUS
        '=', '+' -> KeyEvent.KEYCODE_EQUALS
        '[', '{' -> KeyEvent.KEYCODE_LEFT_BRACKET
        ']', '}' -> KeyEvent.KEYCODE_RIGHT_BRACKET
        '\\', '|' -> KeyEvent.KEYCODE_BACKSLASH
        ';', ':' -> KeyEvent.KEYCODE_SEMICOLON
        '\'', '"' -> KeyEvent.KEYCODE_APOSTROPHE
        ',', '<' -> KeyEvent.KEYCODE_COMMA
        '.', '>' -> KeyEvent.KEYCODE_PERIOD
        '/', '?' -> KeyEvent.KEYCODE_SLASH
        '`', '~' -> KeyEvent.KEYCODE_GRAVE
        else -> KeyEvent.KEYCODE_UNKNOWN
    }

    fun buildMetaState(ctrl: Boolean, alt: Boolean, shift: Boolean, meta: Boolean): Int {
        var m = 0
        if (ctrl) m = m or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (alt) m = m or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (shift) m = m or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        if (meta) m = m or KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
        return m
    }

    /** Sends a real Android key down + key up event with active modifier meta-state. */
    fun sendKey(ic: InputConnection, keyCode: Int, ctrl: Boolean, alt: Boolean, shift: Boolean, meta: Boolean) {
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) return
        val now = SystemClock.uptimeMillis()
        val metaState = buildMetaState(ctrl, alt, shift, meta)
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, metaState))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, metaState))
    }
}
