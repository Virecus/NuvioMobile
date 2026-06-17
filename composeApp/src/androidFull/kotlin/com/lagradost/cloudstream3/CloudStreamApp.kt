@file:Suppress("unused")

package com.lagradost.cloudstream3

/**
 * Stub for CloudStream's CloudStreamApp.
 * Some plugins (e.g. RecTV) reference CloudStreamApp.Companion directly.
 * Extends AcraApplication and re-exposes the same companion API.
 */
open class CloudStreamApp : AcraApplication() {
    companion object {
        @JvmStatic
        var context: android.content.Context?
            get() = AcraApplication.context
            set(value) { AcraApplication.context = value }

        @JvmStatic
        fun <T> getKey(path: String, key: String, default: T? = null): T? = default

        @JvmStatic
        fun <T> getKey(key: String, default: T? = null): T? = default

        @JvmStatic
        fun setKey(path: String, key: String, value: Any?) {}

        @JvmStatic
        fun setKey(key: String, value: Any?) {}

        @JvmStatic
        fun removeKeys(prefix: String) {}

        @JvmStatic
        fun removeKey(path: String, key: String) {}

        @JvmStatic
        fun removeKey(key: String) {}
    }
}
