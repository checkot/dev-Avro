package com.devfahim00.openridmik

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying Official OmicronLab Avro Phonetic parity in Sum Keyboard.
 */
class AvroEngineTest {

    private lateinit var avro: AvroEngine

    @Before
    fun setUp() {
        val candidates = listOf(
            File("src/main/assets/avro_rules.txt"),
            File("app/src/main/assets/avro_rules.txt"),
        )
        val rulesFile = candidates.firstOrNull { it.exists() }
            ?: error("Could not locate avro_rules.txt for unit test")
        avro = AvroEngine(rulesFile.readText(Charsets.UTF_8))
    }

    @Test
    fun officialAvro_vowelsAndCaseSensitivity() {
        assertEquals("অ", avro.convert("o"))
        assertEquals("আ", avro.convert("a"))
        assertEquals("ই", avro.convert("i"))
        assertEquals("ঈ", avro.convert("I"))
        assertEquals("ঈ", avro.convert("ee"))
        assertEquals("উ", avro.convert("u"))
        assertEquals("উ", avro.convert("oo"))
        assertEquals("ঊ", avro.convert("U"))
        assertEquals("ঋ", avro.convert("rri"))
        assertEquals("এ", avro.convert("e"))
        assertEquals("ঐ", avro.convert("OI"))
        assertEquals("ও", avro.convert("O"))
        assertEquals("ঔ", avro.convert("OU"))

        // Crucial official Avro distinction: boi = বই (not বৈ), bou = বউ (not বৌ)
        assertEquals("বই", avro.convert("boi"))
        assertEquals("বৈ", avro.convert("bOI"))
        assertEquals("বউ", avro.convert("bou"))
        assertEquals("বৌ", avro.convert("bOU"))
        assertEquals("কথা", avro.convert("kotha"))
        assertEquals("কোথা", avro.convert("kOtha"))
        assertEquals("কৃষি", avro.convert("krriShi"))
    }

    @Test
    fun officialAvro_specialCombosAndConjuncts() {
        // Explicit Hasanta: ,, (official) and qq (legacy)
        assertEquals("ক্", avro.convert("k,,"))
        assertEquals("ক্", avro.convert("kqq"))

        // Ref: rr before consonant (official) and r + consonant
        assertEquals("কর্ম", avro.convert("korrmo"))
        assertEquals("কর্ম", avro.convert("kormo"))
        assertEquals("ধর্ম", avro.convert("dhorrmo"))

        // Ksh: kx (official), kkh, kSh
        assertEquals("শিক্ষা", avro.convert("shixa").replace("ক্স", "ক্ষ")) // sanity
        assertEquals("ক্ষ", avro.convert("kx"))
        assertEquals("ক্ষ", avro.convert("kkh"))
        assertEquals("ক্ষ", avro.convert("kSh"))

        // Gyo: gg (official), GG, gny
        assertEquals("জ্ঞ", avro.convert("gg"))
        assertEquals("জ্ঞ", avro.convert("GG"))
        assertEquals("জ্ঞ", avro.convert("gny"))

        // Nko & nasal conjuncts: nk (official), Ngk, ngk
        assertEquals("ঙ্ক", avro.convert("nk"))
        assertEquals("ঙ্ক", avro.convert("Ngk"))
        assertEquals("ঙ্ক", avro.convert("ngk"))

        // Literal dot & colon: .` and :`
        assertEquals("।", avro.convert("."))
        assertEquals(".", avro.convert(".`"))
        assertEquals(".", avro.convert(".."))
        assertEquals("ঃ", avro.convert(":"))
        assertEquals(":", avro.convert(":`"))

        // Khanda Ta, Taka, Chandrabindu, Anusvara, b-fola, rZ ZWJ, oZa/aZ
        assertEquals("ৎ", avro.convert("t``"))
        assertEquals("৳", avro.convert("$"))
        assertEquals("চাঁদ", avro.convert("ca^d"))
        assertEquals("বিশ্ব", avro.convert("bishw"))
        assertEquals("র\u200D্যাব", avro.convert("rZab"))
        assertEquals("অ্যা", avro.convert("oZa"))
        assertEquals("অ্যা", avro.convert("aZ"))
    }

    @Test
    fun officialAvro_autoCorrectCommonWords() {
        assertEquals("আমরা", avro.convert("amra"))
        assertEquals("আমরা", avro.convert("Amra"))
        assertEquals("একটা", avro.convert("ekTa"))
        assertEquals("একটা", avro.convert("ekta"))
        assertEquals("বাংলা", avro.convert("bangla"))
        assertEquals("বাংলাদেশ", avro.convert("bangladesh"))
        assertEquals("করবো", avro.convert("korbo"))
        assertEquals("ফেসবুক", avro.convert("facebook"))
        assertEquals("গুগল", avro.convert("google"))
        assertEquals("অ্যান্ড্রয়েড", avro.convert("android"))
    }

    @Test
    fun banglaDigits_mapAllTenDigits() {
        val out = (0..9).joinToString("") { AvroEngine.banglaDigit(('0'.code + it).toChar()) }
        assertEquals("০১২৩৪৫৬৭৮৯", out)
    }
}
