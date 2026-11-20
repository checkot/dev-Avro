package com.devfahim00.openridmik

import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Avro Phonetic & Keyboard Typing Guide migrated from the keyboard toolbar book icon
 * into Settings so the toolbar slot can be dedicated to Developer PC & Fn modes.
 */
class KeymapGuideActivity : Activity() {

    private var bg = 0
    private var surface = 0
    private var line = 0
    private var fg = 0
    private var muted = 0
    private var accent = 0

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

        // Top bar
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), 0, dp(4), dp(10))
        }
        val backBtn = text("←", 24f, fg, bold = true).apply {
            setPadding(dp(6), dp(6), dp(14), dp(6))
            setOnClickListener { finish() }
        }
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("Avro Typing Guide", 24f, fg, bold = true))
            addView(text("Phonetic Bangla rules & quick keyboard gestures", 13f, muted))
        }
        topBar.addView(backBtn)
        topBar.addView(titleCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(topBar, lp())

        addSection(
            root,
            "VOWELS (স্বরবর্ণ)",
            listOf(
                "o → অ" to "a → আ",
                "i → ই" to "I / ee → ঈ",
                "u → উ" to "U / oo → ঊ",
                "rri → ঋ" to "e → এ",
                "OI → ঐ" to "O → ও",
                "OU → ঔ" to "` (backtick) → break kar",
            ),
        )

        addSection(
            root,
            "CONSONANTS (ব্যঞ্জনবর্ণ)",
            listOf(
                "k → ক   kh → খ" to "g → গ   gh → ঘ   Ng → ঙ",
                "c → চ   ch → ছ" to "j → জ   jh → ঝ   NG → ঞ",
                "T → ট   Th → ঠ" to "D → ড   Dh → ঢ   N → ণ",
                "t → ত   th → থ" to "d → দ   dh → ধ   n → ন",
                "p → প   ph/f → ফ" to "b → ব   bh/v → ভ   m → ম",
                "z → য   r → র" to "l → ল   sh/S → শ   Sh → ষ",
                "s → স   h → হ" to "R → ড়   Rh → ঢ়   y → য়",
                "t`` → ৎ   ng → ং" to ": → ঃ   ^ → ঁ",
            ),
        )

        addSection(
            root,
            "COMMON EXAMPLES",
            listOf(
                "ami → আমি" to "bangla → বাংলা",
                "desh → দেশ" to "kemon → কেমন",
                "dhonnobad → ধন্যবাদ" to "bhalobasha → ভালোবাসা",
                "proshno → প্রশ্ন" to "shikkha → শিক্ষা",
            ),
        )

        addSection(
            root,
            "KEYBOARD SHORTCUTS & GESTURES",
            listOf(
                "Slide Spacebar" to "Switch English / Avro / Probhat / Jatiyo",
                "Top Bar </> Button" to "Tap to cycle: Normal → Dev PC → Dev Fn/Arrows",
                "Top Bar Mic Button" to "1-tap Gemini AI Live Voice Typing",
                "Long-press x / c / v" to "Cut / Copy / Paste",
                "Long-press letters" to "Quick symbols (৳ % _ ; @ # & / etc.)",
            ),
        )

        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            addView(root)
        }
        setContentView(scroll)
    }

    private fun addSection(root: LinearLayout, heading: String, pairs: List<Pair<String, String>>) {
        val h = text(heading, 12f, muted, bold = true).apply {
            letterSpacing = 0.08f
            setPadding(dp(8), dp(18), dp(8), dp(6))
        }
        root.addView(h, lp())

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(surface, 16)
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        pairs.forEachIndexed { idx, (left, right) ->
            if (idx > 0) {
                val div = View(this).apply { setBackgroundColor(line) }
                card.addView(div, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1))
            }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(2), dp(10), dp(2), dp(10))
            }
            row.addView(text(left, 13.5f, fg, bold = true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(text(right, 13.5f, accent), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.15f))
            card.addView(row, lp())
        }
        root.addView(card, lp())
    }
}
