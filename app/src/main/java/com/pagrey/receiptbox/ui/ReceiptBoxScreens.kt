package com.pagrey.receiptbox.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pagrey.receiptbox.data.Receipt
import com.pagrey.receiptbox.util.parseReceiptAmount
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val euro = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
private val categories = listOf("Alimentación", "Hogar", "Transporte", "Salud", "Tecnología", "Ocio", "Ropa", "Otros")
private val receiptDateFormatters = listOf(
    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
    DateTimeFormatter.ofPattern("dd-MM-yyyy"),
    DateTimeFormatter.ISO_LOCAL_DATE,
    DateTimeFormatter.ofPattern("yyyy/MM/dd")
)

@Composable
fun HomeScreen(receipts: List<Receipt>, onAdd: () -> Unit, onOpen: (Long) -> Unit, onSeeAll: () -> Unit) {
    val today = remember { LocalDate.now() }
    val monthReceipts = remember(receipts, today.year, today.monthValue) {
        receipts.filter { receipt ->
            parseReceiptDate(receipt.date)?.let { it.year == today.year && it.monthValue == today.monthValue } == true
        }
    }
    val monthTotal = monthReceipts.sumOf { it.total ?: 0.0 }
    val total = receipts.sumOf { it.total ?: 0.0 }

    Scaffold(floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, "Añadir ticket") } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Spacer(Modifier.height(12.dp))
                Text("ReceiptBox", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Tus tickets, siempre a mano", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Gasto de este mes", style = MaterialTheme.typography.labelLarge)
                        Text(euro.format(monthTotal), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("${monthReceipts.size} tickets este mes · ${receipts.size} en total", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(4.dp))
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("Gasto acumulado", style = MaterialTheme.typography.labelLarge); Text(euro.format(total), fontWeight = FontWeight.SemiBold) }
                        Column(horizontalAlignment = Alignment.End) { Text("Tickets", style = MaterialTheme.typography.labelLarge); Text(receipts.size.toString(), fontWeight = FontWeight.SemiBold) }
                    }
                }
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(52.dp)) { Icon(Icons.Default.Add, null); Text("  Añadir ticket") }
                Text("Tickets recientes", style = MaterialTheme.typography.titleLarge)
            }
            if (receipts.isEmpty()) item { EmptyState() } else {
                items(receipts.take(5), key = { it.id }) { ReceiptRow(it, onOpen) }
                item { Button(onClick = onSeeAll, modifier = Modifier.fillMaxWidth()) { Text("Ver todos los tickets") } }
            }
            item { AdBannerPlaceholder() }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable private fun AdBannerPlaceholder() {
    Surface(Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
        Box(contentAlignment = Alignment.Center) { Text("Espacio publicitario", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun EmptyState() {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ReceiptLong, null, Modifier.size(48.dp))
        Text("Aún no tienes tickets", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
        Text("Fotografía o importa tu primer ticket y ReceiptBox organizará sus datos.", modifier = Modifier.padding(top = 8.dp))
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptListScreen(receipts: List<Receipt>, query: String, onQuery: (String) -> Unit, onOpen: (Long) -> Unit) {
    val filtered = remember(receipts, query) { if (query.isBlank()) receipts else receipts.filter { it.merchant.contains(query, true) || it.rawText.contains(query, true) || it.category.contains(query, true) } }
    Scaffold(topBar = { TopAppBar(title = { Text("Tickets") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), placeholder = { Text("Buscar tickets") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
            Spacer(Modifier.height(12.dp))
            if (filtered.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (query.isBlank()) "No hay tickets todavía" else "No se han encontrado resultados") }
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(filtered, key = { it.id }) { ReceiptRow(it, onOpen) }; item { Spacer(Modifier.height(8.dp)); AdBannerPlaceholder() } }
        }
    }
}

@Composable private fun ReceiptRow(receipt: Receipt, onOpen: (Long) -> Unit) {
    Card(onClick = { onOpen(receipt.id) }, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.ReceiptLong, null, Modifier.size(32.dp))
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(receipt.merchant.ifBlank { "Ticket sin comercio" }, fontWeight = FontWeight.SemiBold)
            Text(listOf(receipt.date, receipt.category).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Datos pendientes" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(receipt.total?.let(euro::format) ?: "—", fontWeight = FontWeight.Bold)
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(receipt: Receipt?, onBack: () -> Unit, onDelete: (Receipt) -> Unit, onUpdate: (Receipt) -> Unit) {
    var editing by remember(receipt?.id) { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("Detalle") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") } }, actions = { if (receipt != null) IconButton(onClick = { editing = true }) { Icon(Icons.Default.Edit, "Editar") } }) }) { padding ->
        val current = receipt
        if (current == null) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("No se ha encontrado el ticket") }
        else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Text(current.merchant.ifBlank { "Comercio no detectado" }, style = MaterialTheme.typography.headlineMedium)
                    if (current.imagePath.isNotBlank()) {
                        val bitmap = remember(current.imagePath) { BitmapFactory.decodeFile(current.imagePath) }
                        bitmap?.let { Image(it.asImageBitmap(), "Imagen original del ticket", Modifier.fillMaxWidth().height(260.dp)) }
                    }
                    DetailField("Fecha", current.date)
                    DetailField("Total", current.total?.let(euro::format) ?: "No detectado")
                    DetailField("IVA", current.tax?.let(euro::format) ?: "No detectado")
                    DetailField("N.º de ticket", current.receiptNumber)
                    DetailField("Categoría", current.category)
                    Button(onClick = { onDelete(current); onBack() }, modifier = Modifier.fillMaxWidth()) { Text("Eliminar ticket") }
                }
                item { Text("Texto OCR original", style = MaterialTheme.typography.titleLarge); Card(Modifier.fillMaxWidth()) { Text(current.rawText.ifBlank { "Sin texto OCR" }, Modifier.padding(16.dp)) } }
            }
            if (editing) EditReceiptDialog(current, { editing = false }) { onUpdate(it); editing = false }
        }
    }
}

@Composable private fun EditReceiptDialog(receipt: Receipt, onDismiss: () -> Unit, onSave: (Receipt) -> Unit) {
    var merchant by remember(receipt.id) { mutableStateOf(receipt.merchant) }
    var date by remember(receipt.id) { mutableStateOf(receipt.date) }
    var total by remember(receipt.id) { mutableStateOf(receipt.total?.toString().orEmpty()) }
    var tax by remember(receipt.id) { mutableStateOf(receipt.tax?.toString().orEmpty()) }
    var number by remember(receipt.id) { mutableStateOf(receipt.receiptNumber) }
    var category by remember(receipt.id) { mutableStateOf(if (receipt.category in categories) receipt.category else "Otros") }
    var expanded by remember(receipt.id) { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Editar ticket") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(merchant, { merchant = it }, label = { Text("Comercio") }, singleLine = true)
        OutlinedTextField(date, { date = it }, label = { Text("Fecha") }, singleLine = true)
        OutlinedTextField(total, { total = it }, label = { Text("Total") }, singleLine = true)
        OutlinedTextField(tax, { tax = it }, label = { Text("IVA") }, singleLine = true)
        OutlinedTextField(number, { number = it }, label = { Text("N.º de ticket") }, singleLine = true)
        Box { Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(category) }; DropdownMenu(expanded, { expanded = false }) { categories.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { category = item; expanded = false }) } } }
    } }, confirmButton = { Button(onClick = { onSave(receipt.copy(merchant = merchant.trim(), date = date.trim(), total = parseReceiptAmount(total), tax = parseReceiptAmount(tax), receiptNumber = number.trim(), category = category)) }) { Text("Guardar") } }, dismissButton = { Button(onClick = onDismiss) { Text("Cancelar") } })
}

private fun parseReceiptDate(value: String): LocalDate? {
    val clean = value.trim()
    if (clean.isEmpty()) return null
    return receiptDateFormatters.firstNotNullOfOrNull { formatter ->
        try { LocalDate.parse(clean, formatter) } catch (_: DateTimeParseException) { null }
    }
}

@Composable private fun DetailField(label: String, value: String) { Column(Modifier.fillMaxWidth()) { Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value.ifBlank { "—" }) } }

@Composable fun BottomNav(selected: String, onSelect: (String) -> Unit) { NavigationBar {
    NavigationBarItem(selected == "home", { onSelect("home") }, icon = { Icon(Icons.Default.ReceiptLong, null) }, label = { Text("Inicio") })
    NavigationBarItem(selected == "tickets", { onSelect("tickets") }, icon = { Icon(Icons.Default.Search, null) }, label = { Text("Tickets") })
    NavigationBarItem(selected == "add", { onSelect("add") }, icon = { Icon(Icons.Default.Add, null) }, label = { Text("Añadir") })
} }
