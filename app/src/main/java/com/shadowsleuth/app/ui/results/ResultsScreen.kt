package com.shadowsleuth.app.ui.results

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shadowsleuth.app.R
import com.shadowsleuth.app.data.model.DuplicateGroup
import com.shadowsleuth.app.data.model.ImageMetadata
import com.shadowsleuth.app.ui.components.DeleteConfirmDialog
import com.shadowsleuth.app.ui.components.DuplicateGroupCard
import com.shadowsleuth.app.ui.components.ImageActionBottomSheet
import com.shadowsleuth.app.ui.components.ImageDetailDialog
import com.shadowsleuth.app.ui.components.SsEmptyState
import com.shadowsleuth.app.ui.components.SsFab
import com.shadowsleuth.app.ui.components.SsFilterChipRow
import com.shadowsleuth.app.ui.components.SsPrimaryButton
import com.shadowsleuth.app.ui.components.SsSecondaryButton
import com.shadowsleuth.app.ui.components.SsTopBar
import com.shadowsleuth.app.viewmodel.DHashScanState
import com.shadowsleuth.app.viewmodel.ScanState
import com.shadowsleuth.app.viewmodel.ScanViewModel
import kotlinx.coroutines.launch

private enum class ResultFilter {
    ALL, FILENAME, SIZE, DHASH
}

@Composable
fun ResultsScreen(
    viewModel: ScanViewModel,
    onBack: () -> Unit,
    onImageClick: (ImageMetadata) -> Unit
) {
    val state = viewModel.scanState.collectAsState().value
    val dHashScanState = viewModel.dHashScanState.collectAsState().value
    val allGroups = if (state is ScanState.Complete) state.groups else emptyList()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var currentFilter by remember { mutableStateOf(ResultFilter.ALL) }

    val filterItems = listOf(
        ResultFilter.ALL to "全部",
        ResultFilter.FILENAME to stringResource(R.string.filename_match),
        ResultFilter.SIZE to stringResource(R.string.size_match),
        ResultFilter.DHASH to stringResource(R.string.dhash_similar)
    )

    val groups by remember(allGroups, currentFilter) {
        derivedStateOf {
            when (currentFilter) {
                ResultFilter.ALL -> allGroups
                ResultFilter.FILENAME -> allGroups.filter { it.matchType == DuplicateGroup.MatchType.FILENAME }
                ResultFilter.SIZE -> allGroups.filter { it.matchType == DuplicateGroup.MatchType.SIZE }
                ResultFilter.DHASH -> allGroups.filter { it.matchType == DuplicateGroup.MatchType.DHASH }
            }
        }
    }

    val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }

    LaunchedEffect(Unit) {
        viewModel.scrollToTopResults.collect {
            if (allGroups.isNotEmpty()) {
                listState.animateScrollToItem(0)
            }
        }
    }

    var selectedImage by remember { mutableStateOf<ImageMetadata?>(null) }
    var showActionSheet by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SsTopBar(
                title = stringResource(R.string.results),
                onBack = onBack
            )
        },
        floatingActionButton = {
            if (showScrollToTop) {
                SsFab(
                    icon = Icons.Filled.VerticalAlignTop,
                    contentDescription = "回到顶部",
                    onClick = {
                        scope.launch { listState.animateScrollToItem(0) }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (allGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                SsEmptyState(
                    icon = Icons.Filled.ContentCopy,
                    title = stringResource(R.string.no_duplicates),
                    subtitle = "请先返回扫描页执行扫描"
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Summary row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "共 ${groups.size} 组重复图片",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    scope.launch { listState.animateScrollToItem(0) }
                                }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    // dHash scan button
                    FlatDHashScanButton(
                        dHashScanState = dHashScanState,
                        onStartDHashScan = { viewModel.startDHashScan() }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    // Filter chips
                    SsFilterChipRow(
                        items = filterItems,
                        selected = currentFilter,
                        onSelect = { currentFilter = it }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                items(groups, key = { it.id }) { group ->
                    DuplicateGroupCard(
                        group = group,
                        onImageClick = onImageClick,
                        onImageLongClick = { image ->
                            selectedImage = image
                            showActionSheet = true
                        }
                    )
                }
            }
        }
    }

    if (showActionSheet && selectedImage != null) {
        ImageActionBottomSheet(
            image = selectedImage!!,
            onViewDetails = {
                showActionSheet = false
                showDetailDialog = true
            },
            onDelete = {
                showActionSheet = false
                showDeleteDialog = true
            },
            onDismiss = { showActionSheet = false }
        )
    }

    if (showDetailDialog && selectedImage != null) {
        ImageDetailDialog(
            image = selectedImage!!,
            onDismiss = { showDetailDialog = false }
        )
    }

    if (showDeleteDialog && selectedImage != null) {
        DeleteConfirmDialog(
            image = selectedImage!!,
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteImage(
                    selectedImage!!,
                    onSuccess = {
                        selectedImage = null
                        scope.launch { snackbarHostState.showSnackbar("图片已删除") }
                    },
                    onError = { message ->
                        selectedImage = null
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    }
                )
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun FlatDHashScanButton(
    dHashScanState: DHashScanState,
    onStartDHashScan: () -> Unit
) {
    when (dHashScanState) {
        is DHashScanState.Idle -> {
            SsSecondaryButton(
                text = stringResource(R.string.dhash_scan),
                onClick = onStartDHashScan,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.AutoAwesome
            )
        }
        is DHashScanState.Computing -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (dHashScanState.total > 0)
                        "正在计算 dHash… ${dHashScanState.computed}/${dHashScanState.total}"
                    else "正在计算 dHash…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        is DHashScanState.Complete -> {
            Text(
                text = "✓ dHash 扫描完成，发现 ${dHashScanState.groups.size} 组相似图片",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
        is DHashScanState.Error -> {
            Text(
                text = "✕ dHash 扫描失败：${dHashScanState.message}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}
