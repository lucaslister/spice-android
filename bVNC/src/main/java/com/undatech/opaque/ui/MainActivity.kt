package com.undatech.opaque.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.undatech.opaque.ui.data.AppPreferences
import com.undatech.opaque.ui.servers.ServerFormScreen
import com.undatech.opaque.ui.servers.ServerListScreen
import com.undatech.opaque.ui.settings.SettingsScreen
import com.undatech.opaque.ui.theme.SpiceTheme
import com.undatech.opaque.ui.theme.ThemeMode
import com.undatech.opaque.ui.vms.VmBrowserScreen

/**
 * Compose entry point for the redesigned, Proxmox-first SPICE client. Hosts the navigation
 * graph: server list (home) -> add/edit server / VM browser -> (native) SPICE session.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = AppPreferences(this)

        setContent {
            var themeMode by remember { mutableStateOf(prefs.themeMode) }
            var dynamicColor by remember { mutableStateOf(prefs.dynamicColor) }

            SpiceTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = Routes.SERVERS) {
                    composable(Routes.SERVERS) {
                        ServerListScreen(
                            onAddServer = { navController.navigate(Routes.serverForm()) },
                            onEditServer = { id -> navController.navigate(Routes.serverForm(id)) },
                            onOpenServer = { id -> navController.navigate(Routes.vmBrowser(id)) },
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        )
                    }
                    composable(
                        route = Routes.SERVER_FORM,
                        arguments = listOf(navArgument(Routes.ARG_SERVER_ID) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }),
                    ) { backStackEntry ->
                        ServerFormScreen(
                            serverId = backStackEntry.arguments?.getString(Routes.ARG_SERVER_ID),
                            onBack = { navController.popBackStack() },
                            onSaved = { navController.popBackStack() },
                        )
                    }
                    composable(
                        route = Routes.VM_BROWSER,
                        arguments = listOf(navArgument(Routes.ARG_SERVER_ID) {
                            type = NavType.StringType
                        }),
                    ) { backStackEntry ->
                        VmBrowserScreen(
                            serverId = backStackEntry.arguments?.getString(Routes.ARG_SERVER_ID).orEmpty(),
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            themeMode = themeMode,
                            dynamicColor = dynamicColor,
                            onThemeMode = {
                                themeMode = it
                                prefs.themeMode = it
                            },
                            onDynamicColor = {
                                dynamicColor = it
                                prefs.dynamicColor = it
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
