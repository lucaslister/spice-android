package com.undatech.opaque.ui.data

import android.content.Context
import com.undatech.opaque.ui.theme.ThemeMode
import org.json.JSONArray

/** Lightweight, non-secret UI preferences (theme, dynamic color, recent sessions). */
class AppPreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("spice_ui_prefs", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        // Default to Light (user preference); Dark and System are selectable in Settings.
        get() = runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.LIGHT.name)!!) }
            .getOrDefault(ThemeMode.LIGHT)
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    var dynamicColor: Boolean
        // Default off so the designed teal brand scheme shows; opt into wallpaper colors in Settings.
        get() = prefs.getBoolean(KEY_DYNAMIC, false)
        set(value) = prefs.edit().putBoolean(KEY_DYNAMIC, value).apply()

    /** Most-recent-first list of launched sessions (capped at [MAX_RECENTS]). */
    fun getRecents(): List<RecentSession> {
        val raw = prefs.getString(KEY_RECENTS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { RecentSession.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    /** Records a launch: de-duplicates by VM, moves it to the front, trims to [MAX_RECENTS]. */
    fun addRecent(session: RecentSession) {
        val updated = (listOf(session) + getRecents().filter { it.key != session.key })
            .take(MAX_RECENTS)
        val arr = JSONArray().apply { updated.forEach { put(it.toJson()) } }
        prefs.edit().putString(KEY_RECENTS, arr.toString()).apply()
    }

    /** Drops any recents that point at a server that no longer exists. */
    fun pruneRecents(validServerIds: Set<String>) {
        val kept = getRecents().filter { it.serverId in validServerIds }
        val arr = JSONArray().apply { kept.forEach { put(it.toJson()) } }
        prefs.edit().putString(KEY_RECENTS, arr.toString()).apply()
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_DYNAMIC = "dynamic_color"
        const val KEY_RECENTS = "recent_sessions"
        const val MAX_RECENTS = 6
    }
}
