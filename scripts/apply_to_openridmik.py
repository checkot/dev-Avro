#!/usr/bin/env python3
"""
Applies My Avro Keyboard (On-Screen PC Developer Layout + Swipeable Fn/Arrows Page +
Developer Mode Hardware Shortcuts & Text Macros) cleanly onto https://github.com/devfahim00/OpenRidmik.

Usage:
    python3 scripts/apply_to_openridmik.py <path-to-openridmik-repo>
"""

import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

OVERLAY_FILES = [
    "settings.gradle.kts",
    "app/build.gradle.kts",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/res/values/strings.xml",
    "app/src/main/java/com/devfahim00/openridmik/Prefs.kt",
    "app/src/main/java/com/devfahim00/openridmik/DeveloperKeyboard.kt",
    "app/src/main/java/com/devfahim00/openridmik/Shortcut.kt",
    "app/src/main/java/com/devfahim00/openridmik/ShortcutRepository.kt",
    "app/src/main/java/com/devfahim00/openridmik/ShortcutManager.kt",
    "app/src/main/java/com/devfahim00/openridmik/MacroExecutor.kt",
    "app/src/main/java/com/devfahim00/openridmik/DeveloperSettingsActivity.kt",
    "app/src/main/java/com/devfahim00/openridmik/ShortcutCaptureActivity.kt",
    "app/src/test/java/com/devfahim00/openridmik/ShortcutLogicTest.kt",
]


def replace_regex_once(src: str, pattern: str, replacement: str, label: str, already_marker: str) -> str:
    if already_marker in src:
        return src
    m = re.search(pattern, src, flags=re.MULTILINE)
    if not m:
        raise RuntimeError(f"Anchor not found for {label}")
    return src[:m.start()] + replacement + src[m.end():]


def replace_once(src: str, anchor: str, replacement: str, label: str, already_marker: str) -> str:
    if already_marker in src:
        return src
    if anchor not in src:
        raise RuntimeError(f"Anchor not found for {label}")
    return src.replace(anchor, replacement, 1)


