package com.eternalvpn.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.graphics.vector.ImageVector
import com.eternalvpn.app.R

sealed class AppScreen(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    data object Home : AppScreen("home", R.string.nav_home, Icons.Filled.Home)
    data object Nodes : AppScreen("nodes", R.string.nav_nodes, Icons.Filled.Storage)
    data object Scanner : AppScreen("scanner", R.string.nav_scanner, Icons.Filled.Radar)
    data object Routing : AppScreen("routing", R.string.nav_routing, Icons.Filled.Dns)
    data object Settings : AppScreen("settings", R.string.nav_settings, Icons.Filled.Settings)

    companion object {
        val bottomItems = listOf(Home, Nodes, Scanner, Routing, Settings)
    }
}
