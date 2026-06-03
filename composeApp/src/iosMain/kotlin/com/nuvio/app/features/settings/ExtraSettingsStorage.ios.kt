package com.nuvio.app.features.settings

import platform.Foundation.NSUserDefaults

actual object ExtraSettingsStorage {
    private const val KEY_LIVE_TV = "royal_extra_live_tv_enabled"
    private const val KEY_TRANSLATE_TRAKT = "royal_extra_translate_trakt_comments"

    actual fun loadLiveTvEnabled(): Boolean? {
        val defaults = NSUserDefaults.standardUserDefaults
        return if (defaults.objectForKey(KEY_LIVE_TV) != null) defaults.boolForKey(KEY_LIVE_TV) else null
    }

    actual fun saveLiveTvEnabled(value: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(value, forKey = KEY_LIVE_TV)
    }

    actual fun loadTranslateTraktComments(): Boolean? {
        val defaults = NSUserDefaults.standardUserDefaults
        return if (defaults.objectForKey(KEY_TRANSLATE_TRAKT) != null) defaults.boolForKey(KEY_TRANSLATE_TRAKT) else null
    }

    actual fun saveTranslateTraktComments(value: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(value, forKey = KEY_TRANSLATE_TRAKT)
    }

    actual fun loadDeveloperMode(): Boolean? {
        val defaults = NSUserDefaults.standardUserDefaults
        return if (defaults.objectForKey("royal_extra_developer_mode") != null) defaults.boolForKey("royal_extra_developer_mode") else null
    }

    actual fun saveDeveloperMode(value: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(value, forKey = "royal_extra_developer_mode")
    }
}
