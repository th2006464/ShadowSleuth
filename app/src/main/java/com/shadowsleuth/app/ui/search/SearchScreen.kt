package com.shadowsleuth.app.ui.search

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shadowsleuth.app.R
import com.shadowsleuth.app.data.ImageScanner
import com.shadowsleuth.app.data.model.ImageMetadata
import com.shadowsleuth.app.ui.components.DeleteConfirmDialog
import com.shadowsleuth.app.ui.components.DuplicateGroupCard
import com.shadowsleuth.app.ui.components.ImageActionBottomSheet
import com.shadowsleuth.app.ui.components.ImageDetailDialog
import com.shadowsleuth.app.ui.components.ThumbnailImage
import com.shadowsleuth.app.viewmodel.ScanViewModel
import com.shadowsleuth.app.viewmodel.SearchState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
            TopAppBar(
                title = { Text(stringResource(R.string.search)) },
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    if (ScanViewModel.hasPermission(context)) {
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    } else {
                        viewModel.setSearchError("需要相册权限才能搜索图片，请授予权限后重试")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.AddPhotoAlternate,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.select_image))
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (state) {
                is SearchState.NoSample -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "选择一张图片，搜索手机里与它重复或相似的图片",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is SearchState.Ready -> {
                    val sample = state.sample
                    val groups = state.groups
                    if (groups.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "未找到与该图片重复或相似的图片",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "样本图片",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ThumbnailImage(image = sample, size = 160)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.search_matches),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        }
                    }
                }
                is SearchState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "搜索失败",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
