#!/usr/bin/env python3
"""
Applies My Avro Keyboard (Developer Mode, Hardware Shortcuts & Text Macros)
cleanly onto a clone of https://github.com/devfahim00/OpenRidmik.

Usage:
    python3 scripts/apply_to_openridmik.py <path-to-openridmik-repo>
"""

import os
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
    "app/src/main/java/com/devfahim00/openridmik/Shortcut.kt",
    "app/src/main/java/com/devfahim00/openridmik/ShortcutRepository.kt",
    "app/src/main/java/com/devfahim00/openridmik/ShortcutManager.kt",
    "app/src/main/java/com/devfahim00/openridmik/MacroExecutor.kt",
    "app/src/main/java/com/devfahim00/openridmik/DeveloperSettingsActivity.kt",
    "app/src/main/java/com/devfahim00/openridmik/ShortcutCaptureActivity.kt",
    "app/src/test/java/com/devfahim00/openridmik/ShortcutLogicTest.kt",
]

SERVICE_FIELD_ANCHOR = "    private var gearBtn: IconView? = null"
SERVICE_FIELD_INSERT = """    private var gearBtn: IconView? = null
    private var consumedShortcutKeyCode: Int = KeyEvent.KEYCODE_UNKNOWN"""

SERVICE_LIFECYCLE_ANCHOR = "    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {"
SERVICE_LIFECYCLE_INSERT = """    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
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

SETTINGS_CLASS_ANCHOR = "class SettingsActivity : Activity() {"
SETTINGS_CLASS_INSERT = """class SettingsActivity : Activity() {
    private var devModeStatus: TextView? = null"""

SETTINGS_TITLE_OLD = 'addView(text("OpenRidmik", 30f, fg, bold = true))'
SETTINGS_TITLE_NEW = 'addView(text("My Avro Keyboard", 28f, fg, bold = true))'

SETTINGS_DIAG_ANCHOR = '        // ---- keyboard keeps switching? ----'
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


def patch_openridmik_service(target: Path) -> None:
    svc = target / "app/src/main/java/com/devfahim00/openridmik/OpenRidmikService.kt"
    if not svc.exists():
        raise FileNotFoundError(f"Missing {svc}")
    src = svc.read_text(encoding="utf-8")

    if "consumedShortcutKeyCode" not in src:
        if SERVICE_FIELD_ANCHOR not in src:
            raise RuntimeError("Could not find gearBtn field anchor in OpenRidmikService.kt")
        src = src.replace(SERVICE_FIELD_ANCHOR, SERVICE_FIELD_INSERT, 1)

    if "ShortcutManager.findMatch" not in src:
        if SERVICE_LIFECYCLE_ANCHOR not in src:
            raise RuntimeError("Could not find onStartInputView anchor in OpenRidmikService.kt")
        src = src.replace(SERVICE_LIFECYCLE_ANCHOR, SERVICE_LIFECYCLE_INSERT, 1)

    svc.write_text(src, encoding="utf-8")
    print("[OK] Patched OpenRidmikService.kt")


def patch_settings_activity(target: Path) -> None:
    act = target / "app/src/main/java/com/devfahim00/openridmik/SettingsActivity.kt"
    if not act.exists():
        raise FileNotFoundError(f"Missing {act}")
    src = act.read_text(encoding="utf-8")

    if "private var devModeStatus" not in src:
        if SETTINGS_CLASS_ANCHOR not in src:
            raise RuntimeError("Could not find SettingsActivity class header")
        src = src.replace(SETTINGS_CLASS_ANCHOR, SETTINGS_CLASS_INSERT, 1)

    if SETTINGS_TITLE_OLD in src:
        src = src.replace(SETTINGS_TITLE_OLD, SETTINGS_TITLE_NEW, 1)

    if "DeveloperSettingsActivity" not in src:
        if SETTINGS_DIAG_ANCHOR not in src:
            raise RuntimeError("Could not find diagnostics section anchor in SettingsActivity.kt")
        src = src.replace(SETTINGS_DIAG_ANCHOR, SETTINGS_DEV_SECTION, 1)

    if "private fun updateDevModeSummary()" not in src:
        # Insert helper before final closing brace of class
        last_brace = src.rfind("}")
        if last_brace == -1:
            raise RuntimeError("Malformed SettingsActivity.kt")
        src = src[:last_brace] + SETTINGS_HELPER_METHOD + "\n}\n"

    if "updateDevModeSummary()" in src and "override fun onResume()" in src:
        # Ensure onResume refreshes the Developer Mode summary badge
        if "super.onResume()\n        updateDevModeSummary()" not in src:
            src = src.replace(
                "super.onResume()",
                "super.onResume()\n        updateDevModeSummary()",
                1,
            )

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

    patch_openridmik_service(target)
    patch_settings_activity(target)

    print("\nAll My Avro Keyboard modules and patches applied cleanly!")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
