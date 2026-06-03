package com.nuvio.app.core.license

expect object LicenseStorage {
    fun loadLicenseCode(): String?
    fun saveLicenseCode(code: String)
    fun clearLicenseCode()
}
