package com.devfahim00.openridmik

/**
 * Rule-driven phonetic (Official OmicronLab Avro-compatible) Latin -> Bangla transliterator.
 * Supports official Avro combos (,, hasanta, rr ref, kx/kkh, gg/GG, nk/Ngk, .`, :`, OI/OU/O,
 * rZ -> র‍্য with ZWJ, word-initial oZa/aZ -> অ্যা) plus a built-in phonetic Auto-Correct table.
 */
class AvroEngine(rulesText: String) {

    private enum class T { V, C, W, J, S }

    private class Rule(val type: T, val value: String, val kar: String)

    private val rules = HashMap<String, Rule>()
    private var maxLen = 1

    /** One row of the rules file, kept in file order (used by the key-map screen). */
    class Entry(val key: String, val kind: Char, val value: String, val kar: String)

    val entries = ArrayList<Entry>()

    init {
        for (line in rulesText.lines()) {
            val l = line.trim()
            if (l.isEmpty() || l.startsWith("#")) continue
            val p = l.split(Regex("\\s+"))
            if (p.size < 3) continue
            val type = when (p[0]) {
                "V" -> T.V
                "C" -> T.C
                "W" -> T.W
                "J" -> T.J
                else -> T.S
            }
            val value = if (p[2] == "-") "" else p[2]
            val kar = if (p.size > 3 && p[3] != "-") p[3] else ""
            rules[p[1]] = Rule(type, value, kar)
            entries.add(Entry(p[1], p[0][0], value, kar))
            if (p[1].length > maxLen) maxLen = p[1].length
        }
    }

    private fun lookupRule(input: String, pos: Int): Pair<Rule, Int>? {
        var l = minOf(maxLen, input.length - pos)
        while (l > 0) {
            val r = rules[input.substring(pos, pos + l)]
            if (r != null) return Pair(r, l)
            l--
        }
        if (pos < input.length && input[pos].isUpperCase()) {
            val lc = input[pos].lowercaseChar()
            var l2 = minOf(maxLen, input.length - pos)
            while (l2 > 0) {
                val r = rules[lc.toString() + input.substring(pos + 1, pos + l2)]
                if (r != null) return Pair(r, l2)
                l2--
            }
        }
        return null
    }

    private fun isNextConsonant(input: String, pos: Int): Boolean {
        if (pos >= input.length) return false
        val next = lookupRule(input, pos) ?: return false
        return next.first.type == T.C || next.first.type == T.J || next.first.type == T.W
    }

    private fun lookupAutoCorrect(input: String): String? {
        AUTO_CORRECT[input]?.let { return it }
        if (input.length >= 2 && input[0].isUpperCase() && input.substring(1).all { it.isLowerCase() }) {
            AUTO_CORRECT[input.lowercase()]?.let { return it }
        }
        return null
    }

