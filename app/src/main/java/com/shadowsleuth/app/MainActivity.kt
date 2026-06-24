package com.shadowsleuth.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shadowsleuth.app.ui.components.SsDialog
import com.shadowsleuth.app.ui.components.SsGhostButton
import com.shadowsleuth.app.ui.navigation.Screen
import com.shadowsleuth.app.ui.preview.PreviewScreen
import com.shadowsleuth.app.ui.results.ResultsScreen
import com.shadowsleuth.app.ui.scan.ScanScreen
import com.shadowsleuth.app.ui.search.SearchScreen
import com.shadowsleuth.app.ui.theme.ShadowSleuthTheme
import com.shadowsleuth.app.ui.theme.ThemeMode
import com.shadowsleuth.app.ui.theme.ThemeViewModel
import com.shadowsleuth.app.viewmodel.ScanViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: ScanViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> }

    private val deletePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.retryPendingDelete()
        } else {
            viewModel.clearPendingDelete()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val missingPermissions = ScanViewModel.getAllRequiredPermissions()
            .filter { checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
            .toTypedArray()

        if (missingPermissions.isNotEmpty()) {
            permissionsLauncher.launch(missingPermissions)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pendingDeleteRequest.collect { intentSender ->
                    val request = androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                    deletePermissionLauncher.launch(request)
                }
            }
        }

        setContent {
            val themeMode by themeViewModel.themeMode.collectAsState()
            ShadowSleuthTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                MainApp(
                    navController = navController,
                    viewModel = viewModel,
                    themeViewModel = themeViewModel,
                    onRequestPermission = { permission ->
                        permissionsLauncher.launch(arrayOf(permission))
                    },
                )
            }
        }
    }

    private fun openManageStorageSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        }
        manageStorageLauncher.launch(intent)
    }
}

