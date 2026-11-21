#!/usr/bin/env python3
"""
Builds the One-Time Limited Edition "Nusai Keyboard" onto a fresh clone of
https://github.com/devfahim00/OpenRidmik:
- Alphabetical ABCD layout (a-j, k-s, t-z) instead of QWERTY
- Avro (Bangla) + English ONLY (no Probhat, Jatiyo, or Developer Mode)
- 100% offline one-time edition (Updater disabled, no INTERNET permission, no Telegram button)
- Keeps Twilight Blossom & all themes, Ridmik symbol hints, fast long-press, and Spacebar cursor move

Usage:
    python3 scripts/apply_nusai_edition.py <path-to-openridmik-repo>
"""

import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def replace_once(src: str, anchor: str, replacement: str, label: str) -> str:
    if anchor not in src:
        raise RuntimeError(f"Anchor not found for {label}")
    return src.replace(anchor, replacement, 1)


def write_gradle_and_manifest(target: Path) -> None:
    (target / "settings.gradle.kts").write_text(
        """pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NusaiKeyboard"
include(":app")
""",
        encoding="utf-8",
    )

    (target / "app/build.gradle.kts").write_text(
        """plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.devfahim00.openridmik"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nusai.keyboard"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0 Limited"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
""",
        encoding="utf-8",
    )

    # 100% offline manifest (no INTERNET permission, no update checks)
    (target / "app/src/main/AndroidManifest.xml").write_text(
        """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme">

        <activity
            android:name=".SettingsActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".OpenRidmikService"
            android:exported="true"
            android:label="@string/app_name"
            android:permission="android.permission.BIND_INPUT_METHOD">
            <intent-filter>
                <action android:name="android.view.InputMethod" />
            </intent-filter>
            <meta-data
                android:name="android.view.im"
                android:resource="@xml/method" />
        </service>
    </application>
</manifest>
""",
        encoding="utf-8",
    )

    (target / "app/src/main/res/values/strings.xml").write_text(
        """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Nusai Keyboard</string>
    <string name="subtype_bn">বাংলা</string>
    <string name="subtype_en">English</string>
</resources>
""",
        encoding="utf-8",
    )
    print("[OK] Wrote Nusai Gradle, offline Manifest & strings.xml")


def write_offline_updater(target: Path) -> None:
    # Neutralize Updater so zero network calls or update prompts ever happen
    (target / "app/src/main/java/com/devfahim00/openridmik/Updater.kt").write_text(
        """package com.devfahim00.openridmik

import android.content.Context

/** One-Time Limited Edition: update checks are permanently disabled. */
object Updater {
    class Info(val version: String, val apkUrl: String, val pageUrl: String)

    sealed class Result {
        class Available(val info: Info) : Result()
        object UpToDate : Result()
        object Failed : Result()
    }

    fun currentVersion(c: Context): String = "1.0.0 Limited"

    fun available(c: Context): Info? = null

    fun check(c: Context, force: Boolean, onDone: ((Result) -> Unit)? = null) {
        onDone?.invoke(Result.UpToDate)
    }
}
""",
        encoding="utf-8",
    )
    print("[OK] Disabled Updater.kt for One-Time Limited Edition")


def patch_prefs(target: Path) -> None:
    # Copy our Prefs.kt then restrict enabledLangs strictly to 0 (English) and 1 (Avro)
    shutil.copy2(
        ROOT / "app/src/main/java/com/devfahim00/openridmik/Prefs.kt",
        target / "app/src/main/java/com/devfahim00/openridmik/Prefs.kt",
    )
    shutil.copy2(
        ROOT / "app/src/main/java/com/devfahim00/openridmik/Theme.kt",
        target / "app/src/main/java/com/devfahim00/openridmik/Theme.kt",
    )
    p = target / "app/src/main/java/com/devfahim00/openridmik/Prefs.kt"
    src = p.read_text(encoding="utf-8")
    src = src.replace(
        'listOf("lang_en", "lang_avro", "lang_probhat", "lang_jatiyo")',
        'listOf("lang_en", "lang_avro")',
    )
    p.write_text(src, encoding="utf-8")
    print("[OK] Copied Theme.kt and configured Prefs.kt (Avro + English only)")


