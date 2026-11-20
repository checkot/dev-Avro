package com.devfahim00.openridmik

import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for My Avro Keyboard Developer Mode shortcut logic and security guards.
 */
class ShortcutLogicTest {

    @Test
    fun comboKey_and_displayCombo_formatConsistently() {
        val sc = Shortcut(
            id = "test-1",
            name = "BMR Verified",
            ctrl = true,
            shift = false,
            alt = true,
            keyCode = KeyEvent.KEYCODE_M,
            macroText = "Batch manufacturing record reviewed and verified.",
            enabled = true,
        )
        assertEquals("C:1|S:0|A:1|K:${KeyEvent.KEYCODE_M}", sc.comboKey())
        assertEquals("Ctrl + Alt + M", sc.displayCombo())
    }

    @Test
    fun displayCombo_supportsDigitsAndShiftCombinations() {
        assertEquals(
            "Alt + Shift + 1",
            Shortcut.formatCombo(ctrl = false, shift = true, alt = true, keyCode = KeyEvent.KEYCODE_1),
        )
        assertEquals(
            "Ctrl + Shift + B",
            Shortcut.formatCombo(ctrl = true, shift = true, alt = false, keyCode = KeyEvent.KEYCODE_B),
        )
    }

    @Test
    fun reservedCombos_areBlocked_whileCustomCombos_areAllowed() {
        // Reserved standard editing & system shortcuts
        assertNotNull(ShortcutManager.reservedReason(ctrl = true, shift = false, alt = false, keyCode = KeyEvent.KEYCODE_C))
        assertNotNull(ShortcutManager.reservedReason(ctrl = true, shift = false, alt = false, keyCode = KeyEvent.KEYCODE_V))
        assertNotNull(ShortcutManager.reservedReason(ctrl = true, shift = false, alt = false, keyCode = KeyEvent.KEYCODE_X))
        assertNotNull(ShortcutManager.reservedReason(ctrl = true, shift = false, alt = false, keyCode = KeyEvent.KEYCODE_A))
        assertNotNull(ShortcutManager.reservedReason(ctrl = true, shift = false, alt = false, keyCode = KeyEvent.KEYCODE_Z))
        assertNotNull(ShortcutManager.reservedReason(ctrl = false, shift = false, alt = true, keyCode = KeyEvent.KEYCODE_TAB))
        assertNotNull(ShortcutManager.reservedReason(ctrl = true, shift = false, alt = false, keyCode = KeyEvent.KEYCODE_SPACE))
        assertNotNull(ShortcutManager.reservedReason(ctrl = false, shift = false, alt = true, keyCode = KeyEvent.KEYCODE_F4))

        // Allowed custom developer shortcuts
        assertNull(ShortcutManager.reservedReason(ctrl = true, shift = false, alt = true, keyCode = KeyEvent.KEYCODE_M))
        assertNull(ShortcutManager.reservedReason(ctrl = true, shift = true, alt = false, keyCode = KeyEvent.KEYCODE_B))
        assertNull(ShortcutManager.reservedReason(ctrl = false, shift = true, alt = true, keyCode = KeyEvent.KEYCODE_1))
        assertNull(ShortcutManager.reservedReason(ctrl = true, shift = true, alt = false, keyCode = KeyEvent.KEYCODE_E))
    }

    @Test
    fun modifierAndTriggerKeyClassification_worksCorrectly() {
        assertTrue(ShortcutManager.isModifierKey(KeyEvent.KEYCODE_CTRL_LEFT))
        assertTrue(ShortcutManager.isModifierKey(KeyEvent.KEYCODE_ALT_RIGHT))
        assertTrue(ShortcutManager.isModifierKey(KeyEvent.KEYCODE_SHIFT_LEFT))
        assertFalse(ShortcutManager.isModifierKey(KeyEvent.KEYCODE_M))

        assertTrue(ShortcutManager.isSupportedTriggerKey(KeyEvent.KEYCODE_M))
        assertTrue(ShortcutManager.isSupportedTriggerKey(KeyEvent.KEYCODE_1))
        assertTrue(ShortcutManager.isSupportedTriggerKey(KeyEvent.KEYCODE_F5))
        assertFalse(ShortcutManager.isSupportedTriggerKey(KeyEvent.KEYCODE_VOLUME_UP))
        assertFalse(ShortcutManager.isSupportedTriggerKey(KeyEvent.KEYCODE_ENTER))
    }

    @Test
    fun passwordFields_areDetectedAndBlocked() {
        val textPwd = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val visiblePwd = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        val webPwd = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        }
        val numPin = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        val normalText = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }

        assertTrue(MacroExecutor.isPasswordField(textPwd))
        assertTrue(MacroExecutor.isPasswordField(visiblePwd))
        assertTrue(MacroExecutor.isPasswordField(webPwd))
        assertTrue(MacroExecutor.isPasswordField(numPin))
        assertFalse(MacroExecutor.isPasswordField(normalText))
        assertFalse(MacroExecutor.isPasswordField(null))
    }

    @Test
    fun macroExecutor_handlesNullInputConnectionSafely() {
        val sc = Shortcut(
            name = "Signature",
            ctrl = true,
            shift = true,
            alt = false,
            keyCode = KeyEvent.KEYCODE_E,
            macroText = "Regards,\nMd. Ariful Islam",
        )
        val res = MacroExecutor.execute(
            ic = null,
            editorInfo = null,
            shortcut = sc,
            flushComposing = {},
        )
        assertEquals(MacroExecutor.Result.NO_INPUT_CONNECTION, res)
    }
}