def patch_keyboard_view(target: Path) -> None:
    kv = target / "app/src/main/java/com/devfahim00/openridmik/KeyboardView.kt"
    src = kv.read_text(encoding="utf-8").replace("\r\n", "\n")

    src = replace_regex_once(
        src,
        r"fun onSpaceSwipe\(dir: Int\)\s*\{\s*\}",
        "fun onSpaceSwipe(dir: Int) {}\n\n        /** Main Developer keyboard dragged sideways to flip between PC QWERTY and Fn/Arrows page. */\n        fun onBoardSwipe(dir: Int) {}",
        "KeyboardView.Listener",
        "fun onBoardSwipe",
    )

    src = replace_regex_once(
        src,
        r"var shiftState = 0\n\s*set\(v\) \{\n\s*field = v\n\s*invalidate\(\)\n\s*\}",
        """var shiftState = 0
        set(v) {
            field = v
            invalidate()
        }
    /** Active sticky modifiers bitmask (DevCodes.MOD_CTRL / MOD_ALT / MOD_META) for highlight. */
    var activeModifiers = 0
        set(v) {
            if (field == v) return
            field = v
            invalidate()
        }
    /** When true (Developer layout), horizontal swipe across the board flips between Page 1 and Page 2. */
    var boardSwipeEnabled = false
    private var boardSwipeId = -1
    private var boardSwipeStartX = 0f
    private var boardSwipeStartY = 0f
    private var boardSwipeFired = false

    private fun isModifierActive(code: Int): Boolean = when (code) {
        DevCodes.CTRL -> (activeModifiers and DevCodes.MOD_CTRL) != 0
        DevCodes.ALT -> (activeModifiers and DevCodes.MOD_ALT) != 0
        DevCodes.META -> (activeModifiers and DevCodes.MOD_META) != 0
        else -> false
    }""",
        "KeyboardView props",
        "var activeModifiers",
    )

    src = replace_regex_once(
        src,
        r"val isEnter = k\.code == Codes\.ENTER\n"
        r"\s*fill\.color = when \{\n"
        r"\s*pressed -> cPressed\n"
        r"\s*isEnter -> cAccent\n"
        r"\s*k\.special -> cSpecial\n"
        r"\s*else -> cKey\n"
        r"\s*\}\n"
        r"\s*canvas\.drawRoundRect\(box, rad, rad, fill\)\n"
        r"\s*val fg = if \(isEnter && !pressed\) cAccentText else cText",
        """val isEnter = k.code == Codes.ENTER
                val isModOn = isModifierActive(k.code)
                fill.color = when {
                    pressed -> cPressed
                    isEnter || isModOn -> cAccent
                    k.special -> cSpecial
                    else -> cKey
                }
                canvas.drawRoundRect(box, rad, rad, fill)
                val fg = if ((isEnter || isModOn) && !pressed) cAccentText else cText""",
        "KeyboardView onDraw",
        "val isModOn",
    )

    src = replace_regex_once(
        src,
        r"for \(p in presses\.values\) \{\n\s*if \(p\.swiping\) drawSwipePreview\(canvas, p\) else drawPreview\(canvas, p\)\n\s*\}",
        """for (p in presses.values) {
            if (p.swiping) {
                if (p.key.code == Codes.SPACE) drawSwipePreview(canvas, p)
            } else {
                drawPreview(canvas, p)
            }
        }""",
        "KeyboardView preview",
        "if (p.key.code == Codes.SPACE) drawSwipePreview",
    )

    src = replace_regex_once(
        src,
        r"val p = Press\(h\.first, h\.second, x\)\n\s*presses\[id\] = p",
        """val p = Press(h.first, h.second, x)
        presses[id] = p
        if (boardSwipeEnabled && h.first.code != Codes.SPACE && !h.first.repeat) {
            boardSwipeId = id
            boardSwipeStartX = x
            boardSwipeStartY = y
            boardSwipeFired = false
        }""",
        "KeyboardView down",
        "boardSwipeStartX = x",
    )

    src = replace_once(
        src,
        "        if (p.longDone || p.key.repeat) return",
        """        if (boardSwipeEnabled && id == boardSwipeId && !p.longDone) {
            val dx = x - boardSwipeStartX
            val dy = y - boardSwipeStartY
            if (!boardSwipeFired && Math.abs(dx) > dp(64f) && Math.abs(dx) > Math.abs(dy) * 1.8f) {
                boardSwipeFired = true
                p.swiping = true
                handler.removeCallbacks(p.longRun)
                if (haptic) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                listener?.onBoardSwipe(if (dx > 0) 1 else -1)
                invalidate()
                return
            }
            if (boardSwipeFired) return
        }
        if (p.longDone || p.key.repeat) return""",
        "KeyboardView move",
        "listener?.onBoardSwipe",
    )

    src = replace_regex_once(
        src,
        r"private fun up\(id: Int\) \{\n\s*val p = presses\.remove\(id\) \?: return",
        """private fun up(id: Int) {
        if (id == boardSwipeId) {
            boardSwipeId = -1
            boardSwipeFired = false
        }
        val p = presses.remove(id) ?: return""",
        "KeyboardView up",
        "if (id == boardSwipeId)",
    )

    src = replace_once(
        src,
        "        presses.clear()",
        """        presses.clear()
        boardSwipeId = -1
        boardSwipeFired = false""",
        "KeyboardView cancelAll",
        "boardSwipeFired = false\n    }",
    )

    kv.write_text(src, encoding="utf-8")
    print("[OK] Patched KeyboardView.kt")


SVC_FIELD_ANCHOR = "    private var gearBtn: IconView? = null"
SVC_FIELD_REPLACE = """    private var gearBtn: IconView? = null
    private var developer = false
    private var devPage = 0
    private var devCtrl = false
    private var devAlt = false
    private var devMeta = false
    private var consumedShortcutKeyCode: Int = KeyEvent.KEYCODE_UNKNOWN"""

