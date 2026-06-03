package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.extra_settings_developer_mode_title
import nuvio.composeapp.generated.resources.extra_settings_live_tv_title
import nuvio.composeapp.generated.resources.extra_settings_live_tv_subtitle
import nuvio.composeapp.generated.resources.extra_settings_translate_trakt_title
import nuvio.composeapp.generated.resources.extra_settings_translate_trakt_subtitle
import nuvio.composeapp.generated.resources.extra_settings_section_general

private const val DEVELOPER_PASSWORD = "istanbul123"

internal fun LazyListScope.extraSettingsContent(
    isTablet: Boolean,
    uiState: ExtraSettings,
) {
    item {
        var showPasswordDialog by remember { mutableStateOf(false) }

        if (showPasswordDialog) {
            DeveloperPasswordDialog(
                onConfirm = {
                    ExtraSettingsRepository.setDeveloperMode(true)
                    showPasswordDialog = false
                },
                onDismiss = { showPasswordDialog = false },
            )
        }

        SettingsSection(
            title = stringResource(Res.string.extra_settings_section_general),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.extra_settings_live_tv_title),
                    description = stringResource(Res.string.extra_settings_live_tv_subtitle),
                    checked = uiState.liveTvEnabled,
                    isTablet = isTablet,
                    onCheckedChange = ExtraSettingsRepository::setLiveTvEnabled,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.extra_settings_translate_trakt_title),
                    description = stringResource(Res.string.extra_settings_translate_trakt_subtitle),
                    checked = uiState.translateTraktComments,
                    isTablet = isTablet,
                    onCheckedChange = ExtraSettingsRepository::setTranslateTraktComments,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.extra_settings_developer_mode_title),
                    description = null,
                    checked = uiState.developerMode,
                    isTablet = isTablet,
                    onCheckedChange = { newValue ->
                        if (newValue) {
                            showPasswordDialog = true
                        } else {
                            ExtraSettingsRepository.setDeveloperMode(false)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DeveloperPasswordDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Geliştirici Ayarları") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Devam etmek için şifreyi girin.")
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; error = false },
                    placeholder = { Text("Şifre") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = error,
                    supportingText = if (error) ({ Text("Hatalı şifre") }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (input == DEVELOPER_PASSWORD) {
                    onConfirm()
                } else {
                    error = true
                    input = ""
                }
            }) {
                Text("Onayla")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        },
    )
}
