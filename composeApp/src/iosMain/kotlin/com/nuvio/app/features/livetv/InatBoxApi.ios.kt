package com.nuvio.app.features.livetv

// iOS'ta InatBox API şifreleme desteklenmiyor, null döner
actual suspend fun fetchInatBoxAllChannels(): List<LiveChannel>? = null
actual suspend fun resolveInatBoxStreamUrl(itemJson: String): String? = null