SVC_START_VIEW_ANCHOR = "    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {"
SVC_START_VIEW_REPLACE = """    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        consumedShortcutKeyCode = KeyEvent.KEYCODE_UNKNOWN
        passwordField = MacroExecutor.isPasswordField(attribute)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null || !Prefs.isDeveloperMode(this)) {
            return super.onKeyDown(keyCode, event)
        }
        if (event.repeatCount > 0) {
            if (consumedShortcutKeyCode == keyCode) return true
            return super.onKeyDown(keyCode, event)
        }
        val shortcut = ShortcutManager.findMatch(this, event) ?: return super.onKeyDown(keyCode, event)
        consumedShortcutKeyCode = keyCode
        val ic = currentInputConnection
        val result = MacroExecutor.execute(
            ic = ic,
            editorInfo = currentInputEditorInfo,
            shortcut = shortcut,
            flushComposing = {
                if (ic != null && raw.isNotEmpty()) {
                    finishComposing(ic)
                }
            },
        )
        if (result == MacroExecutor.Result.BLOCKED_PASSWORD) {
            Toast.makeText(this, "Macros are disabled in password fields", Toast.LENGTH_SHORT).show()
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (consumedShortcutKeyCode != KeyEvent.KEYCODE_UNKNOWN && consumedShortcutKeyCode == keyCode) {
            consumedShortcutKeyCode = KeyEvent.KEYCODE_UNKNOWN
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {"""

SVC_LANG_INIT_ANCHOR = """        bangla = if (forceLatin) false else Prefs.isBangla(this)
        probhat = Prefs.layout(this) == "probhat"
        jatiyo = Prefs.layout(this) == "jatiyo\""""
SVC_LANG_INIT_REPLACE = """        developer = !forceLatin && Prefs.isDeveloperLayout(this)
        bangla = if (forceLatin || developer) false else Prefs.isBangla(this)
        probhat = Prefs.layout(this) == "probhat"
        jatiyo = Prefs.layout(this) == "jatiyo"
        devPage = 0
        clearDevModifiers()"""

SVC_REBUILD_ROWS_ANCHOR = """            Mode.ALPHA -> Layouts.alpha(
                shift > 0, bangla,
                Prefs.get(this, "numberRow"), Prefs.get(this, "banglaDigits"),
                probhat,
                jatiyo,
            )"""
SVC_REBUILD_ROWS_REPLACE = """            Mode.ALPHA -> if (developer) {
                DeveloperKeyboard.buildPage(devPage, shift > 0)
            } else {
                Layouts.alpha(
                    shift > 0, bangla,
                    Prefs.get(this, "numberRow"), Prefs.get(this, "banglaDigits"),
                    probhat,
                    jatiyo,
                )
            }"""

SVC_SPACE_LABEL_ANCHOR = """        kbView.spaceLabel = when {
            !bangla -> "English\""""
SVC_SPACE_LABEL_REPLACE = """        kbView.boardSwipeEnabled = (mode == Mode.ALPHA && developer)
        kbView.activeModifiers = devModifierMask()
        kbView.spaceLabel = when {
            developer -> if (devPage == 0) "Developer" else "Developer · Fn"
            !bangla -> "English\""""

SVC_HANDLE_KEY_ANCHOR = """        when (key.code) {
            Codes.SHIFT -> handleShift()"""
SVC_HANDLE_KEY_REPLACE = """        if (developer && handleDeveloperKey(ic, key)) return
        when (key.code) {
            Codes.SHIFT -> handleShift()"""

