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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shadowsleuth.app.R
import com.shadowsleuth.app.data.model.ImageMetadata
import com.shadowsleuth.app.ui.components.DuplicateGroupCard
import com.shadowsleuth.app.ui.components.ThumbnailImage
import com.shadowsleuth.app.viewmodel.ScanViewModel
import com.shadowsleuth.app.viewmodel.SearchState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: ScanViewModel,
    onBack: () -> Unit,
    onImageClick: (ImageMetadata) -> Unit
) {
    val state = viewModel.searchState.collectAsState().value
    val context = LocalContext.current

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val metadata = uriToImageMetadata(context, it)
            viewModel.searchSample(metadata)
        }
    }

    Scaffold(
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
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
                                    onImageClick = onImageClick
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

private fun uriToImageMetadata(context: android.content.Context, uri: Uri): ImageMetadata {
    var path = uri.toString()
    var displayName = uri.lastPathSegment ?: "unknown"
    var size = 0L
    var width = 0
    var height = 0
    var mimeType = "image/*"

    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val displayNameIdx = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.SIZE)
            val widthIdx = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.WIDTH)
            val heightIdx = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.HEIGHT)
            val mimeIdx = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.MIME_TYPE)
            val dataIdx = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.DATA)

            if (displayNameIdx >= 0) displayName = cursor.getString(displayNameIdx) ?: displayName
            if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            if (widthIdx >= 0) width = cursor.getInt(widthIdx)
            if (heightIdx >= 0) height = cursor.getInt(heightIdx)
            if (mimeIdx >= 0) mimeType = cursor.getString(mimeIdx) ?: mimeType
            if (dataIdx >= 0) path = cursor.getString(dataIdx) ?: path
        }
    }

    return ImageMetadata(
        id = System.currentTimeMillis(),
        uri = uri,
        path = path,
        displayName = displayName,
        sizeBytes = size,
        dateAdded = System.currentTimeMillis(),
        width = width,
        height = height,
        mimeType = mimeType
    )
}
