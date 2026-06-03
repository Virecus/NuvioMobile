package com.nuvio.app.core.license

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LicenseRecord(
    val id: String,
    @SerialName("ad") val firstName: String,
    @SerialName("soyad") val lastName: String,
    @SerialName("tel") val phone: String,
    @SerialName("mail") val email: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("deadline_at") val deadlineAt: String,
    @SerialName("licence_code") val licenceCode: String,
)

sealed interface LicenseStatus {
    data object Loading : LicenseStatus
    data object Missing : LicenseStatus
    data object NetworkError : LicenseStatus
    data class Invalid(val attemptedCode: String?) : LicenseStatus
    data class AccountMismatch(val record: LicenseRecord, val signedInEmail: String?) : LicenseStatus
    data class NotStarted(val record: LicenseRecord, val startsAt: String) : LicenseStatus
    data class Expired(val record: LicenseRecord, val deadlineAt: String, val expiredDaysAgo: Long) : LicenseStatus
    data class Valid(val record: LicenseRecord, val deadlineAt: String, val remainingDays: Long) : LicenseStatus
}