def patch_layouts_abcd(target: Path) -> None:
    lay = target / "app/src/main/java/com/devfahim00/openridmik/Layouts.kt"
    src = lay.read_text(encoding="utf-8").replace("\r\n", "\n")

    # Replace QWERTY rows with Alphabetical ABCD rows (10 + 9 + 7 = 26 letters)
    src = replace_once(src, 'letters("qwertyuiop", shift, bangla)', 'letters("abcdefghij", shift, bangla)', "ABCD row 1")
    src = replace_once(src, 'letters("asdfghjkl", shift, bangla)', 'letters("klmnopqrs", shift, bangla)', "ABCD row 2")
    src = replace_once(src, 'letters("zxcvbnm", shift, bangla)', 'letters("tuvwxyz", shift, bangla)', "ABCD row 3")

    # Clean symbol hints on letter keys
    symbol_hint_block = (
        "    private val symbolHint = mapOf(\n"
        "        'a' to \"\\u09F3\", 'b' to \"%\", 'c' to \"_\", 'd' to \";\", 'e' to \"<\",\n"
        "        'f' to \">\", 'g' to \"+\", 'h' to \"=\", 'i' to \"[\", 'j' to \"]\",\n"
        "        'k' to \"@\", 'l' to \"#\", 'm' to \"&\", 'n' to \"*\", 'o' to \"-\",\n"
        "        'p' to \"!\", 'q' to \"?\", 'r' to \"(\", 's' to \")\",\n"
        "        't' to \"\\\"\", 'u' to \"'\", 'w' to \":\", 'y' to \"/\", 'z' to \"~\",\n"
        "    )"
    )
    src = re.sub(
        r"    private val lowerHint = mapOf\(.*?\n    \)",
        lambda _m: symbol_hint_block,
        src,
        count=1,
        flags=re.DOTALL,
    )
    src = src.replace(
        "val hint = if (bangla) (if (shift) upperHint[c] else lowerHint[c]) else null",
        "val hint = if (shift) (if (bangla) upperHint[c] else null) ?: symbolHint[raw] else symbolHint[raw]",
    )
    lay.write_text(src, encoding="utf-8")
    print("[OK] Patched Layouts.kt to Alphabetical ABCD (a-j / k-s / t-z) + symbol hints")


