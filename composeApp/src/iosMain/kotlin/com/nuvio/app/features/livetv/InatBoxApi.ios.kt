package com.nuvio.app.features.livetv

// iOS: CloudStream DEX plugins are Android-only, so InatBox live TV is unavailable.
actual suspend fun fetchPluginLiveChannels(sourceId: String): List<LiveChannel>? = null
actual suspend fun resolvePluginChannel(sourceId: String, channelId: String): LiveResolveResult =
    LiveResolveResult.Empty
actual suspend fun resolvePluginEpisodeStream(sourceId: String, episodeData: String): PluginLiveStream? = null