SVC_CURRENT_LANG_REGEX = (
    r"    /\*\* 0 = English.*?private fun setLang\(idx: Int\) \{.*?\n    \}"
)
SVC_CURRENT_LANG_REPLACE = """    /** 0 = English, 1 = Avro (phonetic), 2 = Probhat, 3 = Jatiyo, 4 = Developer. */
    private fun currentLang(): Int = when {
        developer -> 4
        !bangla -> 0
        probhat -> 2
        jatiyo -> 3
        else -> 1
    }

    private fun applyLangFlags(idx: Int) {
        clearDevModifiers()
        when (idx) {
            0 -> {
                developer = false
                bangla = false
            }
            1 -> {
                developer = false
                bangla = true
                probhat = false
                jatiyo = false
            }
            2 -> {
                developer = false
                bangla = true
                probhat = true
                jatiyo = false
            }
            3 -> {
                developer = false
                bangla = true
                probhat = false
                jatiyo = true
            }
            else -> {
                developer = true
                bangla = false
                probhat = false
                jatiyo = false
                devPage = 0
            }
        }
    }

    /** Switches layout (space-bar swipe); the space bar label and popup show which one is active. */
    private fun setLang(idx: Int) {
        currentInputConnection?.let { finishComposing(it) }
        applyLangFlags(idx)
        if (developer) {
            if (!forceLatin) Prefs.setDeveloperLayout(this)
        } else {
            if (bangla) Prefs.setLayout(this, if (probhat) "probhat" else if (jatiyo) "jatiyo" else "avro")
            if (!forceLatin) Prefs.setBangla(this, bangla)
        }
        shift = 0
        rebuild()
        updateAutoCap()
        currentInputConnection?.let { refreshBar(it) }
    }

    override fun onBoardSwipe(dir: Int) {
        if (mode == Mode.ALPHA && developer) {
            devPage = if (devPage == 0) 1 else 0
            rebuild()
        }
    }

    private fun devModifierMask(): Int {
        var m = 0
        if (devCtrl) m = m or DevCodes.MOD_CTRL
        if (devAlt) m = m or DevCodes.MOD_ALT
        if (devMeta) m = m or DevCodes.MOD_META
        return m
    }

    private fun clearDevModifiers() {
        devCtrl = false
        devAlt = false
        devMeta = false
        if (::kbView.isInitialized) kbView.activeModifiers = 0
    }

    private fun handleDeveloperKey(ic: InputConnection, key: Key): Boolean {
        val code = key.code
        when (code) {
            DevCodes.CTRL -> {
                devCtrl = !devCtrl
                kbView.activeModifiers = devModifierMask()
                return true
            }
            DevCodes.ALT -> {
                devAlt = !devAlt
                kbView.activeModifiers = devModifierMask()
                return true
            }
            DevCodes.META -> {
                devMeta = !devMeta
                kbView.activeModifiers = devModifierMask()
                return true
            }
            DevCodes.PAGE_TOGGLE -> {
                devPage = if (devPage == 0) 1 else 0
                rebuild()
                return true
            }
            Codes.CUT -> {
                ic.performContextMenuAction(android.R.id.cut)
                return true
            }
            Codes.COPY -> {
                ic.performContextMenuAction(android.R.id.copy)
                return true
            }
            Codes.PASTE -> {
                ic.performContextMenuAction(android.R.id.paste)
                return true
            }
        }

        // Functional PC keys (Esc, Tab, F1-F12, Arrows, Home, End, PgUp, PgDn, Ins, Del)
        if (DevCodes.isDevCode(code)) {
            val androidKey = DeveloperKeyboard.toAndroidKeyCode(code)
            if (androidKey != KeyEvent.KEYCODE_UNKNOWN) {
                finishComposing(ic)
                val usedShift = shift > 0
                if ((devCtrl || devAlt) && !devMeta && Prefs.isDeveloperMode(this)) {
                    val synthetic = KeyEvent(
                        0L, 0L, KeyEvent.ACTION_DOWN, androidKey, 0,
                        DeveloperKeyboard.buildMetaState(devCtrl, devAlt, usedShift, false),
                    )
                    val macro = ShortcutManager.findMatch(this, synthetic)
                    if (macro != null) {
                        val res = MacroExecutor.execute(ic, currentInputEditorInfo, macro) {
                            if (raw.isNotEmpty()) finishComposing(ic)
                        }
                        if (res == MacroExecutor.Result.BLOCKED_PASSWORD) {
                            Toast.makeText(this, "Macros are disabled in password fields", Toast.LENGTH_SHORT).show()
                        }
                        clearDevModifiers()
                        if (shift == 1) { shift = 0; rebuild() }
                        return true
                    }
                }
                DeveloperKeyboard.sendKey(ic, androidKey, devCtrl, devAlt, usedShift, devMeta)
                if (devCtrl || devAlt || devMeta) clearDevModifiers()
                if (shift == 1) { shift = 0; rebuild() }
            }
            return true
        }

        // If Ctrl / Alt / Meta is armed on the on-screen Developer keyboard and a letter/number/symbol is tapped
        if ((devCtrl || devAlt || devMeta) && code > 0 && key.out.length == 1) {
            val androidKey = DeveloperKeyboard.charToKeyCode(key.out[0])
            if (androidKey != KeyEvent.KEYCODE_UNKNOWN) {
                finishComposing(ic)
                val usedShift = shift > 0
                if ((devCtrl || devAlt) && !devMeta && Prefs.isDeveloperMode(this)) {
                    val synthetic = KeyEvent(
                        0L, 0L, KeyEvent.ACTION_DOWN, androidKey, 0,
                        DeveloperKeyboard.buildMetaState(devCtrl, devAlt, usedShift, false),
                    )
                    val macro = ShortcutManager.findMatch(this, synthetic)
                    if (macro != null) {
                        val res = MacroExecutor.execute(ic, currentInputEditorInfo, macro) {
                            if (raw.isNotEmpty()) finishComposing(ic)
                        }
                        if (res == MacroExecutor.Result.BLOCKED_PASSWORD) {
                            Toast.makeText(this, "Macros are disabled in password fields", Toast.LENGTH_SHORT).show()
                        }
                        clearDevModifiers()
                        if (shift == 1) { shift = 0; rebuild() }
                        return true
                    }
                }
                DeveloperKeyboard.sendKey(ic, androidKey, devCtrl, devAlt, usedShift, devMeta)
                clearDevModifiers()
                if (shift == 1) { shift = 0; rebuild() }
                return true
            }
        }

        return false
    }"""


