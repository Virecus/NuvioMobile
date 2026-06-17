package com.nuvio.app.features.livetv

import android.content.Context

/**
 * PlayStore variant: no CloudStream plugin runtime and no Conscrypt provider.
 * Live TV here is limited to the built-in Giniko source, so there is nothing to
 * initialise.
 */
fun initLiveTvRuntime(context: Context) {
    // no-op
}
