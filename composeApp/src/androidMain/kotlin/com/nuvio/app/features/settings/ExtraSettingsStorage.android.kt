package com.nuvio.app.features.settings

import android.content.Context
import android.content.SharedPreferences

actual object ExtraSettingsStorage {
    private const val PREFS_NAME = "royal_extra_settings"
    private const val KEY_LIVE_TV = "live_tv_enabled"
    private const val KEY_TRANSLATE_TRAKT = "translate_trakt_comments"
    private const val KEY_DEVELOPER_MODE = "developer_mode"
    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadLiveTvEnabled(): Boolean? =
        prefs?.let { if (it.contains(KEY_LIVE_TV)) it.getBoolean(KEY_LIVE_TV, true) else null }

    actual fun saveLiveTvEnabled(value: Boolean) {
        prefs?.edit()?.putBoolean(KEY_LIVE_TV, value)?.apply()
    }

    actual fun loadTranslateTraktComments(): Boolean? =
        prefs?.let { if (it.contains(KEY_TRANSLATE_TRAKT)) it.getBoolean(KEY_TRANSLATE_TRAKT, false) else null }

    actual fun saveTranslateTraktComments(value: Boolean) {
        prefs?.edit()?.putBoolean(KEY_TRANSLATE_TRAKT, value)?.apply()
    }

    actual fun loadDeveloperMode(): Boolean? =
        prefs?.let { if (it.contains(KEY_DEVELOPER_MODE)) it.getBoolean(KEY_DEVELOPER_MODE, false) else null }

    actual fun saveDeveloperMode(value: Boolean) {
        prefs?.edit()?.putBoolean(KEY_DEVELOPER_MODE, value)?.apply()
    }
}