def patch_keyboard_view(target: Path) -> None:
    kv = target / "app/src/main/java/com/devfahim00/openridmik/KeyboardView.kt"
    src = kv.read_text(encoding="utf-8").replace("\r\n", "\n")

    src = re.sub(
        r"fun onSpaceSwipe\(dir: Int\)\s*\{\s*\}",
        "fun onSpaceSwipe(dir: Int) {}\n        fun onCursorMove(dir: Int) {}",
        src,
        count=1,
    )
    src = re.sub(
        r"var shiftState = 0\n\s*set\(v\) \{\n\s*field = v\n\s*invalidate\(\)\n\s*\}",
        """var shiftState = 0
        set(v) {
            field = v
            invalidate()
        }
    var showPopup = true
    var hideHints = false
        set(v) { if (field == v) return; field = v; invalidate() }
    var longPressMs = 250L
    var spaceCursorEnabled = true
    var spaceCursorDelayMs = 800L
    var spaceCursorSpeed = 250
    private var spaceCursorId = -1
    private var spaceCursorActive = false
    private var spaceCursorLastX = 0f
    private val spaceCursorArmRun = Runnable {
        if (spaceCursorId != -1) {
            spaceCursorActive = true
            if (haptic) performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            invalidate()
        }
    }""",
        src,
        count=1,
    )
    src = src.replace("val hinted = k.hint != null", "val hinted = k.hint != null && !hideHints", 1)
    src = src.replace("k.hint?.let {", "if (!hideHints) k.hint?.let {", 1)
    src = src.replace("private fun drawPreview(canvas: Canvas, p: Press) {", "private fun drawPreview(canvas: Canvas, p: Press) {\n        if (!showPopup) return", 1)
    src = src.replace("handler.postDelayed(p.longRun, 420)", "handler.postDelayed(p.longRun, longPressMs)")
    src = src.replace(
        "if (p.swiping) drawSwipePreview(canvas, p) else drawPreview(canvas, p)",
        """if (p.swiping) {
                if (p.key.code == Codes.SPACE) drawSwipePreview(canvas, p)
            } else if (spaceCursorActive && p.key.code == Codes.SPACE) {
                val saved = spaceLabel
                spaceLabel = "◀ Cursor ▶"
                drawSwipePreview(canvas, p)
                spaceLabel = saved
            } else {
                drawPreview(canvas, p)
            }""",
        1,
    )
    src = src.replace(
        "val p = Press(h.first, h.second, x)\n        presses[id] = p",
        """val p = Press(h.first, h.second, x)
        presses[id] = p
        if (spaceCursorEnabled && h.first.code == Codes.SPACE) {
            handler.removeCallbacks(spaceCursorArmRun)
            spaceCursorId = id
            spaceCursorActive = false
            spaceCursorLastX = x
            handler.postDelayed(spaceCursorArmRun, spaceCursorDelayMs)
        }""",
        1,
    )
    src = src.replace(
        "if (p.key.code == Codes.SPACE && !p.longDone) {",
        """if (p.key.code == Codes.SPACE && spaceCursorActive && id == spaceCursorId) {
            val stepPx = dp(24f) * (250f / spaceCursorSpeed.coerceIn(100, 500))
            val delta = x - spaceCursorLastX
            if (Math.abs(delta) >= stepPx) {
                val steps = (delta / stepPx).toInt()
                val dir = if (steps > 0) 1 else -1
                val count = Math.abs(steps).coerceAtMost(6)
                for (n in 0 until count) {
                    listener?.onCursorMove(dir)
                }
                spaceCursorLastX += steps * stepPx
                if (haptic) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            return
        }
        if (p.key.code == Codes.SPACE && !p.longDone) {
            if (Math.abs(x - p.startX) > dp(16f) && id == spaceCursorId && !spaceCursorActive) {
                handler.removeCallbacks(spaceCursorArmRun)
                spaceCursorId = -1
            }""",
        1,
    )
    src = src.replace(
        "private fun up(id: Int) {\n        val p = presses.remove(id) ?: return",
        """private fun up(id: Int) {
        val wasSpaceCursor = (id == spaceCursorId && spaceCursorActive)
        if (id == spaceCursorId) {
            handler.removeCallbacks(spaceCursorArmRun)
            spaceCursorId = -1
            spaceCursorActive = false
        }
        val p = presses.remove(id) ?: return""",
        1,
    )
    src = src.replace(
        "if (!p.longDone && !p.fired && !p.swiping) listener?.onKey(p.key)",
        "if (!p.longDone && !p.fired && !p.swiping && !wasSpaceCursor) listener?.onKey(p.key)",
        1,
    )
    src = src.replace(
        "presses.clear()",
        "presses.clear()\n        handler.removeCallbacks(spaceCursorArmRun)\n        spaceCursorId = -1\n        spaceCursorActive = false",
        1,
    )
    kv.write_text(src, encoding="utf-8")
    print("[OK] Patched KeyboardView.kt (fast long-press, popup/hints, Spacebar cursor)")


