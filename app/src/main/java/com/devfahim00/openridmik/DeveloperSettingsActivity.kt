package com.devfahim00.openridmik

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/**
 * Dedicated settings screen for Developer Mode (hardware keyboard shortcuts & text macros).
 *
 * Intentionally separated from ordinary keyboard settings so normal typing remains
 * simple and unaffected unless the user explicitly enables Developer Mode.
 */
class DeveloperSettingsActivity : Activity() {

    private var bg = 0
    private var surface = 0
    private var line = 0
    private var fg = 0
    private var muted = 0
    private var accent = 0
    private var onAccent = 0

    private lateinit var statusBadge: TextView
    private lateinit var masterSwitch: Switch
    private lateinit var countLabel: TextView
    private lateinit var shortcutContainer: LinearLayout

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, r: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(r).toFloat()
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
            addView(text("Developer Mode", 26f, fg, bold = true))
            addView(text("Custom hardware keyboard shortcuts & text macros", 13f, muted))
        }
        topBar.addView(backBtn)
        topBar.addView(titleCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(topBar, lp())

        // ---- Master toggle card ----
        val toggleCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(surface, 16)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val toggleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val toggleTextCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("Enable Developer Mode", 17f, fg, bold = true))
            statusBadge = text("", 13f, muted)
            addView(statusBadge, lp(2))
        }
        masterSwitch = Switch(this).apply {
            isChecked = Prefs.isDeveloperMode(this@DeveloperSettingsActivity)
            setOnCheckedChangeListener { _, checked ->
                Prefs.setDeveloperMode(this@DeveloperSettingsActivity, checked)
                ShortcutManager.invalidate()
                updateMasterStatus(checked)
            }
        }
        toggleRow.addView(toggleTextCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        toggleRow.addView(masterSwitch)
        toggleCard.addView(toggleRow, lp())

        val toggleNote = text(
            "Default is OFF. When disabled, custom shortcuts never run and normal Avro phonetic typing is completely untouched.",
            12.5f,
            muted,
        )
        toggleCard.addView(toggleNote, lp(10))
        root.addView(toggleCard, lp(12))

        // ---- Add Shortcut primary button ----
        val addBtn = text("+ Add Shortcut", 16f, onAccent, bold = true).apply {
            gravity = Gravity.CENTER
            background = rounded(accent, 14)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setOnClickListener {
                startActivity(Intent(this@DeveloperSettingsActivity, ShortcutCaptureActivity::class.java))
            }
        }
        root.addView(addBtn, lp(14))

        // ---- Configured shortcuts header ----
        val listHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(20), dp(8), dp(6))
        }
        val sectionTitle = text("CONFIGURED SHORTCUTS", 12f, muted, bold = true).apply {
            letterSpacing = 0.08f
        }
        countLabel = text("", 12f, muted)
        listHeader.addView(sectionTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        listHeader.addView(countLabel)
        root.addView(listHeader, lp())

        shortcutContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(shortcutContainer, lp(4))

        // ---- Android limitations note ----
        val limHeader = text("ANDROID HARDWARE KEYBOARD NOTES", 12f, muted, bold = true).apply {
            letterSpacing = 0.08f
            setPadding(dp(8), dp(22), dp(8), dp(6))
        }
        root.addView(limHeader, lp())

        val limCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(surface, 16)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        limCard.addView(
            text(
                "• Shortcuts work with physical (Bluetooth/USB) keyboards while My Avro Keyboard is the active input method.\n" +
                    "• Android and some foreground apps intercept system combos (Meta/Win key, Alt+Tab, Ctrl+Space, Ctrl+C/V/X/A/Z) before they reach the keyboard.\n" +
                    "• Best reliable combinations: Ctrl + Alt + Letter/Number, Ctrl + Shift + Letter/Number, or Alt + Shift + Letter/Number.\n" +
                    "• For security, macros are automatically blocked inside password and PIN fields.",
                13f,
                muted,
            ).apply { setLineSpacing(dp(3).toFloat(), 1f) },
            lp(),
        )
        root.addView(limCard, lp(4))

        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            addView(root)
        }
        setContentView(scroll)

        updateMasterStatus(Prefs.isDeveloperMode(this))
    }

    override fun onResume() {
        super.onResume()
        val enabled = Prefs.isDeveloperMode(this)
        masterSwitch.isChecked = enabled
        updateMasterStatus(enabled)
        renderShortcuts()
    }

    private fun updateMasterStatus(enabled: Boolean) {
        statusBadge.text = if (enabled) "Status: ON — custom shortcuts active" else "Status: OFF — normal typing only"
        statusBadge.setTextColor(if (enabled) accent else muted)
    }

    private fun renderShortcuts() {
        shortcutContainer.removeAllViews()
        val shortcuts = ShortcutRepository.loadAll(this)
        countLabel.text = if (shortcuts.size == 1) "1 shortcut" else "${shortcuts.size} shortcuts"

        if (shortcuts.isEmpty()) {
            val emptyCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(surface, 16)
                setPadding(dp(18), dp(20), dp(18), dp(20))
            }
            emptyCard.addView(text("No shortcuts configured yet", 16f, fg, bold = true))
            emptyCard.addView(
                text(
                    "Tap \"+ Add Shortcut\" to bind a hardware key combination (such as Ctrl + Alt + M or Ctrl + Shift + E) to a Bangla or English text macro.",
                    13.5f,
                    muted,
                ).apply { setLineSpacing(dp(2).toFloat(), 1f) },
                lp(6),
            )
            shortcutContainer.addView(emptyCard, lp())
            return
        }

        shortcuts.forEachIndexed { idx, sc ->
            shortcutContainer.addView(buildShortcutCard(sc), lp(if (idx == 0) 0 else 10))
        }
    }

    private fun buildShortcutCard(sc: Shortcut): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(surface, 16)
            setPadding(dp(16), dp(14), dp(16), dp(12))
            alpha = if (sc.enabled) 1f else 0.65f
        }

        // Top row: Name + Enable/Disable switch
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        titleCol.addView(
            text(sc.name, 16.5f, fg, bold = true).apply {
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            },
        )

        // Combo pill badge
        val pillBg = Color.argb(36, Color.red(accent), Color.green(accent), Color.blue(accent))
        val pill = text(sc.displayCombo(), 12.5f, accent, bold = true).apply {
            background = rounded(pillBg, 8)
            setPadding(dp(8), dp(3), dp(8), dp(3))
        }
        titleCol.addView(
            pill,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(6) },
        )

        val itemSwitch = Switch(this).apply {
            isChecked = sc.enabled
            setOnCheckedChangeListener { _, checked ->
                ShortcutRepository.setEnabled(this@DeveloperSettingsActivity, sc.id, checked)
                card.alpha = if (checked) 1f else 0.65f
            }
        }
        top.addView(titleCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(itemSwitch)
        card.addView(top, lp())

        // Macro preview
        val preview = text(sc.macroText, 13.5f, muted).apply {
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, dp(8), 0, dp(8))
        }
        card.addView(preview, lp())

        // Divider
        val div = View(this).apply { setBackgroundColor(line) }
        card.addView(div, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1))

        // Action row: Edit | Delete
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(6), 0, 0)
        }
        val editBtn = text("Edit", 14f, accent, bold = true).apply {
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener {
                val i = Intent(this@DeveloperSettingsActivity, ShortcutCaptureActivity::class.java)
                i.putExtra(ShortcutCaptureActivity.EXTRA_SHORTCUT_ID, sc.id)
                startActivity(i)
            }
        }
        val deleteBtn = text("Delete", 14f, Color.parseColor("#D32F2F"), bold = true).apply {
            setPadding(dp(12), dp(6), dp(6), dp(6))
            setOnClickListener { confirmDelete(sc) }
        }
        actions.addView(editBtn)
        actions.addView(deleteBtn)
        card.addView(actions, lp())

        return card
    }

    private fun confirmDelete(sc: Shortcut) {
        AlertDialog.Builder(this)
            .setTitle("Delete shortcut?")
            .setMessage("Remove \"${sc.name}\" (${sc.displayCombo()})? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                ShortcutRepository.delete(this, sc.id)
                Toast.makeText(this, "Deleted \"${sc.name}\"", Toast.LENGTH_SHORT).show()
                renderShortcuts()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
