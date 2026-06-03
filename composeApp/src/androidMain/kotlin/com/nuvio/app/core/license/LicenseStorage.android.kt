package com.nuvio.app.core.license

import android.content.Context
import android.content.SharedPreferences

actual object LicenseStorage {
    private const val PREFS_NAME = "royal_license"
    private const val KEY_LICENSE_CODE = "license_code"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadLicenseCode(): String? =
        preferences?.getString(KEY_LICENSE_CODE, null)?.takeIf { it.isNotBlank() }

    actual fun saveLicenseCode(code: String) {
        preferences?.edit()?.putString(KEY_LICENSE_CODE, code)?.apply()
    }

    actual fun clearLicenseCode() {
        preferences?.edit()?.remove(KEY_LICENSE_CODE)?.apply()
    }
}
