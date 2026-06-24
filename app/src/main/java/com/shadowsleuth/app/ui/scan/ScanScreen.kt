package com.shadowsleuth.app.ui.scan

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowsleuth.app.R
import com.shadowsleuth.app.data.ExifReader
import com.shadowsleuth.app.data.ImageScanner
import com.shadowsleuth.app.data.model.ExifInfo
import com.shadowsleuth.app.data.model.ImageMetadata
import com.shadowsleuth.app.ui.components.ExifDetailDialog
import com.shadowsleuth.app.ui.components.SsPrimaryButton
import com.shadowsleuth.app.ui.components.SsSecondaryButton
import com.shadowsleuth.app.ui.components.SsTopBar
import com.shadowsleuth.app.ui.navigation.Screen
import com.shadowsleuth.app.viewmodel.ScanState
import com.shadowsleuth.app.viewmodel.ScanViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onRequestPermission: (String) -> Unit,
    onNavigate: (Screen) -> Unit,
    onInfoClick: () -> Unit = {},
    onThemeClick: () -> Unit = {},
    onRequestManageStorage: () -> Unit = {}
) {
    val state by viewModel.scanState.collectAsState()
    val minSizeKb by viewModel.minSizeKb.collectAsState()
    val context = LocalContext.current

    var selectedExifImage by remember { mutableStateOf<ImageMetadata?>(null) }
    var exifInfo by remember { mutableStateOf<ExifInfo?>(null) }
    var showExifDialog by remember { mutableStateOf(false) }
    var pendingAutoNavigate by remember { mutableStateOf(false) }

    LaunchedEffect(state, pendingAutoNavigate) {
        if (pendingAutoNavigate && state is ScanState.Complete) {
            pendingAutoNavigate = false
            onNavigate(Screen.Results)
        }
        if (state is ScanState.Error) {
            pendingAutoNavigate = false
        }
    }

    val pickImageForExif = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val metadata = ImageScanner.uriToImageMetadata(context, it)
                    val info = ExifReader.read(context, metadata)
                    withContext(Dispatchers.Main) {
                        selectedExifImage = metadata
                        exifInfo = info
                        showExifDialog = true
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val startScanAction = {
        if (ScanViewModel.hasPermission(context)) {
            pendingAutoNavigate = true
            viewModel.startScan()
        } else {
            onRequestPermission(ScanViewModel.getRequiredPermission())
        }
    }

    val viewDetailsAction = {
        if (ScanViewModel.hasPermission(context)) {
            pickImageForExif.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            onRequestPermission(ScanViewModel.getRequiredPermission())
        }
    }

    Scaffold(
        topBar = {
            SsTopBar(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = onThemeClick) {
                        Icon(
                            imageVector = Icons.Filled.DarkMode,
                            contentDescription = stringResource(R.string.theme),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = onInfoClick) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = stringResource(R.string.about),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Hero Card — flat, no gradient
            FlatHeroCard()

            // MANAGE_EXTERNAL_STORAGE banner
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                !ScanViewModel.hasManageStoragePermission()
            ) {
                FlatManageStorageBanner(onClick = onRequestManageStorage)
            }

            // Size threshold label + slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "忽略小于 ${minSizeKb} KB 的图片",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Slider card — flat, no shadow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
                    Slider(
                        value = minSizeKb.toFloat(),
                        onValueChange = { viewModel.setMinSize(it.toInt()) },
                        valueRange = 0f..200f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "0 KB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "200 KB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action area
            when (state) {
                is ScanState.Scanning -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = (state as ScanState.Scanning).message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SsPrimaryButton(
                            text = stringResource(R.string.start_scan),
                            onClick = startScanAction,
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Filled.AutoFixHigh
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SsSecondaryButton(
                            text = "查看图片详细信息",
                            onClick = viewDetailsAction,
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Filled.Visibility
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showExifDialog && selectedExifImage != null && exifInfo != null) {
        ExifDetailDialog(
            exifInfo = exifInfo!!,
            imageUri = selectedExifImage!!.uri,
            displayName = selectedExifImage!!.displayName,
            onDismiss = {
                showExifDialog = false
                selectedExifImage = null
                exifInfo = null
            }
        )
    }
}

// ── Flat Hero Card ────────────────────────────────────────────────────────────
@Composable
private fun FlatHeroCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon block
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "智能查找重复图片",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "纯本地运行 · 不上传 · 不删除",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.80f)
                )
            }
        }
    }
}

// ── Flat Manage Storage Banner ────────────────────────────────────────────────
@Composable
private fun FlatManageStorageBanner(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.manage_storage_required),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.manage_storage_rationale),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.tertiary)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.go_to_settings),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onTertiary
                )
            }
        }
    }
}
