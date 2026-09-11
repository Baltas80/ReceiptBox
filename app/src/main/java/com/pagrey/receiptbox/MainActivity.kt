package com.pagrey.receiptbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.pagrey.receiptbox.ui.*
import com.pagrey.receiptbox.ui.theme.ReceiptBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReceiptBoxTheme { ReceiptBoxApp() } }
    }
}

@Composable
private fun ReceiptBoxApp(viewModel: ReceiptBoxViewModel = viewModel()) {
    val receipts by viewModel.receipts.collectAsStateWithLifecycle(initialValue = emptyList())
    val navController = rememberNavController()
    var query by remember { mutableStateOf("") }
    val navigate: (String) -> Unit = { route -> navController.navigate(route) { launchSingleTop = true } }
    val goHome = { navController.navigate("home") { popUpTo("home") { inclusive = true }; launchSingleTop = true } }

    Scaffold(bottomBar = {
        val route = navController.currentBackStackEntry?.destination?.route ?: "home"
        if (route == "home" || route == "tickets" || route == "add") BottomNav(route, navigate)
    }) { paddingValues ->
        Box(Modifier.padding(paddingValues)) {
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