    fun convert(input: String): String {
        if (input.isEmpty()) return ""
        lookupAutoCorrect(input)?.let { return it }

        val out = StringBuilder()
        var prevCons = false
        var lastWasRa = false
        var i = 0
        while (i < input.length) {
            // Official Avro: word-initial or post-vowel oZa / aZ -> অ্যা (অ + ্ + য + া)
            if (!prevCons) {
                if (input.startsWith("oZa", i)) {
                    out.append("\u0985\u09CD\u09AF\u09BE")
                    prevCons = false
                    lastWasRa = false
                    i += 3
                    continue
                }
                if (input.startsWith("aZ", i)) {
                    out.append("\u0985\u09CD\u09AF\u09BE")
                    prevCons = false
                    lastWasRa = false
                    i += 2
                    continue
                }
                if (input.startsWith("oZ", i)) {
                    out.append("\u0985\u09CD\u09AF")
                    prevCons = true
                    lastWasRa = false
                    i += 2
                    continue
                }
            }

            // Official Avro: "rr" before a consonant produces Ref (র্), e.g., korrmo -> কর্ম
            // (Keep "rri" -> ঋ/ৃ higher priority by checking !startsWith("rri"))
            if (input.startsWith("rr", i) && !input.startsWith("rri", i)) {
                if (isNextConsonant(input, i + 2)) {
                    if (prevCons) out.append(HASANTA)
                    out.append(RA)
                    prevCons = true
                    lastWasRa = true
                } else {
                    if (prevCons) out.append(HASANTA)
                    out.append(RA).append(HASANTA).append(RA)
                    prevCons = true
                    lastWasRa = true
                }
                i += 2
                continue
            }

            val found = lookupRule(input, i)
            if (found == null) {
                out.append(input[i])
                prevCons = false
                lastWasRa = false
                i++
                continue
            }

            val rule = found.first
            val len = found.second
            val key = input.substring(i, i + len)

            when (rule.type) {
                T.V -> {
                    out.append(if (prevCons) rule.kar else rule.value)
                    prevCons = false
                    lastWasRa = false
                }
                T.C -> {
                    if (prevCons) out.append(HASANTA)
                    out.append(rule.value)
                    prevCons = true
                    lastWasRa = (rule.value == RA_STR)
                }
                T.W -> {
                    if (prevCons) {
                        out.append(HASANTA).append(rule.kar)
                        prevCons = true
                    } else {
                        out.append(rule.value)
                        prevCons = false
                    }
                    lastWasRa = false
                }
                T.J -> {
                    if (prevCons) {
                        // Official Avro: r + Z -> র‍্য (Ra + ZWJ + Hasanta + Ja) e.g. rZab -> র‍্যাব
                        if (lastWasRa && key == "Z") {
                            out.append(ZWJ).append(HASANTA).append(rule.kar)
                        } else {
                            out.append(HASANTA).append(rule.kar)
                        }
                    } else {
                        out.append(rule.value)
                    }
                    prevCons = true
                    lastWasRa = false
                }
                T.S -> {
                    out.append(rule.value)
                    prevCons = false
                    lastWasRa = false
                }
            }
            i += len
        }
        return out.toString()
    }

