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
 * Unit tests for My Avro Keyboard Developer Layout, shortcut logic, and security guards.
 */
class ShortcutLogicTest {

    @Test
    fun developerKeyboard_page1AndPage2_buildExpectedStructure() {
        val p1 = DeveloperKeyboard.buildPage1(shift = false)
        assertEquals(6, p1.size)
        // Row 1: Esc, Tab, Shift, Alt, ⌘, Backspace
        assertEquals(DevCodes.ESC, p1[0][0].code)
        assertEquals(DevCodes.TAB, p1[0][1].code)
        assertEquals(Codes.SHIFT, p1[0][2].code)
        assertEquals(DevCodes.ALT, p1[0][3].code)
        assertEquals(DevCodes.META, p1[0][4].code)
        assertEquals(Codes.DELETE, p1[0][5].code)
        // Row 6: Ctrl, Sym, Space, Enter
        assertEquals(DevCodes.CTRL, p1[5][0].code)
        assertEquals(Codes.SYMBOLS, p1[5][1].code)
        assertEquals(Codes.SPACE, p1[5][2].code)
        assertEquals(Codes.ENTER, p1[5][3].code)

        val p2 = DeveloperKeyboard.buildPage2()
        assertEquals(6, p2.size)
        assertEquals(DevCodes.F1, p2[0][0].code)
        assertEquals(DevCodes.F12, p2[1][5].code)
        assertEquals(DevCodes.UP, p2[2][2].code)
        assertEquals(DevCodes.LEFT, p2[3][1].code)
        assertEquals(DevCodes.DOWN, p2[3][2].code)
        assertEquals(DevCodes.RIGHT, p2[3][3].code)
    }

    @Test
    fun developerKeyboard_mapsFunctionalKeysToAndroidKeyCodes() {
        assertEquals(KeyEvent.KEYCODE_ESCAPE, DeveloperKeyboard.toAndroidKeyCode(DevCodes.ESC))
        assertEquals(KeyEvent.KEYCODE_TAB, DeveloperKeyboard.toAndroidKeyCode(DevCodes.TAB))
        assertEquals(KeyEvent.KEYCODE_DPAD_UP, DeveloperKeyboard.toAndroidKeyCode(DevCodes.UP))
        assertEquals(KeyEvent.KEYCODE_DPAD_DOWN, DeveloperKeyboard.toAndroidKeyCode(DevCodes.DOWN))
        assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, DeveloperKeyboard.toAndroidKeyCode(DevCodes.LEFT))
        assertEquals(KeyEvent.KEYCODE_DPAD_RIGHT, DeveloperKeyboard.toAndroidKeyCode(DevCodes.RIGHT))
        assertEquals(KeyEvent.KEYCODE_F1, DeveloperKeyboard.toAndroidKeyCode(DevCodes.F1))
        assertEquals(KeyEvent.KEYCODE_F12, DeveloperKeyboard.toAndroidKeyCode(DevCodes.F12))
        assertEquals(KeyEvent.KEYCODE_MOVE_HOME, DeveloperKeyboard.toAndroidKeyCode(DevCodes.HOME))
        assertEquals(KeyEvent.KEYCODE_MOVE_END, DeveloperKeyboard.toAndroidKeyCode(DevCodes.END))
        assertEquals(KeyEvent.KEYCODE_FORWARD_DEL, DeveloperKeyboard.toAndroidKeyCode(DevCodes.FWD_DEL))
    }

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
    fun reservedCombos_areBlocked_whileCustomCombos_areAllowed() {
        assertNotNull(ShortcutManager.reservedReason(ctrl = true, shift = false, alt = false, keyCode = KeyEvent.KEYCODE_C))
        assertNotNull(ShortcutManager.reservedReason(ctrl = true, shift = false, alt = false, keyCode = KeyEvent.KEYCODE_V))
        assertNotNull(ShortcutManager.reservedReason(ctrl = false, shift = false, alt = true, keyCode = KeyEvent.KEYCODE_TAB))
        assertNotNull(ShortcutManager.reservedReason(ctrl = true, shift = false, alt = false, keyCode = KeyEvent.KEYCODE_SPACE))

        assertNull(ShortcutManager.reservedReason(ctrl = true, shift = false, alt = true, keyCode = KeyEvent.KEYCODE_M))
        assertNull(ShortcutManager.reservedReason(ctrl = true, shift = true, alt = false, keyCode = KeyEvent.KEYCODE_B))
        assertNull(ShortcutManager.reservedReason(ctrl = false, shift = true, alt = true, keyCode = KeyEvent.KEYCODE_1))
    }

    @Test
    fun passwordFields_areDetectedAndBlocked() {
        val textPwd = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val numPin = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        val normalText = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        assertTrue(MacroExecutor.isPasswordField(textPwd))
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
        val res = MacroExecutor.execute(null, null, sc) {}
        assertEquals(MacroExecutor.Result.NO_INPUT_CONNECTION, res)
    }
}
