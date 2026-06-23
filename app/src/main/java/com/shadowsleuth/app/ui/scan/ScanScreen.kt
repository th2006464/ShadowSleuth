package com.shadowsleuth.app.ui.scan

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shadowsleuth.app.R
import com.shadowsleuth.app.ui.navigation.Screen
import com.shadowsleuth.app.viewmodel.ScanState
import com.shadowsleuth.app.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onRequestPermission: (String) -> Unit,
    onNavigate: (Screen) -> Unit
) {
    val state by viewModel.scanState.collectAsState()
    val minSizeKb by viewModel.minSizeKb.collectAsState()
    val matchByFilename by viewModel.matchByFilename.collectAsState()
    val matchBySize by viewModel.matchBySize.collectAsState()
    val context = LocalContext.current

    val startScanAction = {
        if (ScanViewModel.hasPermission(context)) {
            viewModel.startScan()
        } else {
            onRequestPermission(ScanViewModel.getRequiredPermission())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Hero card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "智能查找重复图片",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "纯本地运行，不上传、不删除任何图片",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Matching rules
            Text(
                text = "匹配规则",
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = matchByFilename,
                    onClick = { viewModel.setMatchOptions(!matchByFilename, matchBySize) },
                    label = { Text(stringResource(R.string.match_by_filename)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                )
                FilterChip(
                    selected = matchBySize,
                    onClick = { viewModel.setMatchOptions(matchByFilename, !matchBySize) },
                    label = { Text(stringResource(R.string.match_by_size)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                )
            }

            // Small image threshold
            Text(
                text = "忽略小于 ${minSizeKb} KB 的图片",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = minSizeKb.toFloat(),
                onValueChange = { viewModel.setMinSize(it.toInt()) },
                valueRange = 0f..200f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // Action button
            when (state) {
                is ScanState.Scanning -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = (state as ScanState.Scanning).message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                is ScanState.Complete -> {
                    val complete = state as ScanState.Complete
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "找到 ${complete.groups.size} 组重复图片",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { onNavigate(Screen.Results) }) {
                            Text("查看结果")
                        }
                    }
                }
                else -> {
                    Button(
                        onClick = startScanAction,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.start_scan))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