    companion object {
        const val HASANTA = '\u09CD'
        private const val RA = '\u09B0'
        private const val RA_STR = "\u09B0"
        private const val ZWJ = '\u200D'
        private val BN_DIGITS = "\u09E6\u09E7\u09E8\u09E9\u09EA\u09EB\u09EC\u09ED\u09EE\u09EF"

        fun banglaDigit(d: Char): String =
            if (d in '0'..'9') BN_DIGITS[d - '0'].toString() else d.toString()

        /**
         * Official Avro-style phonetic Auto-Correct dictionary for high-frequency Bengali words
         * and loanwords where natural typing omits 'o' or backtick.
         */
        private val AUTO_CORRECT: Map<String, String> = mapOf(
            // Pronouns & people
            "amra" to "আমরা",
            "amader" to "আমাদের",
            "amake" to "আমাকে",
            "amar" to "আমার",
            "tomra" to "তোমরা",
            "tomader" to "তোমাদের",
            "tomake" to "তোমাকে",
            "tomar" to "তোমার",
            "tumar" to "তোমার",
            "apni" to "আপনি",
            "apnara" to "আপনারা",
            "apnake" to "আপনাকে",
            "apnar" to "আপনার",
            "apnader" to "আপনাদের",
            "tader" to "তাদের",
            "tara" to "তারা",
            "onara" to "ওনারা",
            "keu" to "কেউ",
            "shobai" to "সবাই",
            "sobai" to "সবাই",
            "manush" to "মানুষ",
            "bondhu" to "বন্ধু",
            "bhai" to "ভাই",
            "vai" to "ভাই",
            "apu" to "আপু",
            "chele" to "ছেলে",
            "meye" to "মেয়ে",

            // Everyday words with tricky consonant clusters
            "ekta" to "একটা",
            "ekTa" to "একটা",
            "ektu" to "একটু",
            "ekTu" to "একটু",
            "ekdom" to "একদম",
            "ekbare" to "একবারে",
            "ekhon" to "এখন",
            "ekhono" to "এখনো",
            "ekhane" to "এখানে",
            "okhane" to "ওখানে",
            "sekhane" to "সেখানে",
            "jekhane" to "যেখানে",
            "kokhon" to "কখন",
            "kokhono" to "কখনো",
            "tokhon" to "তখন",
            "jokhon" to "যখন",
            "arekta" to "আরেকটা",
            "arekTa" to "আরেকটা",
            "bangla" to "বাংলা",
            "bangladesh" to "বাংলাদেশ",
            "dhaka" to "ঢাকা",
            "gazipur" to "গাজীপুর",
            "bhalo" to "ভালো",
            "valo" to "ভালো",
            "kemon" to "কেমন",
            "achen" to "আছেন",
            "achhen" to "আছেন",
            "acho" to "আছো",
            "achho" to "আছো",
            "aso" to "আছো",
            "ajke" to "আজকে",
            "kalke" to "কালকে",
            "porshu" to "পরশু",
            "sokal" to "সকাল",
            "shokal" to "সকাল",
            "somoy" to "সময়",
            "shomoy" to "সময়",
            "sobsomoy" to "সবসময়",
            "shobshomoy" to "সবসময়",
            "sob" to "সব",
            "shob" to "সব",
            "sobkichu" to "সবকিছু",
            "shobkichu" to "সবকিছু",
            "shobcheye" to "সবচেয়ে",
            "sobcheye" to "সবচেয়ে",
            "kichu" to "কিছু",
            "kono" to "কোনো",
            "jekono" to "যেকোনো",
            "kothay" to "কোথায়",
            "keno" to "কেন",
            "jodi" to "যদি",
            "jodio" to "যদিও",
            "kintu" to "কিন্তু",
            "karon" to "কারণ",
            "tahole" to "তাহলে",
            "tobe" to "তবে",
            "tarpor" to "তারপর",
            "tarmane" to "তারমানে",
            "erpor" to "এরপর",
            "protidin" to "প্রতিদিন",
            "dorkar" to "দরকার",
            "nischoy" to "নিশ্চয়",
            "oboshshoi" to "অবশ্যই",
            "obosshoi" to "অবশ্যই",
            "shudhu" to "শুধু",
            "sudhu" to "শুধু",
            "onek" to "অনেক",
            "khub" to "খুব",
            "beshi" to "বেশি",
            "thik" to "ঠিক",
            "bhul" to "ভুল",
            "vul" to "ভুল",
            "shundor" to "সুন্দর",
            "sundor" to "সুন্দর",
            "dhonnobad" to "ধন্যবাদ",
            "shuvo" to "শুভ",
            "jibon" to "জীবন",
            "prithibi" to "পৃথিবী",
            "kache" to "কাছে",
            "sathe" to "সাথে",
            "shathe" to "সাথে",
            "theke" to "থেকে",
            "jonno" to "জন্য",
            "jonne" to "জন্যে",
            "moddhe" to "মধ্যে",
            "upore" to "উপরে",
            "niche" to "নিচে",
            "bari" to "বাড়ি",

            // Common verbs that auto-conjunct without Auto-Correct
            "korbo" to "করবো",
            "korbe" to "করবে",
            "korben" to "করবেন",
            "korchi" to "করছি",
            "korchhi" to "করছি",
            "kortesi" to "করতেছি",
            "korte" to "করতে",
            "korlam" to "করলাম",
            "korlo" to "করলো",
            "bolbo" to "বলবো",
            "bolbe" to "বলবে",
            "bolben" to "বলবেন",
            "bolchi" to "বলছি",
            "bolchhi" to "বলছি",
            "bolte" to "বলতে",
            "bollam" to "বললাম",
            "bollo" to "বললো",
            "dekhbo" to "দেখবো",
            "dekhbe" to "দেখবে",
            "dekhben" to "দেখবেন",
            "dekhchi" to "দেখছি",
            "dekhte" to "দেখতে",
            "dekhlam" to "দেখলাম",
            "asbo" to "আসবো",
            "ashbo" to "আসবো",
            "asbe" to "আসবে",
            "ashbe" to "আসবে",
            "asben" to "আসবেন",
            "aschi" to "আসছি",
            "ashchi" to "আসছি",
            "aslam" to "আসলাম",
            "ashlam" to "আসলাম",
            "aste" to "আসতে",
            "ashte" to "আসতে",
            "thakbo" to "থাকবো",
            "thakbe" to "থাকবে",
            "thakben" to "থাকবেন",
            "thaklam" to "থাকলাম",
            "thakte" to "থাকতে",
            "parbo" to "পারবো",
            "parbe" to "পারবে",
            "parben" to "পারবেন",
            "parchi" to "পারছি",
            "parlam" to "পারলাম",
            "parte" to "পারতে",
            "lagbe" to "লাগবে",
            "lagche" to "লাগছে",
            "laglo" to "লাগলো",
            "hobe" to "হবে",
            "hocche" to "হচ্ছে",
            "holo" to "হলো",
            "hoyeche" to "হয়েছে",
            "hoise" to "হইছে",
            "bujhlam" to "বুঝলাম",
            "bujhchi" to "বুঝছি",
            "bujhte" to "বুঝতে",
            "bujhbo" to "বুঝবো",
            "janina" to "জানিনা",
            "jante" to "জানতে",
            "jabo" to "যাবো",
            "jacchi" to "যাচ্ছি",
            "gelam" to "গেলাম",

            // Greetings & common expressions
            "allah" to "আল্লাহ",
            "inshallah" to "ইনশাআল্লাহ",
            "alhamdulillah" to "আলহামদুলিল্লাহ",
            "assalamu" to "আসসালামু",
            "alaikum" to "আলাইকুম",
            "salam" to "সালাম",

            // Common English loanwords (Official Avro Auto-Correct style)
            "facebook" to "ফেসবুক",
            "google" to "গুগল",
            "youtube" to "ইউটিউব",
            "whatsapp" to "হোয়াটসঅ্যাপ",
            "android" to "অ্যান্ড্রয়েড",
            "keyboard" to "কিবোর্ড",
            "mobile" to "মোবাইল",
            "phone" to "ফোন",
            "internet" to "ইন্টারনেট",
            "computer" to "কম্পিউটার",
            "office" to "অফিস",
            "school" to "স্কুল",
            "college" to "কলেজ",
            "university" to "ইউনিভার্সিটি",
            "hospital" to "হাসপাতাল",
            "doctor" to "ডাক্তার",
            "police" to "পুলিশ",
            "bank" to "ব্যাংক",
            "email" to "ইমেইল",
            "message" to "মেসেজ",
            "video" to "ভিডিও",
            "photo" to "ফটো",
            "status" to "স্ট্যাটাস",
            "comment" to "কমেন্ট",
            "share" to "শেয়ার",
            "like" to "লাইক",
            "group" to "গ্রুপ",
            "page" to "পেজ",
            "link" to "লিংক",
            "app" to "অ্যাপ",
            "update" to "আপডেট",
            "download" to "ডাউনলোড",
            "install" to "ইনস্টল",
            "setting" to "সেটিং",
            "settings" to "সেটিংস",
        )
    }
}

