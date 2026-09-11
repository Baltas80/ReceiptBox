package com.pagrey.receiptbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pagrey.receiptbox.ui.AddReceiptScreen
import com.pagrey.receiptbox.ui.BottomNav
import com.pagrey.receiptbox.ui.HomeScreen
import com.pagrey.receiptbox.ui.ReceiptBoxViewModel
import com.pagrey.receiptbox.ui.ReceiptDetailScreen
import com.pagrey.receiptbox.ui.ReceiptListScreen
import com.pagrey.receiptbox.ui.theme.ReceiptBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReceiptBoxTheme { ReceiptBoxApp() } }
    }
}

@androidx.compose.runtime.Composable
private fun ReceiptBoxApp(viewModel: ReceiptBoxViewModel = viewModel()) {
    val receipts by viewModel.receipts.collectAsStateWithLifecycle(initialValue = emptyList())
    val navController = rememberNavController()
    var query by remember { mutableStateOf("") }
    val navigate: (String) -> Unit = { route -> navController.navigate(route) { launchSingleTop = true } }
    val goHome = { navController.navigate("home") { popUpTo("home") { inclusive = true }; launchSingleTop = true } }

    Scaffold(bottomBar = {
        val route = navController.currentBackStackEntry?.destination?.route ?: "home"
        if (route == "home" || route == "tickets") BottomNav(route, navigate)
    }) { padding ->
        Box(androidx.compose.ui.Modifier.padding(padding)) {
            NavHost(navController, startDestination = "home") {
                composable("home") { HomeScreen(receipts, { navigate("add") }, { id -> navigate("detail/$id") }, { navigate("tickets") }) }
                composable("tickets") { ReceiptListScreen(receipts, query, { query = it }, { id -> navigate("detail/$id") }) }
                composable("add") { AddReceiptScreen(viewModel, { id -> navController.navigate("detail/$id") { popUpTo("home") } }, goHome) }
                composable("detail/{id}") { entry ->
                    val id = entry.arguments?.getString("id")?.toLongOrNull()
                    val selected = receipts.firstOrNull { it.id == id }
                    ReceiptDetailScreen(selected, { navController.popBackStack() }, viewModel::delete, viewModel::save)
                }
            }
        }
    }
}
