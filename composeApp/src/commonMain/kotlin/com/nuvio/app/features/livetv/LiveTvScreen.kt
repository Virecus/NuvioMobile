package com.nuvio.app.features.livetv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun LiveTvScreen(
    modifier: Modifier = Modifier,
    onPlay: (title: String, url: String) -> Unit = { _, _ -> },
) {
    val scope = rememberCoroutineScope()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val allSources = remember { listOf(INATBOX_SOURCE, GINIKO_SOURCE) }
    var state by remember { mutableStateOf<LiveTvState>(LiveTvState.Idle) }
    var navState by remember { mutableStateOf<LiveNavState>(LiveNavState.SourceList) }
    var loadingChannelId by remember { mutableStateOf<String?>(null) }
    var loadingSourceId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (state is LiveTvState.Idle) {
            state = LiveTvState.Ready(
                sources = allSources,
                channelsBySource = emptyMap(),
            )
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val s = state) {
            LiveTvState.Idle,
            LiveTvState.Loading -> LiveTvLoading()

            LiveTvState.NetworkError -> LiveTvError {
                scope.launch {
                    state = LiveTvState.Ready(sources = allSources, channelsBySource = emptyMap())
                    navState = LiveNavState.SourceList
                }
            }

            is LiveTvState.Ready -> {
                AnimatedContent(
                    targetState = navState,
                    label = "live_tv_nav",
                    transitionSpec = {
                        val forward = targetState !is LiveNavState.SourceList &&
                            initialState is LiveNavState.SourceList ||
                            targetState is LiveNavState.ChannelList &&
                            initialState is LiveNavState.CategoryList
                        if (forward) {
                            (slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(280)))
                                .togetherWith(slideOutHorizontally(tween(220)) { -it / 4 } + fadeOut(tween(220)))
                        } else {
                            (slideInHorizontally(tween(280)) { -it / 3 } + fadeIn(tween(280)))
                                .togetherWith(slideOutHorizontally(tween(220)) { it / 4 } + fadeOut(tween(220)))
                        }
                    },
                ) { nav ->
                    when (nav) {
                        LiveNavState.SourceList -> {
                            LiveSourceListScreen(
                                sources = s.sources,
                                loadingSourceId = loadingSourceId,
                                statusBarTop = statusBarTop,
                                onSourceSelected = { source ->
                                    val cached = s.channelsBySource[source.id]
                                    if (cached != null) {
                                        navState = source.toNavCategoryList()
                                    } else {
                                        scope.launch {
                                            loadingSourceId = source.id
                                            val channels = LiveTvRepository.fetchChannelsForSource(source.id)
                                            loadingSourceId = null
                                            if (channels != null) {
                                                state = s.copy(
                                                    channelsBySource = s.channelsBySource + (source.id to channels)
                                                )
                                                navState = source.toNavCategoryList()
                                            } else {
                                                state = LiveTvState.NetworkError
                                            }
                                        }
                                    }
                                },
                            )
                        }

                        is LiveNavState.CategoryList -> {
                            val source = nav.toSource()
                            val sourceChannels = s.channelsBySource[source.id] ?: emptyList()
                            val channelsByCategory = remember(sourceChannels) {
                                sourceChannels.groupBy { it.category }
                            }
                            val categories = remember(sourceChannels) {
                                sourceChannels.map { it.category }.distinct().sorted()
                            }
                            LiveCategoryListScreen(
                                source = source,
                                categories = categories,
                                channelsByCategory = channelsByCategory,
                                statusBarTop = statusBarTop,
                                onBack = { navState = LiveNavState.SourceList },
                                onCategorySelected = { category ->
                                    navState = source.toNavChannelList(category)
                                },
                            )
                        }

                        is LiveNavState.ChannelList -> {
                            val source = nav.toSource()
                            val sourceChannels2 = s.channelsBySource[source.id] ?: emptyList()
                            val channels = remember(sourceChannels2, nav.category) {
                                sourceChannels2.filter { it.category == nav.category }
                            }
                            LiveChannelListScreen(
                                source = source,
                                category = nav.category,
                                channels = channels,
                                loadingChannelId = loadingChannelId,
                                statusBarTop = statusBarTop,
                                onBack = { navState = source.toNavCategoryList() },
                                onChannelClick = { channel ->
                                    if (loadingChannelId != null) return@LiveChannelListScreen
                                    scope.launch {
                                        loadingChannelId = channel.id
                                        val url = LiveTvRepository.resolveStreamUrl(channel)
                                        loadingChannelId = null
                                        if (url != null) {
                                            onPlay(channel.name, url)
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Kaynak listesi ────────────────────────────────────────────────────────────

@Composable
private fun LiveSourceListScreen(
    sources: List<LiveSource>,
    loadingSourceId: String?,
    statusBarTop: androidx.compose.ui.unit.Dp,
    onSourceSelected: (LiveSource) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = statusBarTop + 16.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(key = "header") {
            Text(
                text = "Canlı TV",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item(key = "sources_label") {
            Text(
                text = "Platformlar",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        item(key = "sources_row") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(sources, key = { it.id }) { source ->
                    LiveSourceCard(
                        source = source,
                        isLoading = loadingSourceId == source.id,
                        onClick = { if (loadingSourceId == null) onSourceSelected(source) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveSourceCard(source: LiveSource, isLoading: Boolean = false, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(200.dp).height(112.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp).align(Alignment.Center),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else if (!source.logo.isNullOrBlank()) {
                AsyncImage(
                    model = source.logo,
                    contentDescription = source.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(48.dp).align(Alignment.Center),
                )
            } else {
                Icon(
                    Icons.Default.Tv,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(40.dp).align(Alignment.Center),
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(0.7f)),
                        ),
                        RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (source.description.isNotBlank()) {
                    Text(
                        text = source.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ─── Kategori listesi ──────────────────────────────────────────────────────────

@Composable
private fun LiveCategoryListScreen(
    source: LiveSource,
    categories: List<String>,
    channelsByCategory: Map<String, List<LiveChannel>>,
    statusBarTop: androidx.compose.ui.unit.Dp,
    onBack: () -> Unit,
    onCategorySelected: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = statusBarTop + 8.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
                }
                Column {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Canlı TV",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        itemsIndexed(categories, key = { _, cat -> cat }) { _, category ->
            val count = channelsByCategory[category]?.size ?: 0
            LiveCategoryRow(
                category = category,
                count = count,
                onClick = { onCategorySelected(category) },
            )
        }
    }
}

@Composable
private fun LiveCategoryRow(
    category: String,
    count: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$count içerik",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}

// ─── Kanal listesi ─────────────────────────────────────────────────────────────

@Composable
private fun LiveChannelListScreen(
    source: LiveSource,
    category: String,
    channels: List<LiveChannel>,
    loadingChannelId: String?,
    statusBarTop: androidx.compose.ui.unit.Dp,
    onBack: () -> Unit,
    onChannelClick: (LiveChannel) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = statusBarTop + 8.dp,
            start = 16.dp,
            end = 16.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
                }
                Column {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "${source.name} · ${channels.size} içerik",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))
        }

        itemsIndexed(channels, key = { _, ch -> ch.id }) { _, channel ->
            LiveChannelRow(
                channel = channel,
                isLoading = loadingChannelId == channel.id,
                onClick = { onChannelClick(channel) },
            )
        }
    }
}

@Composable
private fun LiveChannelRow(
    channel: LiveChannel,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(80.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Sol: poster
            Box(
                modifier = Modifier
                    .width(128.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (channel.poster.isNotBlank()) {
                    AsyncImage(
                        model = channel.poster,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, MaterialTheme.colorScheme.surfaceVariant.copy(0.8f)),
                                startX = 60f,
                            ),
                        ),
                    )
                } else {
                    Box(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Sağ: kanal adı
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
            )
        }
    }
}

// ─── Yardımcılar ───────────────────────────────────────────────────────────────

@Composable
private fun LiveTvLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("Kanallar yükleniyor…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LiveTvError(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Tv,
                null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
            Spacer(Modifier.height(12.dp))
            Text("Bağlantı hatası", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Tekrar dene")
            }
        }
    }
}
