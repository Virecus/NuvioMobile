package com.nuvio.app.features.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ExtraSettings(
    val liveTvEnabled: Boolean = false,
    val translateTraktComments: Boolean = false,
    val developerMode: Boolean = false,
)

object ExtraSettingsRepository {
    private val _uiState = MutableStateFlow(ExtraSettings())
    val uiState: StateFlow<ExtraSettings> = _uiState.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true
        val liveTv = ExtraSettingsStorage.loadLiveTvEnabled() ?: true
        val translate = ExtraSettingsStorage.loadTranslateTraktComments() ?: true
        val devMode = ExtraSettingsStorage.loadDeveloperMode() ?: false
        _uiState.value = ExtraSettings(liveTvEnabled = liveTv, translateTraktComments = translate, developerMode = devMode)
    }

    fun setLiveTvEnabled(value: Boolean) {
        ExtraSettingsStorage.saveLiveTvEnabled(value)
        _uiState.value = _uiState.value.copy(liveTvEnabled = value)
    }

    fun setTranslateTraktComments(value: Boolean) {
        ExtraSettingsStorage.saveTranslateTraktComments(value)
        _uiState.value = _uiState.value.copy(translateTraktComments = value)
    }

    fun setDeveloperMode(value: Boolean) {
        ExtraSettingsStorage.saveDeveloperMode(value)
        _uiState.value = _uiState.value.copy(developerMode = value)
    }
}
