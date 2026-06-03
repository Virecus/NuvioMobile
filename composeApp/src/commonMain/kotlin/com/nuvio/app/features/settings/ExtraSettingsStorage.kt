package com.nuvio.app.features.settings

expect object ExtraSettingsStorage {
    fun loadLiveTvEnabled(): Boolean?
    fun saveLiveTvEnabled(value: Boolean)
    fun loadTranslateTraktComments(): Boolean?
    fun saveTranslateTraktComments(value: Boolean)
    fun loadDeveloperMode(): Boolean?
    fun saveDeveloperMode(value: Boolean)
}
