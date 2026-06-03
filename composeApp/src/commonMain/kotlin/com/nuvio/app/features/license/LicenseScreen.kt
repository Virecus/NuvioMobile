package com.nuvio.app.features.license

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.AuthState
import com.nuvio.app.core.license.LicenseManager
import com.nuvio.app.core.license.LicenseRecord
import com.nuvio.app.core.license.LicenseStatus
import com.nuvio.app.core.license.formatIsoDate
import com.nuvio.app.core.ui.NuvioSurfaceCard
import com.nuvio.app.core.ui.nuvioOverlayGradientBrush
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.app_logo_wordmark
import nuvio.composeapp.generated.resources.compose_auth_email
import nuvio.composeapp.generated.resources.compose_auth_password
import nuvio.composeapp.generated.resources.compose_auth_sign_in
import nuvio.composeapp.generated.resources.license_account_mismatch_subtitle
import nuvio.composeapp.generated.resources.license_account_mismatch_title
import nuvio.composeapp.generated.resources.license_account_required_subtitle
import nuvio.composeapp.generated.resources.license_account_section_subtitle
import nuvio.composeapp.generated.resources.license_account_section_title
import nuvio.composeapp.generated.resources.license_activate
import nuvio.composeapp.generated.resources.license_clear
import nuvio.composeapp.generated.resources.license_code_placeholder
import nuvio.composeapp.generated.resources.license_expired_subtitle
import nuvio.composeapp.generated.resources.license_expired_title
import nuvio.composeapp.generated.resources.license_gate_hint
import nuvio.composeapp.generated.resources.license_invalid_subtitle
import nuvio.composeapp.generated.resources.license_invalid_title
import nuvio.composeapp.generated.resources.license_loading
import nuvio.composeapp.generated.resources.license_loading_subtitle
import nuvio.composeapp.generated.resources.license_missing_subtitle
import nuvio.composeapp.generated.resources.license_missing_title
import nuvio.composeapp.generated.resources.license_network_error_subtitle
import nuvio.composeapp.generated.resources.license_network_error_title
import nuvio.composeapp.generated.resources.license_not_started_subtitle
import nuvio.composeapp.generated.resources.license_not_started_title
import nuvio.composeapp.generated.resources.license_signed_in_as
import nuvio.composeapp.generated.resources.license_subtitle
import nuvio.composeapp.generated.resources.license_switch_account
import nuvio.composeapp.generated.resources.license_title
import nuvio.composeapp.generated.resources.license_valid_subtitle
import nuvio.composeapp.generated.resources.license_valid_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LicenseScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    var licenseCode by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<LicenseStatus>(LicenseStatus.Loading) }
    var isSubmitting by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }
    var isSigningIn by remember { mutableStateOf(false) }

    val authState by AuthRepository.state.collectAsStateWithLifecycle()
    val authRepoError by AuthRepository.error.collectAsStateWithLifecycle()

    LaunchedEffect(authRepoError) {
        if (authRepoError != null) authError = authRepoError
    }

    LaunchedEffect(Unit) {
        LicenseManager.status.collect { status = it }
    }

    LaunchedEffect(Unit) {
        licenseCode = com.nuvio.app.core.license.LicenseStorage.loadLicenseCode().orEmpty()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = nuvioOverlayGradientBrush())
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = statusBarTop + 60.dp,
                    bottom = 40.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Image(
                    painter = painterResource(Res.drawable.app_logo_wordmark),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(48.dp),
                    contentScale = ContentScale.Fit,
                )

                Text(
                    text = stringResource(Res.string.license_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Text(
                    text = stringResource(Res.string.license_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                // Account section
                NuvioSurfaceCard {
                    Text(
                        text = stringResource(Res.string.license_account_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val signedIn = authState as? AuthState.Authenticated
                    if (signedIn == null || signedIn.isAnonymous) {
                        Text(
                            text = stringResource(Res.string.license_account_section_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; authError = null },
                            label = { Text(stringResource(Res.string.compose_auth_email)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        var showPassword by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; authError = null },
                            label = { Text(stringResource(Res.string.compose_auth_password)) },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                        contentDescription = null,
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                if (!isSigningIn && email.isNotBlank() && password.isNotBlank()) {
                                    scope.launch {
                                        isSigningIn = true
                                        authError = null
                                        val result = AuthRepository.signInWithEmail(email.trim(), password)
                                        if (result.isFailure) {
                                            authError = result.exceptionOrNull()?.message
                                        }
                                        isSigningIn = false
                                    }
                                }
                            }),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        if (authError != null) {
                            Text(
                                text = authError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    isSigningIn = true
                                    authError = null
                                    val result = AuthRepository.signInWithEmail(email.trim(), password)
                                    if (result.isFailure) {
                                        authError = result.exceptionOrNull()?.message
                                    }
                                    isSigningIn = false
                                }
                            },
                            enabled = !isSigningIn && email.isNotBlank() && password.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text(stringResource(Res.string.compose_auth_sign_in))
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(Res.string.license_signed_in_as, signedIn.email ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { scope.launch { AuthRepository.signOut() }; Unit },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(Res.string.license_switch_account))
                        }
                    }
                }

                // License status card
                LicenseStatusCard(status = status)

                // License key input
                OutlinedTextField(
                    value = licenseCode,
                    onValueChange = { licenseCode = it },
                    label = { Text(stringResource(Res.string.license_code_placeholder)) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        scope.launch {
                            isSubmitting = true
                            status = LicenseManager.submitLicenseCode(licenseCode)
                            isSubmitting = false
                        }
                    }),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                isSubmitting = true
                                status = LicenseManager.submitLicenseCode(licenseCode)
                                isSubmitting = false
                            }
                        },
                        enabled = !isSubmitting && (authState as? AuthState.Authenticated)?.isAnonymous == false,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(stringResource(Res.string.license_activate))
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                licenseCode = ""
                                LicenseManager.clearLicenseCode()
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(Res.string.license_clear))
                    }
                }

                Text(
                    text = stringResource(Res.string.license_gate_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun LicenseStatusCard(status: LicenseStatus) {
    data class Content(
        val title: String,
        val subtitle: String,
        val icon: ImageVector,
        val accent: Color,
        val record: LicenseRecord? = null,
    )

    val content = when (status) {
        LicenseStatus.Loading -> Content(
            title = stringResource(Res.string.license_loading),
            subtitle = stringResource(Res.string.license_loading_subtitle),
            icon = Icons.Default.VpnKey,
            accent = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LicenseStatus.Missing -> Content(
            title = stringResource(Res.string.license_missing_title),
            subtitle = stringResource(Res.string.license_missing_subtitle),
            icon = Icons.Default.ErrorOutline,
            accent = Color(0xFFFFB74D),
        )
        is LicenseStatus.Invalid -> Content(
            title = stringResource(Res.string.license_invalid_title),
            subtitle = stringResource(Res.string.license_invalid_subtitle),
            icon = Icons.Default.ErrorOutline,
            accent = MaterialTheme.colorScheme.error,
        )
        LicenseStatus.NetworkError -> Content(
            title = stringResource(Res.string.license_network_error_title),
            subtitle = stringResource(Res.string.license_network_error_subtitle),
            icon = Icons.Default.ErrorOutline,
            accent = MaterialTheme.colorScheme.error,
        )
        is LicenseStatus.AccountMismatch -> Content(
            title = stringResource(Res.string.license_account_mismatch_title),
            subtitle = if (status.signedInEmail.isNullOrBlank()) {
                stringResource(Res.string.license_account_required_subtitle)
            } else {
                stringResource(Res.string.license_account_mismatch_subtitle)
            },
            icon = Icons.Default.ErrorOutline,
            accent = Color(0xFFFFB74D),
            record = status.record,
        )
        is LicenseStatus.NotStarted -> Content(
            title = stringResource(Res.string.license_not_started_title),
            subtitle = stringResource(Res.string.license_not_started_subtitle, formatIsoDate(status.startsAt)),
            icon = Icons.Default.ErrorOutline,
            accent = Color(0xFFFFB74D),
            record = status.record,
        )
        is LicenseStatus.Expired -> Content(
            title = stringResource(Res.string.license_expired_title),
            subtitle = stringResource(Res.string.license_expired_subtitle, formatIsoDate(status.deadlineAt)),
            icon = Icons.Default.ErrorOutline,
            accent = MaterialTheme.colorScheme.error,
            record = status.record,
        )
        is LicenseStatus.Valid -> Content(
            title = stringResource(Res.string.license_valid_title),
            subtitle = stringResource(
                Res.string.license_valid_subtitle,
                status.remainingDays.toString(),
                formatIsoDate(status.deadlineAt),
            ),
            icon = Icons.Default.CheckCircle,
            accent = Color(0xFF7CFF9B),
            record = status.record,
        )
    }

    NuvioSurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = content.icon,
                contentDescription = null,
                tint = content.accent,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = content.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = content.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (content.record != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${content.record.firstName} ${content.record.lastName}  •  ${content.record.email}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

