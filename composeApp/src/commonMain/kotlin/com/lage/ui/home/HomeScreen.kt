package com.lage.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.lage.data.model.Song
import com.lage.data.repository.SongRepository
import kotlinx.coroutines.launch

/**
 * 发现首页 — 使用 LazyColumn 内置懒加载
 *
 * 性能优化:
 *   - LazyColumn: 只组合可见 item (等价于 Flutter ListView.builder)
 *   - Coil AsyncImage: 异步加载 + 磁盘缓存 + 内存缓存
 *   - 分页加载: 滚动到底自动加载更多
 *   - 下拉刷新: 清除缓存后重新加载
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val repository = remember { SongRepository() }
    val scope = rememberCoroutineScope()

    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasMore by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("latest") }

    val listState = rememberLazyListState()

    // ── 首次加载 ──────────────────────────────────────────
    fun load(append: Boolean = false, refresh: Boolean = false) {
        scope.launch {
            isLoading = true
            repository.getSongs(
                sortBy = sortBy,
                offset = if (append) songs.size else 0,
                forceRefresh = refresh,
            ).onSuccess { newSongs ->
                songs = if (append) songs + newSongs else newSongs
                hasMore = newSongs.size >= 20
                error = null
            }.onFailure { e ->
                error = e.message
            }
            isLoading = false
        }
    }

    fun refresh() { load(refresh = true) }
    fun search(q: String) {
        searchQuery = q
        scope.launch {
            isLoading = true
            repository.searchSongs(q).onSuccess { songs = it; hasMore = false; error = null }
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    // ── 滚动分页 ──────────────────────────────────────────
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisible != null && lastVisible.index >= songs.size - 5
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && hasMore && !isLoading) {
            load(append = true)
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("搜索歌曲或艺人...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text("发现")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearching = !isSearching
                        if (!isSearching) { searchQuery = ""; refresh() }
                    }) {
                        Icon(
                            if (isSearching) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "搜索"
                        )
                    }
                    if (!isSearching) {
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.Sort, "排序")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("latest" to "最新", "popular" to "最受欢迎", "analyzed" to "最多分析").forEach { (k, v) ->
                                DropdownMenuItem(text = { Text(v) }, onClick = {
                                    expanded = false; sortBy = k; refresh()
                                })
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            isLoading && songs.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            error != null && songs.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("加载失败", style = MaterialTheme.typography.titleMedium)
                    Text(error ?: "", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { refresh() }) { Text("重试") }
                }
            }

            songs.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center
            ) {
                Text("暂无歌曲", style = MaterialTheme.typography.titleMedium)
            }

            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                items(songs, key = { it.id }) { song ->
                    SongCard(song = song, onClick = { onSongClick(song.id) })
                }
                if (hasMore && !isLoading) {
                    item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    } }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// 歌曲卡片 — 使用 Coil AsyncImage 异步加载封面
// ═══════════════════════════════════════════════════════════

@Composable
fun SongCard(song: Song, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 封面 — Coil 异步加载，内置磁盘 + 内存缓存
            AsyncImage(
                model = song.coverUrl,
                contentDescription = song.title,
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
            )

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium)
                Text(song.artist, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatsChip(Icons.Default.Analytics, "${song.analysisCount}")
                    StatsChip(Icons.Default.FavoriteBorder, "${song.likeCount}")
                    song.bpm?.let { StatsChip(Icons.Default.Speed, "${it.toInt()} BPM") }
                }
            }

            FilledTonalIconButton(onClick = onClick) {
                Icon(Icons.Default.PlayArrow, "播放")
            }
        }
    }
}

@Composable
fun StatsChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(2.dp))
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