// ── Main App ──────────────────────────────────────────────────────────────────
@Composable
private fun MainApp(
    navController: NavHostController,
    viewModel: ScanViewModel,
    themeViewModel: ThemeViewModel,
    onRequestPermission: (String) -> Unit,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val bottomBarItems = listOf(Screen.Scan, Screen.Results, Screen.Search)
    val showBottomBar = currentRoute in bottomBarItems.map { it.route }

    var showInfoDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(MaterialTheme.colorScheme.background),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomBarItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        val label = when (screen) {
                            Screen.Scan -> stringResource(R.string.scan)
                            Screen.Results -> stringResource(R.string.results)
                            Screen.Search -> stringResource(R.string.search)
                            else -> ""
                        }
                        val iconVec = when (screen) {
                            Screen.Scan -> Icons.Filled.PhotoLibrary
                            Screen.Results -> Icons.Filled.Storage
                            Screen.Search -> Icons.Filled.Search
                            else -> Icons.Filled.PhotoLibrary
                        }

                        val interactionSource = remember { MutableInteractionSource() }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = rememberRipple()
                                ) {
                                    when {
                                        selected && screen == Screen.Results -> {
                                            viewModel.scrollToTopResults()
                                        }
                                        !selected && screen == Screen.Scan -> {
                                            navController.popBackStack(Screen.Scan.route, inclusive = false)
                                        }
                                        !selected -> {
                                            navController.navigate(screen.route) {
                                                popUpTo(Screen.Scan.route) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconVec,
                                        contentDescription = label,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = iconVec,
                                    contentDescription = label,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Scan.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Scan.route) {
                ScanScreen(
                    viewModel = viewModel,
                    onRequestPermission = onRequestPermission,
                    onNavigate = { navController.navigate(it.route) },
                    onInfoClick = { showInfoDialog = true },
                    onThemeClick = { showThemeDialog = true },
                )
            }
            composable(Screen.Results.route) {
                ResultsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onImageClick = { image ->
                        navController.navigate(Screen.Preview.createRoute(image.id.toString()))
                    }
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onImageClick = { image ->
                        navController.navigate(Screen.Preview.createRoute(image.id.toString()))
                    }
                )
            }
            composable(Screen.Preview.route) { backStackEntry ->
                val imageId = backStackEntry.arguments?.getString("imageId")
                val image = imageId?.toLongOrNull()?.let { viewModel.findImageById(it) }
                if (image != null) {
                    PreviewScreen(image = image, onBack = { navController.popBackStack() })
                } else {
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Text(
                            text = "图片未找到",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showInfoDialog) {
        FlatInfoDialog(
            viewModel = viewModel,
            onDismiss = { showInfoDialog = false },
        )
    }

    if (showThemeDialog) {
        FlatThemePickerDialog(
            currentMode = themeViewModel.themeMode.collectAsState().value,
            onModeSelected = { mode ->
                themeViewModel.setThemeMode(mode)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}

// ── Flat Info Dialog (replaces system AlertDialog) ────────────────────────────
@Composable
private fun FlatInfoDialog(
    viewModel: ScanViewModel,
    onDismiss: () -> Unit
) {
    val dHashCacheSize by viewModel.dHashCacheSize.collectAsState()
    val dHashCacheBytes by viewModel.dHashCacheBytes.collectAsState()
    val dHashCacheCreatedAt by viewModel.dHashCacheCreatedAt.collectAsState()
    val deletePermissionEnabled by viewModel.deletePermissionEnabled.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }

    val cacheSizeText = when {
        dHashCacheBytes >= 1024 * 1024 -> String.format("%.1f MB", dHashCacheBytes / (1024.0 * 1024.0))
        dHashCacheBytes >= 1024 -> String.format("%.1f KB", dHashCacheBytes / 1024.0)
        else -> "${dHashCacheBytes} B"
    }

    val cacheTimeText = if (dHashCacheCreatedAt > 0) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        sdf.format(java.util.Date(dHashCacheCreatedAt))
    } else ""

    SsDialog(
        onDismiss = onDismiss,
        title = "关于双影密探",
        dismissText = stringResource(R.string.got_it)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "双影密探（ShadowSleuth）是一款纯本地、离线、无网络上传的 Android 重复图片查找工具。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "版本：1.3.2",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )


            Spacer(modifier = Modifier.height(16.dp))

            // dHash cache card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Memory,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "dHash 缓存",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (dHashCacheSize == 0) {
                        Text(
                            text = "暂无缓存（首次执行 dHash 扫描后会生成）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "已缓存 $dHashCacheSize 张图片的哈希值（内存中，重启后自动清空）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "占用内存约 $cacheSizeText · 创建于 $cacheTimeText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (dHashCacheSize > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        SsGhostButton(
                            text = "清空 dHash 缓存",
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Filled.DeleteSweep,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 删除权限卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.delete_permission),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.delete_permission_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (deletePermissionEnabled) stringResource(R.string.delete_permission_on_hint) else stringResource(R.string.delete_permission_off_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = deletePermissionEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setDeletePermissionEnabled(enabled)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle("项目发布与更新")
            Text(
                text = "https://github.com/th2006464/ShadowSleuth",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            SectionTitle("联系我们")
            Text(
                text = "微信：Fox_Tang",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            SectionTitle("版权与声明")
            Text(
                text = "· 本应用不会上传、分析或删除您的图片，所有操作均在本地完成。\n" +
                        "· 应用图标、界面及代码均受版权保护，未经授权禁止转载或商用。\n" +
                        "· 使用本应用即表示您同意自行承担操作风险，建议删除前再次确认。\n" +
                        "· 如有侵权或违规内容，请联系我们处理。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Clear cache confirmation
    if (showClearConfirm) {
        SsDialog(
            onDismiss = { showClearConfirm = false },
            title = "清空 dHash 缓存",
            confirmText = "清空",
            onConfirm = {
                viewModel.clearDHashCache()
                showClearConfirm = false
            },
            confirmColor = MaterialTheme.colorScheme.error,
            dismissText = "取消"
        ) {
            Text(
                text = "将清除内存中已缓存的 $dHashCacheSize 条哈希记录。下次使用 dHash 功能时会重新计算。确定继续？",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Flat Theme Picker Dialog (replaces system AlertDialog) ────────────────────
@Composable
private fun FlatThemePickerDialog(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    SsDialog(
        onDismiss = onDismiss,
        title = "主题模式",
        dismissText = stringResource(R.string.cancel)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FlatThemeOption(
                label = stringResource(R.string.theme_light),
                selected = currentMode == ThemeMode.LIGHT,
                onClick = { onModeSelected(ThemeMode.LIGHT) }
            )
            FlatThemeOption(
                label = stringResource(R.string.theme_dark),
                selected = currentMode == ThemeMode.DARK,
                onClick = { onModeSelected(ThemeMode.DARK) }
            )
            FlatThemeOption(
                label = stringResource(R.string.theme_system),
                selected = currentMode == ThemeMode.SYSTEM,
                onClick = { onModeSelected(ThemeMode.SYSTEM) }
            )
        }
    }
}

@Composable
private fun FlatThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surface
            )
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple()
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(4.dp))
}
