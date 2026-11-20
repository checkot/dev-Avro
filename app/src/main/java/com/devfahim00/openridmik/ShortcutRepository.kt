package com.devfahim00.openridmik

import android.content.Context
import org.json.JSONArray

/**
 * Local-only persistent storage for custom hardware keyboard shortcuts.
 *
 * Uses private [android.content.SharedPreferences] with JSON serialization,
 * matching OpenRidmik's zero-dependency persistence style.
 */
object ShortcutRepository {

    private const val PREF_NAME = "myavro_shortcuts"
    private const val KEY_LIST = "shortcuts_json"
    private const val KEY_REV = "shortcuts_rev"

    private fun sp(c: Context) = c.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /** Monotonically increasing revision number so [ShortcutManager] detects updates immediately. */
    fun revision(c: Context): Long = sp(c).getLong(KEY_REV, 0L)

    fun loadAll(c: Context): List<Shortcut> {
        val raw = sp(c).getString(KEY_LIST, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val result = ArrayList<Shortcut>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                Shortcut.fromJson(obj)?.let { result.add(it) }
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveAll(c: Context, shortcuts: List<Shortcut>) {
        val arr = JSONArray()
        for (s in shortcuts) {
            arr.put(s.toJson())
        }
        val nextRev = revision(c) + 1L
        sp(c).edit()
            .putString(KEY_LIST, arr.toString())
            .putLong(KEY_REV, nextRev)
            .apply()
        ShortcutManager.invalidate()
    }

    fun findById(c: Context, id: String): Shortcut? =
        loadAll(c).firstOrNull { it.id == id }

    fun findDuplicate(
        c: Context,
        ctrl: Boolean,
        shift: Boolean,
        alt: Boolean,
        keyCode: Int,
        excludeId: String? = null,
    ): Shortcut? {
        val targetKey = Shortcut.comboKey(ctrl, shift, alt, keyCode)
        return loadAll(c).firstOrNull { it.id != excludeId && it.comboKey() == targetKey }
    }

    fun add(c: Context, shortcut: Shortcut) {
        val list = loadAll(c).toMutableList()
        list.add(shortcut)
        saveAll(c, list)
    }

    fun update(c: Context, updated: Shortcut): Boolean {
        val list = loadAll(c).toMutableList()
        val idx = list.indexOfFirst { it.id == updated.id }
        if (idx < 0) return false
        list[idx] = updated
        saveAll(c, list)
        return true
    }

    fun delete(c: Context, id: String): Boolean {
        val list = loadAll(c).toMutableList()
        val removed = list.removeAll { it.id == id }
        if (removed) saveAll(c, list)
        return removed
    }

    fun setEnabled(c: Context, id: String, enabled: Boolean): Boolean {
        val list = loadAll(c).toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return false
        list[idx] = list[idx].copy(enabled = enabled)
        saveAll(c, list)
        return true
    }
}
