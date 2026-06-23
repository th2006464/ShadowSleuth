package com.shadowsleuth.app.ui.navigation

sealed class Screen(val route: String) {
    data object Scan : Screen("scan")
    data object Results : Screen("results")
    data object Search : Screen("search")
    data object Preview : Screen("preview/{imageId}") {
        fun createRoute(imageId: String) = "preview/$imageId"
    }
}