def patch_service(target: Path) -> None:
    svc = target / "app/src/main/java/com/devfahim00/openridmik/OpenRidmikService.kt"
    src = svc.read_text(encoding="utf-8").replace("\r\n", "\n")

    src = src.replace(
        "    private var gearBtn: IconView? = null",
        "    private var gearBtn: IconView? = null\n    private var lastSpaceTap = 0L",
        1,
    )
    # Lock Bangla layout to Avro only
    src = src.replace('probhat = Prefs.layout(this) == "probhat"', "probhat = false")
    src = src.replace('jatiyo = Prefs.layout(this) == "jatiyo"', "jatiyo = false")

    src = src.replace(
        "        kbView.haptic = Prefs.get(this, \"haptic\")",
        """        kbView.haptic = Prefs.get(this, "haptic")
        kbView.showPopup = Prefs.get(this, "keyPopup")
        kbView.hideHints = Prefs.get(this, "hideHints")
        kbView.longPressMs = Prefs.longPressDelayMs(this)
        kbView.spaceCursorEnabled = Prefs.get(this, "spaceCursor")
        kbView.spaceCursorDelayMs = Prefs.spaceCursorDelayMs(this)
        kbView.spaceCursorSpeed = Prefs.spaceCursorSpeed(this)""",
        1,
    )
    src = src.replace(
        """            Codes.SPACE -> {
                finishComposing(ic)
                ic.commitText(" ", 1)""",
        """            Codes.SPACE -> {
                finishComposing(ic)
                val now = System.currentTimeMillis()
                val dbl = (now - lastSpaceTap) in 1L..350L
                if (dbl && Prefs.get(this, "doubleSpacePeriod")) {
                    ic.deleteSurroundingText(1, 0)
                    ic.commitText(if (bangla) "\\u0964 " else ". ", 1)
                    lastSpaceTap = 0L
                } else {
                    ic.commitText(" ", 1)
                    lastSpaceTap = now
                }""",
        1,
    )
    src = src.replace(
        "    private fun updateAutoCap() {",
        """    override fun onCursorMove(dir: Int) {
        val ic = currentInputConnection ?: return
        if (raw.isNotEmpty()) finishComposing(ic)
        val now = android.os.SystemClock.uptimeMillis()
        val key = if (dir < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, key, 0))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, key, 0))
    }

    private fun updateAutoCap() {
        if (!Prefs.get(this, "autoCap")) {
            if (shift == 1) {
                shift = 0
                rebuild()
            }
            return
        }""",
        1,
    )
    svc.write_text(src, encoding="utf-8")
    print("[OK] Patched OpenRidmikService.kt for Nusai Keyboard (Avro + English + Spacebar cursor)")


def patch_settings(target: Path) -> None:
    act = target / "app/src/main/java/com/devfahim00/openridmik/SettingsActivity.kt"
    src = act.read_text(encoding="utf-8").replace("\r\n", "\n")

    src = src.replace('addView(text("OpenRidmik", 30f, fg, bold = true))', 'addView(text("Nusai Keyboard", 28f, fg, bold = true))')
    src = src.replace('"Enable OpenRidmik"', '"Enable Nusai Keyboard"')
    src = src.replace('Set OpenRidmik to', 'Set Nusai Keyboard to')

    # Hide Bangla layout selector (Avro only) & Probhat/Jatiyo toggles
    src = re.sub(
        r'\n\s*// ---- layout \(segmented\) ----.*?(?=\n\s*// ---- theme ----)',
        "\n",
        src,
        flags=re.DOTALL,
    )
    src = re.sub(r'\n\s*langRow\(false, "Probhat", "lang_probhat"\)', "", src)
    src = re.sub(r'\n\s*langRow\(false, "Jatiyo.*?"lang_jatiyo"\)', "", src)

    # Add helpful typing & cursor toggles under Preferences
    src = src.replace(
        'toggleRow(prefs, false, "Clipboard history", "clipboard")',
        """toggleRow(prefs, false, "Clipboard history", "clipboard")
        toggleRow(prefs, false, "Auto-capitalization", "autoCap")
        toggleRow(prefs, false, "Double-space for period", "doubleSpacePeriod")
        toggleRow(prefs, false, "Keypress popup preview", "keyPopup")
        toggleRow(prefs, false, "Hide long-press hints", "hideHints")
        toggleRow(prefs, false, "Spacebar hold cursor move", "spaceCursor")""",
        1,
    )

    # Remove About / Check for updates AND Telegram button completely
    src = re.sub(
        r'\n\s*// ---- about ----.*?(?=\n\s*setContentView\()',
        "\n",
        src,
        flags=re.DOTALL,
    )
    act.write_text(src, encoding="utf-8")
    print("[OK] Patched SettingsActivity.kt for Nusai Keyboard (clean, offline, no updates/TG)")


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: python3 scripts/apply_nusai_edition.py <path-to-openridmik-repo>")
        return 1
    target = Path(sys.argv[1]).resolve()
    write_gradle_and_manifest(target)
    write_offline_updater(target)
    patch_prefs(target)
    patch_layouts_abcd(target)
    patch_keyboard_view(target)
    patch_service(target)
    patch_settings(target)
    print("\nNusai Keyboard (One-Time ABCD Limited Edition) ready to build!")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