/**
 * Bundled dictionary for one language: words ordered by frequency (most common first) plus a small table of
 * common word pairs and sentence starters, so suggestions work from the very first keystroke after install.
 */
class Suggester(text: String, seed: String = "") {
    private val words = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    private val follow = HashMap<String, List<String>>()
    private var starters: List<String> = emptyList()

    init {
        for (line in seed.lines()) {
            val parts = line.trim().split(' ').filter { it.isNotEmpty() }
            if (parts.size < 2) continue
            if (parts[0] == "^") starters = parts.drop(1) else follow[parts[0]] = parts.drop(1)
        }
    }

    /** Words that start with [prefix], most frequent first. */
    fun suggest(prefix: String, max: Int): List<String> {
        if (prefix.isEmpty()) return emptyList()
        val out = ArrayList<String>(max)
        for (w in words) {
            if (w.length > prefix.length && w.startsWith(prefix)) {
                out.add(w)
                if (out.size >= max) break
            }
        }
        return out
    }

    /** Common words that follow [prev]. */
    fun next(prev: String, max: Int): List<String> = follow[prev]?.take(max) ?: emptyList()

    /** Typical first words of a sentence. */
    fun starters(max: Int): List<String> = starters.take(max)

    /** The most common words overall (used when nothing better is known). */
    fun top(max: Int): List<String> = words.take(max)
}
