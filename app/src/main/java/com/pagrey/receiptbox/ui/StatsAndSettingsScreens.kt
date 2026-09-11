package com.pagrey.receiptbox.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pagrey.receiptbox.data.Receipt
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private val statsEuro = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
private val statsCategories = listOf("Alimentación", "Hogar", "Transporte", "Salud", "Tecnología", "Ocio", "Ropa", "Otros")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(receipts: List<Receipt>, onBack: () -> Unit) {
    val today = remember { LocalDate.now() }
    val monthReceipts = receipts.filter { parseStatsDate(it.date)?.let { d -> d.year == today.year && d.monthValue == today.monthValue } == true }
    val monthTotal = monthReceipts.sumOf { it.total ?: 0.0 }
    val categoryTotals = monthReceipts.groupBy { if (it.category in statsCategories) it.category else "Otros" }
        .mapValues { (_, items) -> items.sumOf { it.total ?: 0.0 } }
        .filterValues { it > 0.0 }
        .toList().sortedByDescending { it.second }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Estadísticas") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
        })
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(category, fontWeight = FontWeight.SemiBold)
                                Text(statsEuro.format(amount), fontWeight = FontWeight.SemiBold)
                            }
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
                        SummaryRow("Gasto acumulado", statsEuro.format(receipts.sumOf { it.total ?: 0.0 }))
                        SummaryRow("Tickets registrados", receipts.size.toString())
                        SummaryRow("Media por ticket", if (receipts.isEmpty()) "—" else statsEuro.format(receipts.sumOf { it.total ?: 0.0 } / receipts.size))
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
fun SettingsScreen(darkTheme: Boolean, onDarkThemeChanged: (Boolean) -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Ajustes") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Apariencia", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (darkTheme) Icons.Default.DarkMode else Icons.Default.LightMode, null, Modifier.size(28.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text("Modo oscuro", fontWeight = FontWeight.SemiBold)
                            Text(if (darkTheme) "Activado" else "Desactivado", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = darkTheme, onCheckedChange = onDarkThemeChanged)
                    }
                }
            }
            item {
                Text("Aplicación", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SettingRow(Icons.Default.Category, "Categorías", "Alimentación, Hogar, Transporte y más")
                        SettingRow(Icons.Default.CloudOff, "Copia de seguridad", "Próximamente")
                        SettingRow(Icons.Default.FileDownload, "Exportar datos", "PDF/CSV · Próximamente")
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(24.dp))
        Column(Modifier.padding(start = 14.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

private fun parseStatsDate(value: String): LocalDate? {
    val patterns = listOf("dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd", "yyyy/MM/dd")
    for (pattern in patterns) {
        try { return LocalDate.parse(value.trim(), java.time.format.DateTimeFormatter.ofPattern(pattern)) } catch (_: Exception) { }
    }
    return null
}