def patch_openridmik_service(target: Path) -> None:
    svc = target / "app/src/main/java/com/devfahim00/openridmik/OpenRidmikService.kt"
    src = svc.read_text(encoding="utf-8").replace("\r\n", "\n")
    src = replace_once(src, SVC_FIELD_ANCHOR, SVC_FIELD_REPLACE, "OpenRidmikService fields", "consumedShortcutKeyCode")
    src = replace_once(src, SVC_START_VIEW_ANCHOR, SVC_START_VIEW_REPLACE, "OpenRidmikService onStartInput/onKeyDown", "ShortcutManager.findMatch")
    src = replace_once(src, SVC_LANG_INIT_ANCHOR, SVC_LANG_INIT_REPLACE, "OpenRidmikService lang init", "Prefs.isDeveloperLayout")
    src = replace_once(src, SVC_REBUILD_ROWS_ANCHOR, SVC_REBUILD_ROWS_REPLACE, "OpenRidmikService rebuild rows", "DeveloperKeyboard.buildPage")
    src = replace_once(src, SVC_SPACE_LABEL_ANCHOR, SVC_SPACE_LABEL_REPLACE, "OpenRidmikService spaceLabel", "kbView.boardSwipeEnabled")
    src = replace_once(src, SVC_HANDLE_KEY_ANCHOR, SVC_HANDLE_KEY_REPLACE, "OpenRidmikService handleKey", "handleDeveloperKey(ic, key)")
    if "private fun handleDeveloperKey" not in src:
        m = re.search(SVC_CURRENT_LANG_REGEX, src, flags=re.DOTALL)
        if not m:
            raise RuntimeError("Anchor not found for OpenRidmikService currentLang/setLang")
        src = src[:m.start()] + SVC_CURRENT_LANG_REPLACE + src[m.end():]
    svc.write_text(src, encoding="utf-8")
    print("[OK] Patched OpenRidmikService.kt")


