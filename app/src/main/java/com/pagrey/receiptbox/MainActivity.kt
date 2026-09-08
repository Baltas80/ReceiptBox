package com.pagrey.receiptbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReceiptBoxApp() }
    }
}

@Composable
private fun ReceiptBoxApp() {
    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("ReceiptBox") }) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Tus tickets, organizados.",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Guarda recibos y consulta tus compras rápidamente.",
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )
                Button(onClick = { /* Scanner will be added in the next milestone. */ }) {
                    Text("Añadir ticket")
                }
            }
        }
    }
}
