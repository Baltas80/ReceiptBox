package com.pagrey.receiptbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.pagrey.receiptbox.ui.*
import com.pagrey.receiptbox.ui.theme.ReceiptBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReceiptBoxApp() }
    }
}

@Composable
private fun ReceiptBoxApp(viewModel: ReceiptBoxViewModel = viewModel()) {
    val receipts by viewModel.receipts.collectAsStateWithLifecycle(initialValue = emptyList())
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("receiptbox_settings", android.content.Context.MODE_PRIVATE) }
    var query by remember { mutableStateOf("") }
    var darkTheme by remember { mutableStateOf(preferences.getBoolean("dark_theme", false)) }

    ReceiptBoxTheme(darkTheme = darkTheme) {
        val navigate: (String) -> Unit = { route -> navController.navigate(route) { launchSingleTop = true } }
        val goHome = { navController.navigate("home") { popUpTo("home") { inclusive = true }; launchSingleTop = true } }
        val route = currentBackStackEntry?.destination?.route ?: "home"
        val showBottomBar = route == "home" || route == "tickets" || route == "stats" || route == "settings"

        Scaffold(bottomBar = { if (showBottomBar) BottomNav(route, navigate) }) { paddingValues ->
            Box(Modifier.padding(paddingValues)) {
                NavHost(navController, startDestination = "home") {
                    composable("home") { HomeScreen(receipts, { navigate("add") }, { id -> navigate("detail/$id") }, { navigate("tickets") }) }
                    composable("tickets") { ReceiptListScreen(receipts, query, { query = it }, { id -> navigate("detail/$id") }) }
                    composable("add") { DocumentScannerReceiptScreen(viewModel, { id -> navController.navigate("detail/$id") { popUpTo("home") } }, goHome) }
                    composable("stats") { StatisticsScreen(receipts) { goHome() } }
                    composable("settings") {
                        SettingsScreen(receipts, darkTheme, {
                            darkTheme = it
                            preferences.edit().putBoolean("dark_theme", it).apply()
                        }, { restored -> viewModel.restore(restored) }) { goHome() }
                    }
                    composable("detail/{id}") { entry ->
                        val id = entry.arguments?.getString("id")?.toLongOrNull()
                        val selected = receipts.firstOrNull { it.id == id }
                        ReceiptDetailScreen(selected, { navController.popBackStack() }, viewModel::delete, viewModel::save)
                    }
                }
            }
        }
    }
}
