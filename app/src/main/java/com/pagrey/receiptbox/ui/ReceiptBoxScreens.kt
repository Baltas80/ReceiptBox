package com.pagrey.receiptbox.ui

import android.graphics.BitmapFactory
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pagrey.receiptbox.data.Receipt
import java.text.NumberFormat
import java.util.Locale

private val euro = NumberFormat.getCurrencyInstance(Locale("es", "ES"))

@Composable
fun HomeScreen(receipts: List<Receipt>, onAdd: () -> Unit, onOpen: (Long) -> Unit, onSeeAll: () -> Unit) {
    val total = receipts.sumOf { it.total ?: 0.0 }
    Scaffold(floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = "Añadir ticket") } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Spacer(Modifier.height(8.dp)); Text("ReceiptBox", style = MaterialTheme.typography.headlineMedium)
                Text("Tus tickets, siempre a mano", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(18.dp)) { Text("Gasto registrado", style = MaterialTheme.typography.labelLarge); Text(euro.format(total), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("En ${receipts.size} tickets", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Spacer(Modifier.height(8.dp)); Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(52.dp)) { Icon(Icons.Default.Add, null); Text("  Añadir ticket") }
                Spacer(Modifier.height(8.dp)); Text("Tickets recientes", style = MaterialTheme.typography.titleLarge)
            }
            if (receipts.isEmpty()) item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.ReceiptLong, null, Modifier.size(48.dp)); Text("Aún no tienes tickets", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp)); Text("Fotografía o importa tu primer ticket y ReceiptBox organizará sus datos.", modifier = Modifier.padding(top = 8.dp)) } } }
            else { items(receipts.take(5), key = { it.id }) { ReceiptRow(it, onOpen) }; item { Button(onClick = onSeeAll, modifier = Modifier.fillMaxWidth()) { Text("Ver todos") } } }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptListScreen(receipts: List<Receipt>, query: String, onQuery: (String) -> Unit, onOpen: (Long) -> Unit) {
    val filtered = remember(receipts, query) { if (query.isBlank()) receipts else receipts.filter { it.merchant.contains(query, true) || it.rawText.contains(query, true) } }
    Scaffold(topBar = { SmallTopAppBar(title = { Text("Tickets") }) }) { padding -> Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
        OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), placeholder = { Text("Buscar tickets") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
        Spacer(Modifier.height(12.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(filtered, key = { it.id }) { ReceiptRow(it, onOpen) } }
    } }
}

@Composable private fun ReceiptRow(receipt: Receipt, onOpen: (Long) -> Unit) { Card(onClick = { onOpen(receipt.id) }, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ReceiptLong, null, Modifier.size(32.dp)); Column(Modifier.padding(start = 14.dp).weight(1f)) { Text(receipt.merchant.ifBlank { "Ticket sin comercio" }, fontWeight = FontWeight.SemiBold); Text(receipt.date.ifBlank { "Fecha no detectada" }, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(receipt.total?.let(euro::format) ?: "—", fontWeight = FontWeight.Bold) } } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(receipt: Receipt?, onBack: () -> Unit, onDelete: (Receipt) -> Unit) { Scaffold(topBar = { SmallTopAppBar(title = { Text("Detalle") }, navigationIcon = { IconButton(onClick = onBack) { Text("‹") } }) }) { padding -> if (receipt == null) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("No se ha encontrado el ticket") } else LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text(receipt.merchant.ifBlank { "Comercio no detectado" }, style = MaterialTheme.typography.headlineMedium); Field("Fecha", receipt.date); Field("Total", receipt.total?.let(euro::format) ?: "No detectado"); Field("IVA", receipt.tax?.let(euro::format) ?: "No detectado"); Field("N.º de ticket", receipt.receiptNumber.ifBlank { "No detectado" }); Field("Categoría", receipt.category); if (receipt.imagePath.isNotBlank()) { val bitmap = remember(receipt.imagePath) { BitmapFactory.decodeFile(receipt.imagePath) }; bitmap?.let { androidx.compose.foundation.Image(it.asImageBitmap(), null, Modifier.fillMaxWidth().height(260.dp)) } }; Button(onClick = { onDelete(receipt); onBack() }, modifier = Modifier.fillMaxWidth()) { Text("Eliminar ticket") } }; item { Text("Texto OCR original", style = MaterialTheme.typography.titleLarge); Card(Modifier.fillMaxWidth()) { Text(receipt.rawText.ifBlank { "Sin texto OCR" }, Modifier.padding(16.dp)) } } } } }

@Composable private fun Field(label: String, value: String) { Column(Modifier.fillMaxWidth()) { Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge) } }

@Composable fun BottomNav(selected: String, onSelect: (String) -> Unit) { NavigationBar { NavigationBarItem(selected == "home", { onSelect("home") }, icon = { Icon(Icons.Default.ReceiptLong, null) }, label = { Text("Inicio") }); NavigationBarItem(selected == "tickets", { onSelect("tickets") }, icon = { Icon(Icons.Default.Search, null) }, label = { Text("Tickets") }); NavigationBarItem(selected == "add", { onSelect("add") }, icon = { Icon(Icons.Default.Add, null) }, label = { Text("Añadir") }) } }
