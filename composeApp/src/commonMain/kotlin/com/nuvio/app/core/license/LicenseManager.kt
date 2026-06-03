package com.nuvio.app.core.license

import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.AuthState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

object LicenseManager {
    private const val LICENSE_REGISTRY_URL =
        "https://raw.githubusercontent.com/v1rtech/v1rtvbox/refs/heads/master/Nuvio/nuvio_license_registry.json"

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _status = MutableStateFlow<LicenseStatus>(LicenseStatus.Loading)
    val status: StateFlow<LicenseStatus> = _status.asStateFlow()

    fun initialize() {
        scope.launch {
            AuthRepository.state.collect { authState ->
                val code = LicenseStorage.loadLicenseCode()
                _status.value = LicenseStatus.Loading
                _status.value = resolveStatus(code, authState.emailOrNull())
            }
        }
    }

    suspend fun submitLicenseCode(rawCode: String): LicenseStatus {
        val normalized = rawCode.trim()
        if (normalized.isBlank()) {
            LicenseStorage.clearLicenseCode()
            _status.value = LicenseStatus.Missing
            return LicenseStatus.Missing
        }
        LicenseStorage.saveLicenseCode(normalized)
        val status = resolveStatus(normalized, AuthRepository.state.value.emailOrNull())
        _status.value = status
        return status
    }

    suspend fun clearLicenseCode() {
        LicenseStorage.clearLicenseCode()
        _status.value = LicenseStatus.Missing
    }

    suspend fun refresh() {
        val code = LicenseStorage.loadLicenseCode()
        _status.value = LicenseStatus.Loading
        _status.value = resolveStatus(code, AuthRepository.state.value.emailOrNull())
    }

    suspend fun resolveStatus(rawCode: String?, signedInEmail: String?): LicenseStatus {
        val code = rawCode?.trim().orEmpty()
        if (code.isBlank()) return LicenseStatus.Missing

        val registry = fetchRegistry() ?: return LicenseStatus.NetworkError
        val record = registry.firstOrNull { it.licenceCode.equals(code, ignoreCase = true) }
            ?: return LicenseStatus.Invalid(rawCode)

        val normalizedEmail = signedInEmail?.trim().orEmpty()
        if (normalizedEmail.isBlank() || !record.email.equals(normalizedEmail, ignoreCase = true)) {
            return LicenseStatus.AccountMismatch(record, signedInEmail)
        }

        val now = currentIsoTimestamp()
        if (record.startedAt > now) {
            return LicenseStatus.NotStarted(record, record.startedAt)
        }
        if (record.deadlineAt < now) {
            val expiredDaysAgo = daysBetweenIso(record.deadlineAt, now)
            return LicenseStatus.Expired(record, record.deadlineAt, expiredDaysAgo)
        }

        val remainingDays = daysBetweenIso(now, record.deadlineAt)
        return LicenseStatus.Valid(record, record.deadlineAt, remainingDays)
    }

    private suspend fun fetchRegistry(): List<LicenseRecord>? =
        platformFetchLicenseRegistry(LICENSE_REGISTRY_URL, json)
}

private fun AuthState.emailOrNull(): String? =
    (this as? AuthState.Authenticated)?.email
