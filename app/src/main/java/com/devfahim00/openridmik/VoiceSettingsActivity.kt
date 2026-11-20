package com.devfahim00.openridmik

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/**
 * Settings screen for Gemini AI Voice Typing (API key, microphone permission,
 * model selection, language mode, and smart punctuation).
 */
class VoiceSettingsActivity : Activity() {

    private var bg = 0
    private var surface = 0
    private var line = 0
    private var fg = 0
    private var muted = 0
    private var accent = 0
    private var onAccent = 0

    private lateinit var micStatusText: TextView
    private lateinit var micGrantBtn: TextView
    private lateinit var apiKeyInput: EditText
    private lateinit var modelInput: EditText
    private lateinit var testStatusText: TextView

    private val langButtons = ArrayList<Pair<String, TextView>>()

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
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("AI Voice Typing", 25f, fg, bold = true))
            addView(text("Gemini-powered Bangla & English speech-to-text", 13f, muted))
        }
        topBar.addView(backBtn)
        topBar.addView(titleCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(topBar, lp())

        // ---- 1. Microphone Permission Card ----
        val micCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(surface, 16)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        micCard.addView(text("1. Microphone Permission", 15f, fg, bold = true))
        micStatusText = text("", 13f, muted)
        micCard.addView(micStatusText, lp(4))
        micGrantBtn = text("Grant Microphone Permission", 14.5f, onAccent, bold = true).apply {
            gravity = Gravity.CENTER
            background = rounded(accent, 12)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setOnClickListener {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 101)
            }
        }
        micCard.addView(micGrantBtn, lp(10))
        root.addView(micCard, lp(12))

        // ---- 2. Gemini API Key Card ----
        val keyCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(surface, 16)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        keyCard.addView(text("2. Gemini API Key (Free Tier)", 15f, fg, bold = true))
        keyCard.addView(
            text("Stored locally on your phone only. Never uploaded or shared.", 12.5f, muted),
            lp(3),
        )
        apiKeyInput = EditText(this).apply {
            hint = "Paste AIza... key from Google AI Studio"
            setTextColor(fg)
            setHintTextColor(muted)
            textSize = 14.5f
            background = rounded(bg, 10, line, 1)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
            setText(GeminiVoiceEngine.apiKey(this@VoiceSettingsActivity))
        }
        keyCard.addView(apiKeyInput, lp(10))

        val keyBtnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val showBtn = text("Show", 13.5f, fg, bold = true).apply {
            gravity = Gravity.CENTER
            background = rounded(bg, 10, line, 1)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener {
                val masked = (apiKeyInput.inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0
                apiKeyInput.inputType = if (masked) {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                } else {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
                text = if (masked) "Hide" else "Show"
                apiKeyInput.setSelection(apiKeyInput.text?.length ?: 0)
            }
        }
        val getKeyBtn = text("Get Free Key ↗", 13.5f, accent, bold = true).apply {
            gravity = Gravity.CENTER
            background = rounded(bg, 10, line, 1)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey")))
            }
        }
        val saveKeyBtn = text("Save", 14f, onAccent, bold = true).apply {
            gravity = Gravity.CENTER
            background = rounded(accent, 10)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setOnClickListener { saveSettings(showToast = true) }
        }
        keyBtnRow.addView(showBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(6) })
        keyBtnRow.addView(getKeyBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f).apply { rightMargin = dp(6) })
        keyBtnRow.addView(saveKeyBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        keyCard.addView(keyBtnRow, lp(10))
        root.addView(keyCard, lp(12))

        // ---- 3. Language & Model Card ----
        val optCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(surface, 16)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        optCard.addView(text("3. Language & Smart Cleanup", 15f, fg, bold = true))

        val seg = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rounded(bg, 12, line, 1)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        langButtons.clear()
        listOf("auto" to "Auto (BN+EN)", "bn" to "বাংলা", "en" to "English").forEach { (id, label) ->
            val btn = text(label, 13f, fg, bold = true).apply {
                gravity = Gravity.CENTER
                setPadding(dp(6), dp(10), dp(6), dp(10))
                setOnClickListener {
                    Prefs.setString(this@VoiceSettingsActivity, GeminiVoiceEngine.KEY_LANG, id)
                    refreshLangChips()
                }
            }
            langButtons.add(id to btn)
            seg.addView(btn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        optCard.addView(seg, lp(10))

        val cleanRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(14), 0, dp(6))
        }
        val cleanText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("Smart punctuation & cleanup", 15f, fg))
            addView(text("Adds দাঁড়ি, comma, ? and removes filler sounds", 12f, muted))
        }
        val cleanSwitch = Switch(this).apply {
            isChecked = GeminiVoiceEngine.cleanPunctuation(this@VoiceSettingsActivity)
            setOnCheckedChangeListener { _, checked ->
                Prefs.setString(this@VoiceSettingsActivity, GeminiVoiceEngine.KEY_CLEAN, if (checked) "1" else "0")
            }
        }
        cleanRow.addView(cleanText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        cleanRow.addView(cleanSwitch)
        optCard.addView(cleanRow, lp())

        optCard.addView(text("Gemini Model ID", 13.5f, muted, bold = true), lp(10))
        modelInput = EditText(this).apply {
            hint = GeminiVoiceEngine.DEFAULT_MODEL
            setTextColor(fg)
            setHintTextColor(muted)
            textSize = 14f
            background = rounded(bg, 10, line, 1)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setSingleLine(true)
            setText(GeminiVoiceEngine.model(this@VoiceSettingsActivity))
        }
        optCard.addView(modelInput, lp(6))

        val testBtn = text("Save & Test Gemini Connection", 14.5f, onAccent, bold = true).apply {
            gravity = Gravity.CENTER
            background = rounded(accent, 12)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setOnClickListener {
                saveSettings(showToast = false)
                testStatusText.text = "Testing connection…"
                testStatusText.setTextColor(muted)
                GeminiVoiceEngine.testApiKey(this@VoiceSettingsActivity) { ok, msg ->
                    testStatusText.text = msg
                    testStatusText.setTextColor(if (ok) accent else 0xFFD32F2F.toInt())
                }
            }
        }
        optCard.addView(testBtn, lp(12))
        testStatusText = text("", 13f, muted)
        optCard.addView(testStatusText, lp(8))
        root.addView(optCard, lp(12))

        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            addView(root)
        }
        setContentView(scroll)

        refreshMicStatus()
        refreshLangChips()
    }

    override fun onResume() {
        super.onResume()
        refreshMicStatus()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshMicStatus()
    }

    private fun refreshMicStatus() {
        val granted = GeminiVoiceEngine.hasMicPermission(this)
        micStatusText.text = if (granted) "Status: Granted — microphone ready" else "Status: Not granted yet"
        micStatusText.setTextColor(if (granted) accent else muted)
        micGrantBtn.visibility = if (granted) View.GONE else View.VISIBLE
    }

    private fun refreshLangChips() {
        val cur = GeminiVoiceEngine.langMode(this)
        for ((id, tv) in langButtons) {
            if (id == cur) {
                tv.background = rounded(accent, 9)
                tv.setTextColor(onAccent)
            } else {
                tv.background = null
                tv.setTextColor(fg)
            }
        }
    }

    private fun saveSettings(showToast: Boolean) {
        val key = apiKeyInput.text?.toString()?.trim() ?: ""
        val mdl = modelInput.text?.toString()?.trim().orEmpty().ifEmpty { GeminiVoiceEngine.DEFAULT_MODEL }
        Prefs.setString(this, GeminiVoiceEngine.KEY_API, key)
        Prefs.setString(this, GeminiVoiceEngine.KEY_MODEL, mdl)
        if (showToast) {
            Toast.makeText(this, "AI Voice settings saved", Toast.LENGTH_SHORT).show()
        }
    }
}
