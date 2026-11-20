package com.devfahim00.openridmik

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Activity for creating or editing a hardware keyboard shortcut and its text macro.
 *
 * Supports two ways to choose the key combination:
 * 1. Live physical keyboard capture via [dispatchKeyEvent].
 * 2. Manual modifier chips (Ctrl / Alt / Shift) + key picker fallback for configuring
 *    shortcuts even when a physical keyboard is not currently connected.
 */
class ShortcutCaptureActivity : Activity() {

    companion object {
        const val EXTRA_SHORTCUT_ID = "shortcut_id"
    }

    private var bg = 0
    private var surface = 0
    private var line = 0
    private var fg = 0
    private var muted = 0
    private var accent = 0
    private var onAccent = 0

    private var editingId: String? = null
    private var existingEnabled: Boolean = true

    private var capturing = false
    private var ctrl = false
    private var alt = false
    private var shift = false
    private var keyCode: Int = KeyEvent.KEYCODE_UNKNOWN

    private lateinit var comboPreview: TextView
    private lateinit var captureBtn: TextView
    private lateinit var ctrlChip: TextView
    private lateinit var altChip: TextView
    private lateinit var shiftChip: TextView
    private lateinit var keyPickerBtn: TextView
    private lateinit var nameInput: EditText
    private lateinit var macroInput: EditText
    private lateinit var charCounter: TextView
    private lateinit var errorBanner: TextView

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, r: Int, strokeColor: Int = 0, strokeWidth: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(r).toFloat()
            if (strokeWidth > 0) setStroke(dp(strokeWidth), strokeColor)
        }

    private fun lp(top: Int = 0): LinearLayout.LayoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = dp(top) }

    private fun text(t: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = t
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bg = getColor(R.color.kb_bg)
        surface = getColor(R.color.key_bg)
        line = getColor(R.color.key_special_bg)
        fg = getColor(R.color.key_text)
        muted = getColor(R.color.key_hint)
        accent = getColor(R.color.accent)
        onAccent = getColor(R.color.accent_text)

        @Suppress("DEPRECATION")
        window.statusBarColor = bg
        @Suppress("DEPRECATION")
        window.navigationBarColor = bg
        if (!Themes.isNight(this)) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        editingId = intent.getStringExtra(EXTRA_SHORTCUT_ID)
        val existing = editingId?.let { ShortcutRepository.findById(this, it) }
        if (existing != null) {
            ctrl = existing.ctrl
            alt = existing.alt
            shift = existing.shift
            keyCode = existing.keyCode
            existingEnabled = existing.enabled
        } else {
            editingId = null
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(36))
        }

        // ---- Top bar ----
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), 0, dp(4), dp(8))
        }
        val backBtn = text("←", 24f, fg, bold = true).apply {
            setPadding(dp(6), dp(6), dp(14), dp(6))
            setOnClickListener { finish() }
        }
        val heading = text(if (editingId == null) "Add Shortcut" else "Edit Shortcut", 24f, fg, bold = true)
        topBar.addView(backBtn)
        topBar.addView(heading, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(topBar, lp())

        // ---- Error banner (hidden until validation message) ----
        errorBanner = text("", 13.5f, Color.parseColor("#B71C1C"), bold = false).apply {
            background = rounded(Color.parseColor("#FFEBEE"), 12, Color.parseColor("#EF9A9A"), 1)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            visibility = View.GONE
        }
        root.addView(errorBanner, lp(8))

        // ---- Step 1: Key Combination Card ----
        val comboCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(surface, 16)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        comboCard.addView(text("1. Hardware Key Combination", 15f, fg, bold = true))

        comboPreview = text("", 18f, accent, bold = true).apply {
            gravity = Gravity.CENTER
            background = rounded(bg, 12, line, 1)
            setPadding(dp(14), dp(16), dp(14), dp(16))
        }
        comboCard.addView(comboPreview, lp(10))

        captureBtn = text("Press Physical Keyboard Shortcut", 14.5f, onAccent, bold = true).apply {
            gravity = Gravity.CENTER
            background = rounded(accent, 12)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setOnClickListener { toggleCaptureMode() }
        }
        comboCard.addView(captureBtn, lp(10))

        comboCard.addView(text("Or select manually:", 12.5f, muted), lp(12))

        val chipRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        ctrlChip = makeModifierChip("Ctrl") {
            ctrl = !ctrl
            stopCaptureMode()
            refreshComboUi()
        }
        altChip = makeModifierChip("Alt") {
            alt = !alt
            stopCaptureMode()
            refreshComboUi()
        }
        shiftChip = makeModifierChip("Shift") {
            shift = !shift
            stopCaptureMode()
            refreshComboUi()
        }
        keyPickerBtn = text("Pick Key ▾", 13.5f, fg, bold = true).apply {
            gravity = Gravity.CENTER
            background = rounded(bg, 10, line, 1)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener {
                stopCaptureMode()
                showKeyPickerDialog()
            }
        }
        val chipLp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            rightMargin = dp(6)
        }
        chipRow.addView(ctrlChip, chipLp)
        chipRow.addView(altChip, chipLp)
        chipRow.addView(shiftChip, chipLp)
        chipRow.addView(
            keyPickerBtn,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f),
        )
        comboCard.addView(chipRow, lp(8))
        root.addView(comboCard, lp(12))

        // ---- Step 2: Shortcut Name ----
        val nameCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(surface, 16)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        nameCard.addView(text("2. Shortcut Name", 15f, fg, bold = true))
        nameInput = EditText(this).apply {
            hint = "e.g., BMR Review Note or Email Signature"
            setTextColor(fg)
            setHintTextColor(muted)
            textSize = 15.5f
            background = rounded(bg, 10, line, 1)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setSingleLine(true)
            existing?.let { setText(it.name) }
        }
        nameCard.addView(nameInput, lp(8))
        root.addView(nameCard, lp(12))

        // ---- Step 3: Text Macro ----
        val macroCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(surface, 16)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val macroHead = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        macroHead.addView(
            text("3. Text Macro (Bangla / English / Multiline)", 15f, fg, bold = true),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        charCounter = text("0 / ${ShortcutManager.MAX_MACRO_LENGTH}", 12f, muted)
        macroHead.addView(charCounter)
        macroCard.addView(macroHead, lp())

        macroInput = EditText(this).apply {
            hint = "Enter the text to insert when this shortcut is pressed...\nSupports multiple lines, Bangla, English, and punctuation."
            setTextColor(fg)
            setHintTextColor(muted)
            textSize = 15f
            background = rounded(bg, 10, line, 1)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            gravity = Gravity.TOP or Gravity.START
            minLines = 4
            maxLines = 10
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            existing?.let { setText(it.macroText) }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    updateCharCounter(s?.length ?: 0)
                }
            })
        }
        macroCard.addView(macroInput, lp(8))
        root.addView(macroCard, lp(12))

        // ---- Save / Cancel Buttons ----
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val cancelBtn = text("Cancel", 15.5f, fg, bold = true).apply {
            gravity = Gravity.CENTER
            background = rounded(surface, 14, line, 1)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setOnClickListener { finish() }
        }
        val saveBtn = text("Save Shortcut", 15.5f, onAccent, bold = true).apply {
            gravity = Gravity.CENTER
            background = rounded(accent, 14)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setOnClickListener { attemptSave() }
        }
        btnRow.addView(
            cancelBtn,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(8) },
        )
        btnRow.addView(
            saveBtn,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f),
        )
        root.addView(btnRow, lp(18))

        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            addView(root)
        }
        setContentView(scroll)

        updateCharCounter(macroInput.text?.length ?: 0)
        refreshComboUi()
    }

    private fun makeModifierChip(label: String, onClick: () -> Unit): TextView =
        text(label, 13.5f, fg, bold = true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), dp(10))
            setOnClickListener { onClick() }
        }

    private fun toggleCaptureMode() {
        capturing = !capturing
        if (capturing) {
            clearError()
            currentFocus?.clearFocus()
        }
        refreshComboUi()
    }

    private fun stopCaptureMode() {
        if (capturing) {
            capturing = false
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!capturing) return super.dispatchKeyEvent(event)

        if (event.action == KeyEvent.ACTION_DOWN) {
            val code = event.keyCode
            // Allow hardware Back key to cancel capture mode cleanly
            if (code == KeyEvent.KEYCODE_BACK) {
                stopCaptureMode()
                refreshComboUi()
                return true
            }
            // Wait until the user presses the non-modifier trigger key
            if (ShortcutManager.isModifierKey(code)) {
                return true
            }
            if (event.isMetaPressed) {
                showError("System Meta / Windows / Command combinations are reserved by Android and cannot be captured.")
                stopCaptureMode()
                refreshComboUi()
                return true
            }
            ctrl = event.isCtrlPressed
            alt = event.isAltPressed
            shift = event.isShiftPressed
            keyCode = code
            stopCaptureMode()
            refreshComboUi()

            // Immediately show feedback if combo itself is invalid or reserved
            when {
                !ctrl && !alt -> showError("Include Ctrl or Alt with '${Shortcut.keyCodeToLabel(code)}' so normal typing is not disrupted.")
                !ShortcutManager.isSupportedTriggerKey(code) ->
                    showError("Unsupported key '${Shortcut.keyCodeToLabel(code)}'. Use A–Z, 0–9, F1–F12, or punctuation.")
                ShortcutManager.reservedReason(ctrl, shift, alt, code) != null ->
                    showError("Reserved combination: ${ShortcutManager.reservedReason(ctrl, shift, alt, code)}")
                else -> clearError()
            }
            return true
        }
        if (event.action == KeyEvent.ACTION_UP) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun showKeyPickerDialog() {
        val codes = ArrayList<Int>()
        for (k in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z) codes.add(k)
        for (k in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) codes.add(k)
        for (k in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12) codes.add(k)
        codes.addAll(
            listOf(
                KeyEvent.KEYCODE_MINUS,
                KeyEvent.KEYCODE_EQUALS,
                KeyEvent.KEYCODE_LEFT_BRACKET,
                KeyEvent.KEYCODE_RIGHT_BRACKET,
                KeyEvent.KEYCODE_SEMICOLON,
                KeyEvent.KEYCODE_APOSTROPHE,
                KeyEvent.KEYCODE_COMMA,
                KeyEvent.KEYCODE_PERIOD,
                KeyEvent.KEYCODE_SLASH,
            ),
        )
        val labels = codes.map { Shortcut.keyCodeToLabel(it) }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Choose Trigger Key")
            .setItems(labels) { _, which ->
                keyCode = codes[which]
                if (!ctrl && !alt) ctrl = true
                clearError()
                refreshComboUi()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshComboUi() {
        if (capturing) {
            comboPreview.text = "Listening… Press shortcut on physical keyboard"
            comboPreview.setTextColor(accent)
            comboPreview.background = rounded(bg, 12, accent, 2)
            captureBtn.text = "Cancel Listening"
        } else {
            comboPreview.background = rounded(bg, 12, line, 1)
            captureBtn.text = "Capture Physical Keyboard Shortcut"
            if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
                comboPreview.text = "No shortcut selected"
                comboPreview.setTextColor(muted)
            } else {
                comboPreview.text = Shortcut.formatCombo(ctrl, shift, alt, keyCode)
                comboPreview.setTextColor(accent)
            }
        }

        styleChip(ctrlChip, ctrl)
        styleChip(altChip, alt)
        styleChip(shiftChip, shift)
        keyPickerBtn.text = if (keyCode == KeyEvent.KEYCODE_UNKNOWN) "Pick Key ▾" else "Key: ${Shortcut.keyCodeToLabel(keyCode)}"
    }

    private fun styleChip(v: TextView, selected: Boolean) {
        if (selected) {
            v.background = rounded(accent, 10)
            v.setTextColor(onAccent)
        } else {
            v.background = rounded(bg, 10, line, 1)
            v.setTextColor(fg)
        }
    }

    private fun updateCharCounter(len: Int) {
        charCounter.text = "$len / ${ShortcutManager.MAX_MACRO_LENGTH}"
        charCounter.setTextColor(if (len > ShortcutManager.MAX_MACRO_LENGTH) Color.parseColor("#D32F2F") else muted)
    }

    private fun showError(msg: String) {
        errorBanner.text = msg
        errorBanner.visibility = View.VISIBLE
    }

    private fun clearError() {
        errorBanner.visibility = View.GONE
    }

    private fun attemptSave() {
        stopCaptureMode()
        val name = nameInput.text?.toString() ?: ""
        val macro = macroInput.text?.toString() ?: ""

        when (
            val result = ShortcutManager.validate(
                c = this,
                name = name,
                ctrl = ctrl,
                shift = shift,
                alt = alt,
                keyCode = keyCode,
                macroText = macro,
                editingId = editingId,
            )
        ) {
            is ShortcutManager.ValidationResult.Invalid -> {
                showError(result.message)
                return
            }
            ShortcutManager.ValidationResult.Valid -> {
                clearError()
            }
        }

        val id = editingId
        if (id == null) {
            val created = Shortcut(
                name = name.trim(),
                ctrl = ctrl,
                shift = shift,
                alt = alt,
                keyCode = keyCode,
                macroText = macro,
                enabled = true,
            )
            ShortcutRepository.add(this, created)
            Toast.makeText(this, "Saved \"${created.name}\"", Toast.LENGTH_SHORT).show()
        } else {
            val updated = Shortcut(
                id = id,
                name = name.trim(),
                ctrl = ctrl,
                shift = shift,
                alt = alt,
                keyCode = keyCode,
                macroText = macro,
                enabled = existingEnabled,
            )
            ShortcutRepository.update(this, updated)
            Toast.makeText(this, "Updated \"${updated.name}\"", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
