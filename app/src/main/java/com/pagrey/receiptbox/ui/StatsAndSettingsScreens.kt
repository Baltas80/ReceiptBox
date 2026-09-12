package com.pagrey.receiptbox.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pagrey.receiptbox.data.Receipt
import com.pagrey.receiptbox.util.ReceiptBackup
import com.pagrey.receiptbox.util.ReceiptCsvExporter
import com.pagrey.receiptbox.util.ReceiptPdfExporter
import com.pagrey.receiptbox.util.parseReceiptDate
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private val statsEuro = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
private val statsCategories = listOf("Alimentación", "Hogar", "Transporte", "Salud", "Tecnología", "Ocio", "Ropa", "Otros")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(receipts: List<Receipt>, onBack: () -> Unit) {
    val today = remember { LocalDate.now() }
    val monthReceipts = receipts.filter { parseReceiptDate(it.date)?.let { d -> d.year == today.year && d.monthValue == today.monthValue } == true }
    val monthReceiptsWithTotal = monthReceipts.filter { it.total != null }
    val allReceiptsWithTotal = receipts.filter { it.total != null }
    val monthTotal = monthReceiptsWithTotal.sumOf { it.total ?: 0.0 }
    val categoryTotals = monthReceiptsWithTotal.groupBy { if (it.category in statsCategories) it.category else "Otros" }
        .mapValues { (_, items) -> items.sumOf { it.total ?: 0.0 } }
        .filterValues { it > 0.0 }
        .toList().sortedByDescending { it.second }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Estadísticas") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Este mes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("Gasto total", style = MaterialTheme.typography.labelLarge); Text(statsEuro.format(monthTotal), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                        Column(horizontalAlignment = Alignment.End) { Text("Tickets", style = MaterialTheme.typography.labelLarge); Text(monthReceipts.size.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            item { Text("Gasto por categoría", style = MaterialTheme.typography.titleLarge) }
            if (categoryTotals.isEmpty()) {
                item { Card(Modifier.fillMaxWidth()) { Text("Aún no hay gastos categorizados este mes.", Modifier.padding(18.dp)) } }
            } else {
                items(categoryTotals.size) { index ->
                    val (category, amount) = categoryTotals[index]
                    val fraction = if (monthTotal > 0) (amount / monthTotal).toFloat() else 0f
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(category, fontWeight = FontWeight.SemiBold); Text(statsEuro.format(amount), fontWeight = FontWeight.SemiBold) }
                            LinearProgressIndicator(progress = { fraction.coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
                            Text("${(fraction * 100).toInt()} % del gasto", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text("Resumen", style = MaterialTheme.typography.titleLarge)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryRow("Gasto acumulado", statsEuro.format(allReceiptsWithTotal.sumOf { it.total ?: 0.0 }))
                        SummaryRow("Tickets registrados", receipts.size.toString())
                        SummaryRow("Tickets con importe", allReceiptsWithTotal.size.toString())
                        SummaryRow("Media por ticket", if (allReceiptsWithTotal.isEmpty()) "—" else statsEuro.format(allReceiptsWithTotal.sumOf { it.total ?: 0.0 } / allReceiptsWithTotal.size))
                        SummaryRow("Media este mes", if (monthReceiptsWithTotal.isEmpty()) "—" else statsEuro.format(monthTotal / monthReceiptsWithTotal.size))
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(value, fontWeight = FontWeight.SemiBold) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(receipts: List<Receipt>, darkTheme: Boolean, onDarkThemeChanged: (Boolean) -> Unit, onRestore: (List<Receipt>) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val csvContent = remember(receipts) { ReceiptCsvExporter.export(receipts) }
    val backupContent = remember(receipts) { ReceiptBackup.export(receipts) }
    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) result.data?.data?.let { uri ->
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(csvContent.toByteArray(Charsets.UTF_8)) } }
                .onFailure { Toast.makeText(context, "No se pudo exportar el CSV", Toast.LENGTH_LONG).show() }
        }
    }
    val backupExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) result.data?.data?.let { uri ->
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(backupContent.toByteArray(Charsets.UTF_8)) } }
                .onFailure { Toast.makeText(context, "No se pudo crear la copia", Toast.LENGTH_LONG).show() }
        }
    }
    val pdfExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) result.data?.data?.let { uri ->
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(ReceiptPdfExporter.export(receipts)) } }
                .onSuccess { Toast.makeText(context, "PDF exportado", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "No se pudo exportar el PDF", Toast.LENGTH_LONG).show() }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { ReceiptBackup.import(it.readText()) }
                ?: error("No se pudo leer la copia")
        }.onSuccess { restored ->
            onRestore(restored)
            Toast.makeText(context, "Restaurados ${restored.size} tickets", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Copia no válida: ${it.message ?: "error desconocido"}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Ajustes") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Apariencia", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (darkTheme) Icons.Default.DarkMode else Icons.Default.LightMode, null, Modifier.size(28.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) { Text("Modo oscuro", fontWeight = FontWeight.SemiBold); Text(if (darkTheme) "Activado" else "Desactivado", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Switch(checked = darkTheme, onCheckedChange = onDarkThemeChanged)
                    }
                }
            }
            item {
                Text("Aplicación", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SettingRow(Icons.Default.Category, "Categorías", "Alimentación, Hogar, Transporte y más")
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Backup, null, Modifier.size(24.dp))
                            Column(Modifier.weight(1f).padding(start = 14.dp)) { Text("Copia de seguridad", fontWeight = FontWeight.SemiBold); Text("JSON · ${receipts.size} tickets", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Row {
                                TextButton(onClick = { backupExportLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { type = "application/json"; putExtra(Intent.EXTRA_TITLE, "receiptbox_backup.json") }) }) { Text("Crear") }
                                TextButton(onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain")) }) { Text("Restaurar") }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, null, Modifier.size(24.dp))
                            Column(Modifier.weight(1f).padding(start = 14.dp)) { Text("Exportar PDF", fontWeight = FontWeight.SemiBold); Text("Informe · ${receipts.size} tickets", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            TextButton(onClick = { pdfExportLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { type = "application/pdf"; putExtra(Intent.EXTRA_TITLE, "receiptbox_tickets.pdf") }) }) { Text("Exportar") }
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FileDownload, null, Modifier.size(24.dp))
                            Column(Modifier.weight(1f).padding(start = 14.dp)) { Text("Exportar datos", fontWeight = FontWeight.SemiBold); Text("CSV · ${receipts.size} tickets", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            TextButton(onClick = { csvExportLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { type = "text/csv"; putExtra(Intent.EXTRA_TITLE, "receiptbox_tickets.csv") }) }) { Text("Exportar") }
                        }
                    }
                }
            }
            item {
                Text("Información", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SettingRow(Icons.Default.ReceiptLong, "ReceiptBox", "Tus tickets, siempre a mano")
                        SettingRow(Icons.Default.Info, "Versión", "0.1.0")
                        SettingRow(Icons.Default.Security, "Privacidad", "Tus tickets se almacenan localmente")
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(24.dp)); Column(Modifier.padding(start = 14.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
}
