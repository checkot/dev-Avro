package com.devfahim00.openridmik

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

/**
 * Safely executes text macros into the currently focused Android input field.
 *
 * Security & reliability guarantees:
 * - Blocks execution inside password and numeric-PIN fields.
 * - Handles null or dead [InputConnection] instances without crashing.
 * - Flushes any ongoing Avro phonetic composition before inserting text so Bangla
 *   composition is never corrupted.
 * - Uses [InputConnection.commitText], which inserts at the current cursor position
 *   and naturally replaces any active selection where Android permits.
 */
object MacroExecutor {

    enum class Result {
        SUCCESS,
        BLOCKED_PASSWORD,
        NO_INPUT_CONNECTION,
        EMPTY_MACRO,
        TOO_LONG,
        ERROR,
    }

    /**
     * Checks whether [info] describes a password or numeric-PIN field.
     * Checked dynamically on every execution so hardware-keyboard sessions
     * (where onStartInputView may not have run) are always protected.
     */
    fun isPasswordField(info: EditorInfo?): Boolean {
        if (info == null) return false
        val inputType = info.inputType
        val cls = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return when (cls) {
            InputType.TYPE_CLASS_TEXT -> variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }
    }

    /**
     * Inserts [shortcut.macroText] safely into [ic].
     *
     * @param flushComposing callback provided by [OpenRidmikService] to cleanly commit
     *   any in-progress Avro phonetic buffer before inserting the macro.
     */
    fun execute(
        ic: InputConnection?,
        editorInfo: EditorInfo?,
        shortcut: Shortcut,
        flushComposing: () -> Unit,
    ): Result {
        if (ic == null) return Result.NO_INPUT_CONNECTION
        if (isPasswordField(editorInfo)) return Result.BLOCKED_PASSWORD
        val text = shortcut.macroText
        if (text.isEmpty()) return Result.EMPTY_MACRO
        if (text.length > ShortcutManager.MAX_MACRO_LENGTH) return Result.TOO_LONG

        return try {
            ic.beginBatchEdit()
            try {
                flushComposing()
                val committed = ic.commitText(text, 1)
                if (committed) Result.SUCCESS else Result.ERROR
            } finally {
                ic.endBatchEdit()
            }
        } catch (_: Exception) {
            Result.ERROR
        }
    }
}
