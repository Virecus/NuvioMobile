@file:Suppress("unused")

package com.lagradost.cloudstream3

import android.app.Activity
import android.content.Context
import com.lagradost.api.setContext
import com.lagradost.cloudstream3.utils.DataStore
import com.lagradost.cloudstream3.utils.DataStore.getKey
import com.lagradost.cloudstream3.utils.DataStore.setKey
import com.lagradost.cloudstream3.utils.DataStore.removeKey
import com.lagradost.cloudstream3.utils.DataStore.removeKeys
import com.lagradost.cloudstream3.utils.DataStore.getFolderName
import java.lang.ref.WeakReference

/**
 * Stub for CloudStream's AcraApplication.
 * Extensions reference AcraApplication.context and getKey/setKey for settings persistence.
 * Backed by SharedPreferences via DataStore so extensions like InatBox can store/retrieve keys.
 */
open class AcraApplication {
    companion object {
        /** Application context stub. Extensions use this for PackageManager etc. */
        @JvmStatic
        var context: Context? = null
            set(value) {
                field = value
                // Also set the library's context so WebViewResolver and other
                // library components can access it
                if (value != null) {
                    setContext(WeakReference(value))
                }
            }

        /**
         * Weak reference to the current Activity. CloudStream extensions
         * often require a non-null Activity in their load() method to
         * register MainAPIs. Set from MainActivity.onCreate().
         */
        private var activityRef: WeakReference<Activity>? = null

        @JvmStatic
        fun getActivity(): Activity? = activityRef?.get()

        @JvmStatic
        fun setActivity(activity: Activity?) {
            activityRef = if (activity != null) WeakReference(activity) else null
            // Update the library's context to the Activity (preferred for WebView)
            if (activity != null) {
                setContext(WeakReference(activity))
            }
        }

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> getKey(path: String, key: String, default: T? = null): T? {
            val ctx = context ?: return default
            return try { ctx.getKey<Any>(getFolderName(path, key), default) as? T ?: default }
            catch (_: Exception) { default }
        }

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> getKey(key: String, default: T? = null): T? {
            val ctx = context ?: return default
            return try { ctx.getKey<Any>(key, default) as? T ?: default }
            catch (_: Exception) { default }
        }

        @JvmStatic
        fun setKey(path: String, key: String, value: Any?) {
            val ctx = context ?: return
            try { ctx.setKey(getFolderName(path, key), value) } catch (_: Exception) {}
        }

        @JvmStatic
        fun setKey(key: String, value: Any?) {
            val ctx = context ?: return
            try { ctx.setKey(key, value) } catch (_: Exception) {}
        }

        @JvmStatic
        fun removeKeys(prefix: String) {
            val ctx = context ?: return
            try { ctx.removeKeys(prefix) } catch (_: Exception) {}
        }

        @JvmStatic
        fun removeKey(path: String, key: String) {
            val ctx = context ?: return
            try { ctx.removeKey(getFolderName(path, key)) } catch (_: Exception) {}
        }

        @JvmStatic
        fun removeKey(key: String) {
            val ctx = context ?: return
            try { ctx.removeKey(key) } catch (_: Exception) {}
        }
    }
}
