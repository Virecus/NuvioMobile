package com.nuvio.app.core.license

import platform.Foundation.NSUserDefaults

actual object LicenseStorage {
    private const val KEY_LICENSE_CODE = "royal_license_code"

    actual fun loadLicenseCode(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_LICENSE_CODE)?.takeIf { it.isNotBlank() }

    actual fun saveLicenseCode(code: String) {
        NSUserDefaults.standardUserDefaults.setObject(code, forKey = KEY_LICENSE_CODE)
    }

    actual fun clearLicenseCode() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY_LICENSE_CODE)
    }
}
