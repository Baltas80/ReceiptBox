package com.pagrey.receiptbox.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pagrey.receiptbox.data.Receipt
import com.pagrey.receiptbox.util.parseReceiptAmount
import com.pagrey.receiptbox.util.parseReceiptDate
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private val euro = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
private val categories = listOf("Alimentación", "Hogar", "Transporte", "Salud", "Tecnología", "Ocio", "Ropa", "Otros")

@Composable
fun HomeScreen(receipts: List<Receipt>, onAdd: () -> Unit, onOpen: (Long) -> Unit, onSeeAll: () -> Unit) {
    val today = remember { LocalDate.now() }
    val monthReceipts = remember(receipts, today.year, today.monthValue) {
        receipts.filter { receipt -> parseReceiptDate(receipt.date)?.let { it.year == today.year && it.monthValue == today.monthValue } == true }
    }
    val orderedReceipts = remember(receipts) { sortReceipts(receipts) }
    val monthTotal = monthReceipts.sumOf { it.total ?: 0.0 }
    val total = receipts.sumOf { it.total ?: 0.0 }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_SMALL),
                modifier = Modifier.size(58.dp)
            ) { Icon(Icons.Default.Add, "Añadir ticket") }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
            contentPadding = PaddingValues(
                start = ReceiptBoxDesign.SCREEN_PADDING,
                end = ReceiptBoxDesign.SCREEN_PADDING,
                top = ReceiptBoxDesign.SCREEN_PADDING,
                bottom = 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(ReceiptBoxDesign.ITEM_SPACING)
        ) {
            item {
                Text(ReceiptBoxDesign.APP_NAME, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(ReceiptBoxDesign.TAGLINE, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(ReceiptBoxDesign.COMPACT_SPACING))

                Box(
                    Modifier.fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(ReceiptBoxDesign.CORNER_LARGE))
                        .clip(RoundedCornerShape(ReceiptBoxDesign.CORNER_LARGE))
                        .background(
                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer))
                        )
                ) {
                    Column(Modifier.padding(ReceiptBoxDesign.CARD_PADDING + 6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text("GASTO DE ESTE MES", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.height(ReceiptBoxDesign.FIELD_SPACING / 2))
                                Text(euro.format(monthTotal), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Surface(shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_SMALL), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f)) {
                                Icon(Icons.Default.AccountBalanceWallet, null, Modifier.padding(10.dp).size(24.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(Modifier.height(ReceiptBoxDesign.ITEM_SPACING))
                        Text("${monthReceipts.size} tickets este mes  ·  ${receipts.size} en total", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ReceiptBoxDesign.COMPACT_SPACING + 4.dp)) {
                    StatCard("Gasto acumulado", euro.format(total), Modifier.weight(1f), Icons.Default.Payments)
                    StatCard("Tickets", receipts.size.toString(), Modifier.weight(0.72f), Icons.Default.ReceiptLong)
                }

                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth().height(ReceiptBoxDesign.PRIMARY_BUTTON_HEIGHT),
                    shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_SMALL + 3.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(ReceiptBoxDesign.COMPACT_SPACING))
                    Text(ReceiptBoxDesign.ADD_RECEIPT, style = MaterialTheme.typography.titleMedium)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tickets recientes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    if (orderedReceipts.isNotEmpty()) TextButton(onClick = onSeeAll) { Text("Ver todos") }
                }
            }

            if (orderedReceipts.isEmpty()) {
                item { EmptyState() }
            } else {
                items(orderedReceipts.take(5), key = { it.id }) { ReceiptRow(it, onOpen) }
            }

            item { AdBannerPlaceholder() }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_MEDIUM),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(ReceiptBoxDesign.CARD_PADDING)) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(ReceiptBoxDesign.COMPACT_SPACING))
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AdBannerPlaceholder() {
    Surface(
        Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_SMALL + 2.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = ReceiptBoxDesign.CARD_PADDING), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Campaign, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(ReceiptBoxDesign.COMPACT_SPACING))
            Text("Espacio publicitario", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyState() {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_LARGE - 2.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_SMALL + 4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Default.ReceiptLong, null, Modifier.padding(14.dp).size(34.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(ReceiptBoxDesign.ITEM_SPACING))
            Text(ReceiptBoxDesign.EMPTY_TITLE, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(ReceiptBoxDesign.EMPTY_BODY, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptListScreen(receipts: List<Receipt>, query: String, onQuery: (String) -> Unit, onOpen: (Long) -> Unit) {
    val filtered = remember(receipts, query) {
        val matching = if (query.isBlank()) receipts else receipts.filter { it.merchant.contains(query, true) || it.rawText.contains(query, true) || it.category.contains(query, true) }
        sortReceipts(matching)
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Tickets", fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = ReceiptBoxDesign.SCREEN_PADDING)) {
            OutlinedTextField(
                query,
                onQuery,
                Modifier.fillMaxWidth().heightIn(min = ReceiptBoxDesign.SEARCH_FIELD_HEIGHT),
                placeholder = { Text(ReceiptBoxDesign.SEARCH_PLACEHOLDER) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_MEDIUM)
            )
            Spacer(Modifier.height(ReceiptBoxDesign.ITEM_SPACING))
            if (filtered.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (query.isBlank()) "No hay tickets todavía" else "No se han encontrado resultados", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else LazyColumn(contentPadding = PaddingValues(bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(ReceiptBoxDesign.COMPACT_SPACING + 2.dp)) { items(filtered, key = { it.id }) { ReceiptRow(it, onOpen) }; item { Spacer(Modifier.height(4.dp)); AdBannerPlaceholder() } }
        }
    }
}

private fun sortReceipts(receipts: List<Receipt>): List<Receipt> = receipts.sortedWith(
    compareByDescending<Receipt> { parseReceiptDate(it.date) ?: LocalDate.MIN }
        .thenByDescending { it.createdAt }
)

@Composable private fun ReceiptRow(receipt: Receipt, onOpen: (Long) -> Unit) {
    Surface(
        onClick = { onOpen(receipt.id) },
        modifier = Modifier.fillMaxWidth().heightIn(min = ReceiptBoxDesign.LIST_ROW_MIN_HEIGHT),
        shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_SMALL + 4.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(Modifier.padding(horizontal = ReceiptBoxDesign.REVIEW_CARD_PADDING, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_COMPACT + 3.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Default.ReceiptLong, null, Modifier.padding(10.dp).size(23.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.padding(start = ReceiptBoxDesign.COMPACT_SPACING + 5.dp).weight(1f)) {
                Text(receipt.merchant.ifBlank { "Ticket sin comercio" }, fontWeight = FontWeight.SemiBold)
                Text(listOf(receipt.date, receipt.category).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Datos pendientes" }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text(receipt.total?.let(euro::format) ?: "—", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(receipt: Receipt?, onBack: () -> Unit, onDelete: (Receipt) -> Unit, onUpdate: (Receipt) -> Unit) {
    var editing by remember(receipt?.id) { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("Detalle", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") } }, actions = { if (receipt != null) IconButton(onClick = { editing = true }) { Icon(Icons.Default.Edit, "Editar") } }) }) { padding ->
        val current = receipt
        if (current == null) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("No se ha encontrado el ticket") }
        else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = ReceiptBoxDesign.SCREEN_PADDING), contentPadding = PaddingValues(bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(ReceiptBoxDesign.ITEM_SPACING)) {
                item {
                    Text(current.merchant.ifBlank { "Comercio no detectado" }, style = MaterialTheme.typography.headlineMedium)
                    if (current.imagePath.isNotBlank()) {
                        val bitmap = remember(current.imagePath) { BitmapFactory.decodeFile(current.imagePath) }
                        bitmap?.let { Image(it.asImageBitmap(), "Imagen original del ticket", Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(ReceiptBoxDesign.CORNER_LARGE - 4.dp))) }
                    }
                    DetailField("Fecha", current.date)
                    DetailField("Total", current.total?.let(euro::format) ?: "No detectado")
                    DetailField("IVA", current.tax?.let(euro::format) ?: "No detectado")
                    DetailField("N.º de ticket", current.receiptNumber)
                    DetailField("Categoría", current.category)
                    Button(onClick = { onDelete(current); onBack() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_SMALL + 2.dp)) { Text("Eliminar ticket") }
                }
                item { Text("Texto OCR original", style = MaterialTheme.typography.titleLarge); Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_SMALL + 4.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Text(current.rawText.ifBlank { "Sin texto OCR" }, Modifier.padding(ReceiptBoxDesign.CARD_PADDING)) } }
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
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar ticket", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(ReceiptBoxDesign.FIELD_SPACING)) {
                OutlinedTextField(merchant, { merchant = it }, label = { Text("Comercio") }, singleLine = true)
                OutlinedTextField(date, { date = it }, label = { Text("Fecha") }, singleLine = true)
                OutlinedTextField(total, { total = it }, label = { Text("Total") }, singleLine = true)
                OutlinedTextField(tax, { tax = it }, label = { Text("IVA") }, singleLine = true)
                OutlinedTextField(number, { number = it }, label = { Text("N.º de ticket") }, singleLine = true)
                Box {
                    Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_SMALL)) { Text(category) }
                    DropdownMenu(expanded, { expanded = false }) { categories.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { category = item; expanded = false }) } }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(receipt.copy(merchant = merchant.trim(), date = date.trim(), total = parseReceiptAmount(total), tax = parseReceiptAmount(tax), receiptNumber = number.trim(), category = category)) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable private fun DetailField(label: String, value: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_SMALL), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(ReceiptBoxDesign.CARD_PADDING)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun BottomNav(route: String, onNavigate: (String) -> Unit) {
    NavigationBar {
        NavigationBarItem(selected = route == "home", onClick = { onNavigate("home") }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Inicio") })
        NavigationBarItem(selected = route == "tickets", onClick = { onNavigate("tickets") }, icon = { Icon(Icons.Default.ReceiptLong, null) }, label = { Text("Tickets") })
        NavigationBarItem(selected = route == "stats", onClick = { onNavigate("stats") }, icon = { Icon(Icons.Default.BarChart, null) }, label = { Text("Estadísticas") })
        NavigationBarItem(selected = route == "settings", onClick = { onNavigate("settings") }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Ajustes") })
    }
}
