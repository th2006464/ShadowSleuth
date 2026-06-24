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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
    ) { _ ->
        // 用户授权后不会自动扫描，需点击主页「开始扫描」按钮手动触发
    }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // 用户从设置返回后，MainApp 会重新检查并隐藏提示
    }

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
                    onRequestManageStorage = { openManageStorageSettings() }
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

@Composable
private fun MainApp(
    navController: NavHostController,
    viewModel: ScanViewModel,
    themeViewModel: ThemeViewModel,
    onRequestPermission: (String) -> Unit,
    onRequestManageStorage: () -> Unit
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
                NavigationBar {
                    bottomBarItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        val label = when (screen) {
                            Screen.Scan -> stringResource(R.string.scan)
                            Screen.Results -> stringResource(R.string.results)
                            Screen.Search -> stringResource(R.string.search)
                            else -> ""
                        }
                        val icon = when (screen) {
                            Screen.Scan -> Icons.Filled.PhotoLibrary
                            Screen.Results -> Icons.Filled.Storage
                            Screen.Search -> Icons.Filled.Search
                            else -> Icons.Filled.PhotoLibrary
                        }
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = selected,
                            onClick = {
                                when {
                                    selected && screen == Screen.Results -> {
                                        viewModel.scrollToTopResults()
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
                            }
                        )
                    }
                }
            }
        }
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
                    onRequestManageStorage = onRequestManageStorage
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
                    Text(
                        text = "图片未找到",
                        modifier = Modifier.padding(innerPadding),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }

    if (showInfoDialog) {
        InfoDialog(
            viewModel = viewModel,
            onDismiss = { showInfoDialog = false }
        )
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            currentMode = themeViewModel.themeMode.collectAsState().value,
            onModeSelected = { mode ->
                themeViewModel.setThemeMode(mode)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
private fun InfoDialog(viewModel: ScanViewModel, onDismiss: () -> Unit) {
    val dHashCacheSize by viewModel.dHashCacheSize.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("关于双影密探") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "双影密探（ShadowSleuth）是一款纯本地、离线、无网络上传的 Android 重复图片查找工具。",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                // dHash 缓存信息卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Memory,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "dHash 缓存",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (dHashCacheSize == 0)
                                "暂无缓存（首次执行 dHash 扫描后会生成）"
                            else
                                "已缓存 $dHashCacheSize 张图片的哈希值（内存中，重启后自动清空）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (dHashCacheSize > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showClearConfirm = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("清空 dHash 缓存", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "项目发布与更新：",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "https://github.com/th2006464/ShadowSleuth",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "联系我们：",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "微信：Fox_Tang",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "版权与声明：",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "· 本应用不会上传、分析或删除您的图片，所有操作均在本地完成。\n" +
                            "· 应用图标、界面及代码均受版权保护，未经授权禁止转载或商用。\n" +
                            "· 使用本应用即表示您同意自行承担操作风险，建议删除前再次确认。\n" +
                            "· 如有侵权或违规内容，请联系我们处理。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.got_it))
            }
        }
    )

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("清空 dHash 缓存") },
            text = { Text("将清除内存中已缓存的 $dHashCacheSize 条哈希记录。下次使用 dHash 功能时会重新计算。确定继续？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearDHashCache()
                    showClearConfirm = false
                }) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ThemePickerDialog(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("主题模式") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                ThemeOption(
                    label = stringResource(R.string.theme_light),
                    selected = currentMode == ThemeMode.LIGHT,
                    onClick = { onModeSelected(ThemeMode.LIGHT) }
                )
                ThemeOption(
                    label = stringResource(R.string.theme_dark),
                    selected = currentMode == ThemeMode.DARK,
                    onClick = { onModeSelected(ThemeMode.DARK) }
                )
                ThemeOption(
                    label = stringResource(R.string.theme_system),
                    selected = currentMode == ThemeMode.SYSTEM,
                    onClick = { onModeSelected(ThemeMode.SYSTEM) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (selected) "✓ $label" else label,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
