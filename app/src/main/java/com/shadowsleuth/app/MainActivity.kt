package com.shadowsleuth.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.shadowsleuth.app.viewmodel.ScanViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: ScanViewModel by viewModels()

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[ScanViewModel.getRequiredPermission()] ?: false
        if (readGranted) {
            viewModel.startScan()
        }
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
            ShadowSleuthTheme {
                val navController = rememberNavController()
                MainApp(navController, viewModel) { permission ->
                    permissionsLauncher.launch(arrayOf(permission))
                }
            }
        }
    }
}

@Composable
private fun MainApp(
    navController: NavHostController,
    viewModel: ScanViewModel,
    onRequestPermission: (String) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val bottomBarItems = listOf(Screen.Scan, Screen.Results, Screen.Search)
    val showBottomBar = currentRoute in bottomBarItems.map { it.route }

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
                    onNavigate = { navController.navigate(it.route) }
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
}
