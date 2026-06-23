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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import com.shadowsleuth.app.R
import com.shadowsleuth.app.data.model.DuplicateGroup
import com.shadowsleuth.app.data.model.ImageMetadata
import com.shadowsleuth.app.ui.components.DeleteConfirmDialog
import com.shadowsleuth.app.ui.components.DuplicateGroupCard
import com.shadowsleuth.app.ui.components.ImageActionBottomSheet
import com.shadowsleuth.app.ui.components.ImageDetailDialog
import com.shadowsleuth.app.viewmodel.ScanState
import com.shadowsleuth.app.viewmodel.ScanViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    viewModel: ScanViewModel,
    onBack: () -> Unit,
    onImageClick: (ImageMetadata) -> Unit
) {
    val state = viewModel.scanState.collectAsState().value
    val allGroups = if (state is ScanState.Complete) state.groups else emptyList()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var filterFilename by remember { mutableStateOf(true) }
    var filterSize by remember { mutableStateOf(true) }
    var filterDimensions by remember { mutableStateOf(true) }

    val groups by remember(allGroups, filterFilename, filterSize, filterDimensions) {
        derivedStateOf {
            allGroups.filter { group ->
                when (group.matchType) {
                    DuplicateGroup.MatchType.FILENAME -> filterFilename
                    DuplicateGroup.MatchType.SIZE -> filterSize
                    DuplicateGroup.MatchType.DIMENSIONS -> filterDimensions
                }
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
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.results),
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .clickable {
                                scope.launch {
                                    listState.animateScrollToItem(0)
                                }
                            }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (showScrollToTop) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerticalAlignTop,
                        contentDescription = "回到顶部"
                    )
                }
            }
        }
    ) { paddingValues ->
        if (allGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.no_duplicates),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "请先返回扫描页执行扫描",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "共 ${groups.size} 组重复图片",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FilterChipsRow(
                        filterFilename = filterFilename,
                        filterSize = filterSize,
                        filterDimensions = filterDimensions,
                        onFilterFilename = { filterFilename = it },
                        onFilterSize = { filterSize = it },
                        onFilterDimensions = { filterDimensions = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                        scope.launch {
                            snackbarHostState.showSnackbar("图片已删除")
                        }
                    },
                    onError = { message ->
                        selectedImage = null
                        scope.launch {
                            snackbarHostState.showSnackbar(message)
                        }
                    }
                )
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipsRow(
    filterFilename: Boolean,
    filterSize: Boolean,
    filterDimensions: Boolean,
    onFilterFilename: (Boolean) -> Unit,
    onFilterSize: (Boolean) -> Unit,
    onFilterDimensions: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MatchFilterChip(
            selected = filterFilename,
            onSelectedChange = onFilterFilename,
            label = stringResource(R.string.filename_match),
            modifier = Modifier.weight(1f)
        )
        MatchFilterChip(
            selected = filterSize,
            onSelectedChange = onFilterSize,
            label = stringResource(R.string.size_match),
            modifier = Modifier.weight(1f)
        )
        MatchFilterChip(
            selected = filterDimensions,
            onSelectedChange = onFilterDimensions,
            label = stringResource(R.string.dimensions_match),
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchFilterChip(
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = { onSelectedChange(!selected) },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}
