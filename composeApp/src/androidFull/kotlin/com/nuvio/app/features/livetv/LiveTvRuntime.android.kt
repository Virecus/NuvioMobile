package com.nuvio.app.features.livetv

import android.content.Context
import android.util.Log
import com.nuvio.app.features.livetv.cloudstream.CloudstreamRuntime
import com.nuvio.app.features.livetv.cloudstream.InatBoxPluginBridge
import org.conscrypt.Conscrypt
import java.security.Security

private const val TAG = "LiveTvRuntime"

@Volatile private var conscryptInstalled = false

/**
 * Full variant: install the Conscrypt security provider (needed for the modern
 * TLS handshakes some plugin hosts require) and wire up the CloudStream DEX
 * plugin bridge with the application context.
 */
fun initLiveTvRuntime(context: Context) {
    ensureConscrypt()
    CloudstreamRuntime.initialize(context.applicationContext)
    InatBoxPluginBridge.initialize(context.applicationContext)
}

private fun ensureConscrypt() {
    if (conscryptInstalled) return
    synchronized(CloudstreamRuntime) {
        if (conscryptInstalled) return
        try {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
            Log.d(TAG, "Conscrypt installed")
        } catch (e: Exception) {
            Log.w(TAG, "Conscrypt install failed: ${e.message}")
        }
        conscryptInstalled = true
    }
}
