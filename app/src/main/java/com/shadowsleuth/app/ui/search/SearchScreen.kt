package com.shadowsleuth.app.ui.search

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowsleuth.app.R
import com.shadowsleuth.app.data.ImageScanner
import com.shadowsleuth.app.data.model.ImageMetadata
import com.shadowsleuth.app.ui.components.DeleteConfirmDialog
import com.shadowsleuth.app.ui.components.DuplicateGroupCard
import com.shadowsleuth.app.ui.components.ImageActionBottomSheet
import com.shadowsleuth.app.ui.components.ImageDetailDialog
import com.shadowsleuth.app.ui.components.SsEmptyState
import com.shadowsleuth.app.ui.components.SsPrimaryButton
import com.shadowsleuth.app.ui.components.SsSecondaryButton
import com.shadowsleuth.app.ui.components.SsTopBar
import com.shadowsleuth.app.ui.components.ThumbnailImage
import com.shadowsleuth.app.viewmodel.ScanViewModel
import com.shadowsleuth.app.viewmodel.SearchState
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    viewModel: ScanViewModel,
    onBack: () -> Unit,
    onImageClick: (ImageMetadata) -> Unit
) {
    val state = viewModel.searchState.collectAsState().value
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedImage by remember { mutableStateOf<ImageMetadata?>(null) }
    var showActionSheet by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDHashSearching by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val metadata = ImageScanner.uriToImageMetadata(context, it)
                viewModel.searchSample(metadata)
            } catch (e: Exception) {
                viewModel.setSearchError(e.message ?: "无法读取所选图片")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SsTopBar(
                title = stringResource(R.string.search),
                onBack = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Select image button
            SsPrimaryButton(
                text = stringResource(R.string.select_image),
                onClick = {
                    if (ScanViewModel.hasPermission(context)) {
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    } else {
                        viewModel.setSearchError("需要相册权限才能搜索图片，请授予权限后重试")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.AddPhotoAlternate
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (state) {
                is SearchState.NoSample -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        SsEmptyState(
                            icon = Icons.Filled.ImageSearch,
                            title = "选择一张图片",
                            subtitle = "搜索手机里与它重复或 dHash 相似的图片"
                        )
                    }
                }
                is SearchState.Ready -> {
                    val sample = state.sample
                    val groups = state.groups
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Sample image with label
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "样本图片",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ThumbnailImage(image = sample, size = 88)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sample.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // dHash similar search button
                        val hasDHashGroup = groups.any {
                            it.matchType == com.shadowsleuth.app.data.model.DuplicateGroup.MatchType.DHASH
                        }
                        if (!hasDHashGroup) {
                            if (isDHashSearching) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "正在进行 dHash 相似搜索…",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            } else {
                                SsSecondaryButton(
                                    text = stringResource(R.string.dhash_similar_search),
                                    onClick = {
                                        isDHashSearching = true
                                        viewModel.searchSampleByDHash(sample)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    icon = Icons.Filled.AutoAwesome
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        if (groups.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "未找到与该图片重复或相似的图片",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.search_matches),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Reset loading state once dHash results return
                            if (hasDHashGroup) {
                                isDHashSearching = false
                            }
                            val searchListState = rememberLazyListState()
                            val scrollProgress = if (searchListState.layoutInfo.totalItemsCount > 0 &&
                                searchListState.layoutInfo.visibleItemsInfo.isNotEmpty()
                            ) {
                                val total = searchListState.layoutInfo.totalItemsCount
                                val first = searchListState.firstVisibleItemIndex
                                val visible = searchListState.layoutInfo.visibleItemsInfo.size
                                if (total <= visible) 0f
                                else (first.toFloat() / (total - visible)).coerceIn(0f, 1f)
                            } else 0f

                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = searchListState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
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
                                // Scrollbar indicator
                                if (scrollProgress > 0f) {
                                    Canvas(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight()
                                            .padding(vertical = 4.dp, horizontal = 2.dp)
                                            .width(5.dp)
                                    ) {
                                        val barHeight = size.height * 0.25f
                                        val barY = (size.height - barHeight) * scrollProgress
                                        drawRoundRect(
                                            color = Color.Gray.copy(alpha = 0.35f),
                                            topLeft = androidx.compose.ui.geometry.Offset(0f, barY),
                                            size = androidx.compose.ui.geometry.Size(size.width, barHeight),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is SearchState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "!",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "搜索失败",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
                else -> {}
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