SETTINGS_CLASS_ANCHOR = "class SettingsActivity : Activity() {"
SETTINGS_CLASS_REPLACE = """class SettingsActivity : Activity() {
    private var devModeStatus: TextView? = null"""

SETTINGS_TITLE_OLD = 'addView(text("OpenRidmik", 30f, fg, bold = true))'
SETTINGS_TITLE_NEW = 'addView(text("My Avro Keyboard", 28f, fg, bold = true))'

SETTINGS_LANG_ANCHOR = '        langRow(false, "Probhat", "lang_probhat")'
SETTINGS_LANG_REPLACE = """        langRow(false, "Probhat", "lang_probhat")
        langRow(false, "Developer (PC · Fn · Arrows)", "lang_dev")"""

SETTINGS_DIAG_ANCHOR = "        // ---- keyboard keeps switching? ----"
SETTINGS_DEV_SECTION = """        // ---- developer mode (separated from ordinary keyboard settings) ----
        section("Developer Mode")
        val devCard = card()
        devModeStatus = text("", 14f, muted)
        row(devCard, true, "Hardware Shortcuts & Macros", devModeStatus) {
            startActivity(Intent(this, DeveloperSettingsActivity::class.java))
        }
        updateDevModeSummary()

        // ---- keyboard keeps switching? ----"""

SETTINGS_HELPER_METHOD = """
    private fun updateDevModeSummary() {
        val statusView = devModeStatus ?: return
        val on = Prefs.isDeveloperMode(this)
        val count = ShortcutRepository.loadAll(this).size
        statusView.text = if (on) "ON · $count" else "OFF ›"
    }
"""


def patch_settings_activity(target: Path) -> None:
    act = target / "app/src/main/java/com/devfahim00/openridmik/SettingsActivity.kt"
    src = act.read_text(encoding="utf-8").replace("\r\n", "\n")
    src = replace_once(src, SETTINGS_CLASS_ANCHOR, SETTINGS_CLASS_REPLACE, "SettingsActivity class", "private var devModeStatus")
    if SETTINGS_TITLE_OLD in src:
        src = src.replace(SETTINGS_TITLE_OLD, SETTINGS_TITLE_NEW, 1)
    src = replace_once(src, SETTINGS_LANG_ANCHOR, SETTINGS_LANG_REPLACE, "SettingsActivity lang_dev", '"lang_dev"')
    src = replace_once(src, SETTINGS_DIAG_ANCHOR, SETTINGS_DEV_SECTION, "SettingsActivity dev section", "DeveloperSettingsActivity")
    if "private fun updateDevModeSummary()" not in src:
        last_brace = src.rfind("}")
        if last_brace == -1:
            raise RuntimeError("Malformed SettingsActivity.kt")
        src = src[:last_brace] + SETTINGS_HELPER_METHOD + "\n}\n"
    if "override fun onResume()" in src and "super.onResume()\n        updateDevModeSummary()" not in src:
        src = src.replace("super.onResume()", "super.onResume()\n        updateDevModeSummary()", 1)
    act.write_text(src, encoding="utf-8")
    print("[OK] Patched SettingsActivity.kt")


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: python3 scripts/apply_to_openridmik.py <path-to-openridmik-repo>")
        return 1

    target = Path(sys.argv[1]).resolve()
    if not (target / "app/src/main/AndroidManifest.xml").exists():
        print(f"Error: {target} does not look like an OpenRidmik checkout.")
        return 1

    for rel in OVERLAY_FILES:
        src_file = ROOT / rel
        dst_file = target / rel
        dst_file.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src_file, dst_file)
        print(f"[OK] Copied {rel}")

    patch_keyboard_view(target)
    patch_openridmik_service(target)
    patch_settings_activity(target)

    print("\nAll My Avro Keyboard modules and patches applied cleanly!")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
