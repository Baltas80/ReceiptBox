package com.pagrey.receiptbox.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.pagrey.receiptbox.util.ReceiptFullBackup
import com.pagrey.receiptbox.util.ReceiptPdfExporter
import com.pagrey.receiptbox.util.parseReceiptDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private val statsEuro = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
private val statsCategories = listOf("Alimentación", "Hogar", "Transporte", "Salud", "Tecnología", "Ocio", "Ropa", "Otros")
private const val MAX_BACKUP_IMAGE_BYTES = 10 * 1024 * 1024

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(receipts: List<Receipt>, onBack: () -> Unit) {
    val today = remember { LocalDate.now() }
    val monthReceipts = receipts.filter { parseReceiptDate(it.date)?.let { d -> d.year == today.year && d.monthValue == today.monthValue } == true }
    val monthReceiptsWithTotal = monthReceipts.filter { it.total != null }
    val allReceiptsWithTotal = receipts.filter { it.total != null }
    val monthTotal = monthReceiptsWithTotal.sumOf { it.total ?: 0.0 }
    val categoryTotals = monthReceiptsWithTotal.groupBy { if (it.category in statsCategories) it.category else "Otros" }
        .mapValues { (_, items) -> items.sumOf { it.total ?: 0.0 } }.filterValues { it > 0.0 }
        .toList().sortedByDescending { it.second }
    Scaffold(topBar = { TopAppBar(title = { Text("Estadísticas", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), contentPadding = PaddingValues(bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Spacer(Modifier.height(4.dp))
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_LARGE), color = MaterialTheme.colorScheme.primaryContainer, tonalElevation = 3.dp) {
                    Column(Modifier.fillMaxWidth().padding(22.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text("CONTROL DE GASTOS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(6.dp)); Text("Este mes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(statsEuro.format(monthTotal), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                            }
                            Surface(shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_MEDIUM), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) { Icon(Icons.Default.BarChart, null, Modifier.padding(11.dp).size(26.dp), tint = MaterialTheme.colorScheme.primary) }
                        }
                        Spacer(Modifier.height(14.dp)); Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) { Text("${monthReceipts.size} tickets", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${monthReceiptsWithTotal.size} con importe", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            item { Text("Gasto por categoría", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
            if (categoryTotals.isEmpty()) item { Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_LARGE), color = MaterialTheme.colorScheme.surfaceVariant) { Text("Aún no hay gastos categorizados este mes.", Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            else items(categoryTotals.size) { index ->
                val (category, amount) = categoryTotals[index]; val fraction = if (monthTotal > 0) (amount / monthTotal).toFloat() else 0f
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_MEDIUM), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(category, fontWeight = FontWeight.SemiBold); Text(statsEuro.format(amount), fontWeight = FontWeight.Bold) }; LinearProgressIndicator(progress = { fraction.coerceIn(0f, 1f) }, Modifier.fillMaxWidth(), trackColor = MaterialTheme.colorScheme.surfaceVariant); Text("${(fraction * 100).toInt()} % del gasto", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            }
            item { Spacer(Modifier.height(4.dp)); Text("Resumen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_LARGE), color = MaterialTheme.colorScheme.surfaceVariant) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { SummaryRow("Gasto acumulado", statsEuro.format(allReceiptsWithTotal.sumOf { it.total ?: 0.0 })); SummaryRow("Tickets registrados", receipts.size.toString()); SummaryRow("Tickets con importe", allReceiptsWithTotal.size.toString()); SummaryRow("Media por ticket", if (allReceiptsWithTotal.isEmpty()) "—" else statsEuro.format(allReceiptsWithTotal.sumOf { it.total ?: 0.0 } / allReceiptsWithTotal.size)); SummaryRow("Media este mes", if (monthReceiptsWithTotal.isEmpty()) "—" else statsEuro.format(monthTotal / monthReceiptsWithTotal.size)) } } }
        }
    }
}

@Composable private fun SummaryRow(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontWeight = FontWeight.Bold) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(receipts: List<Receipt>, darkTheme: Boolean, onDarkThemeChanged: (Boolean) -> Unit, onRestore: (List<Receipt>) -> Unit, onRestoreFull: (List<Receipt>) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope(); val csvContent = remember(receipts) { ReceiptCsvExporter.export(receipts) }; val backupContent = remember(receipts) { ReceiptBackup.export(receipts) }
    var pendingRestore by remember { mutableStateOf<List<Receipt>?>(null) }; var pendingFullRestore by remember { mutableStateOf<ReceiptFullBackup.RestoreResult?>(null) }; var pendingFullBackupBytes by remember { mutableStateOf<ByteArray?>(null) }
    val fullBackupExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> if (result.resultCode == android.app.Activity.RESULT_OK) result.data?.data?.let { uri -> pendingFullBackupBytes?.let { bytes -> runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("No se pudo abrir el archivo de destino") }.onSuccess { pendingFullBackupBytes = null; Toast.makeText(context, "Copia completa creada", Toast.LENGTH_SHORT).show() }.onFailure { Toast.makeText(context, "No se pudo crear la copia completa", Toast.LENGTH_LONG).show() } } } }
    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> if (result.resultCode == android.app.Activity.RESULT_OK) result.data?.data?.let { uri -> runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(csvContent.toByteArray(Charsets.UTF_8)) } ?: error("No se pudo abrir el archivo de destino") }.onSuccess { Toast.makeText(context, "CSV exportado", Toast.LENGTH_SHORT).show() }.onFailure { Toast.makeText(context, "No se pudo exportar el CSV", Toast.LENGTH_LONG).show() } } }
    val backupExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> if (result.resultCode == android.app.Activity.RESULT_OK) result.data?.data?.let { uri -> runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(backupContent.toByteArray(Charsets.UTF_8)) } ?: error("No se pudo abrir el archivo de destino") }.onSuccess { Toast.makeText(context, "Copia creada", Toast.LENGTH_SHORT).show() }.onFailure { Toast.makeText(context, "No se pudo crear la copia", Toast.LENGTH_LONG).show() } } }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> if (result.resultCode == android.app.Activity.RESULT_OK) result.data?.data?.let { uri -> runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(ReceiptPdfExporter.export(receipts)) } ?: error("No se pudo abrir el archivo de destino") }.onSuccess { Toast.makeText(context, "PDF exportado", Toast.LENGTH_SHORT).show() }.onFailure { Toast.makeText(context, "No se pudo exportar el PDF", Toast.LENGTH_LONG).show() } } }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { ReceiptBackup.import(it.readText()) } ?: error("No se pudo leer la copia") }.onSuccess { pendingRestore = it }.onFailure { Toast.makeText(context, "Copia no válida: ${it.message ?: "error desconocido"}", Toast.LENGTH_LONG).show() } }
    val fullRestoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    ReceiptFullBackup.`import`(input, File(context.filesDir, "receipts"))
                } ?: error("No se pudo leer la copia")
            }.onSuccess { restored -> scope.launch(Dispatchers.Main) { pendingFullRestore = restored } }
                .onFailure { error -> scope.launch(Dispatchers.Main) { Toast.makeText(context, "Copia no válida: ${error.message ?: "error desconocido"}", Toast.LENGTH_LONG).show() } }
        }
    }
    pendingRestore?.let { restored -> AlertDialog(onDismissRequest = { pendingRestore = null }, title = { Text("Restaurar copia", fontWeight = FontWeight.Bold) }, text = { Text("Se añadirán ${restored.size} tickets a los existentes. Las imágenes no se incluyen en esta copia. ¿Continuar?") }, confirmButton = { TextButton(onClick = { onRestore(restored); pendingRestore = null; Toast.makeText(context, "Restaurados ${restored.size} tickets", Toast.LENGTH_SHORT).show() }) { Text("Restaurar") } }, dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancelar") } }) }
    pendingFullRestore?.let { restored -> AlertDialog(onDismissRequest = { restored.cleanupImages(); pendingFullRestore = null }, title = { Text("Restaurar copia completa", fontWeight = FontWeight.Bold) }, text = { Text("Se añadirán ${restored.receipts.size} tickets y ${restored.restoredImages} imágenes a los existentes${if (restored.missingImages > 0) "; ${restored.missingImages} imágenes no disponibles" else ""}. ¿Continuar?") }, confirmButton = { TextButton(onClick = { onRestoreFull(restored.receipts); pendingFullRestore = null; Toast.makeText(context, if (restored.missingImages == 0) "Restaurados ${restored.receipts.size} tickets con sus imágenes" else "Restaurados ${restored.receipts.size} tickets; ${restored.missingImages} imágenes no disponibles", Toast.LENGTH_LONG).show() }) { Text("Restaurar") } }, dismissButton = { TextButton(onClick = { restored.cleanupImages(); pendingFullRestore = null }) { Text("Cancelar") } }) }
    Scaffold(topBar = { TopAppBar(title = { Text("Ajustes", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), contentPadding = PaddingValues(bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Spacer(Modifier.height(4.dp)); Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_LARGE), color = MaterialTheme.colorScheme.primaryContainer, tonalElevation = 3.dp) { Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Surface(shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_MEDIUM), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) { Icon(Icons.Default.Settings, null, Modifier.padding(11.dp).size(27.dp), tint = MaterialTheme.colorScheme.primary) }; Column(Modifier.padding(start = 14.dp)) { Text("Personaliza ReceiptBox", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Aspecto, datos y exportaciones", color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
            item { Text("Apariencia", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_LARGE), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) { Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (darkTheme) Icons.Default.DarkMode else Icons.Default.LightMode, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary); Column(Modifier.weight(1f).padding(horizontal = 14.dp)) { Text("Modo oscuro", fontWeight = FontWeight.SemiBold); Text(if (darkTheme) "Activado" else "Desactivado", color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(checked = darkTheme, onCheckedChange = onDarkThemeChanged) } } }
            item { Text("Aplicación", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_LARGE), color = MaterialTheme.colorScheme.surfaceVariant) { Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { SettingRow(Icons.Default.Category, "Categorías", "Alimentación, Hogar, Transporte y más"); Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Info, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary); Column(Modifier.weight(1f).padding(horizontal = 14.dp)) { Text("Versión", fontWeight = FontWeight.SemiBold); Text("0.2.0", color = MaterialTheme.colorScheme.onSurfaceVariant) } } } } }
            item { Text("Datos y exportaciones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_LARGE), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) { Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { SettingAction(Icons.Default.Backup, "Copia JSON", "Datos de tickets; no incluye imágenes") { backupExportLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { type = "application/json"; putExtra(Intent.EXTRA_TITLE, "receiptbox-backup.json") }) }; SettingAction(Icons.Default.FolderZip, "Copia completa", "ZIP · tickets + imágenes disponibles") { scope.launch(Dispatchers.IO) { val bytes = receipts) { path -> readBackupImage(path) }; scope.launch(Dispatchers.Main) { pendingFullBackupBytes = bytes; fullBackupExportLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { type = "application/zip"; putExtra(Intent.EXTRA_TITLE, "receiptbox-full-backup.zip") }) } } }; SeReceiptFullBackup.`export`(ttingAction(Icons.Default.Restore, "Restaurar JSON", "Añade los tickets de una copia JSON") { restoreLauncher.launch(arrayOf("application/json", "text/*")) }; SettingAction(Icons.Default.FolderZip, "Restaurar copia completa", "Restaura tickets e imágenes disponibles") { fullRestoreLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }; SettingAction(Icons.Default.PictureAsPdf, "Exportar PDF", "Genera un informe imprimible") { pdfLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { type = "application/pdf"; putExtra(Intent.EXTRA_TITLE, "receiptbox-report.pdf") }) }; SettingAction(Icons.Default.TableChart, "Exportar CSV", "Exporta los datos para Excel u otras hojas") { csvExportLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { type = "text/csv"; putExtra(Intent.EXTRA_TITLE, "receiptbox-export.csv") }) } } } }
            item { Text("Privacidad", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_LARGE), color = MaterialTheme.colorScheme.surfaceVariant) { Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.Top) { Icon(Icons.Default.Lock, null, Modifier.size(27.dp), tint = MaterialTheme.colorScheme.primary); Column(Modifier.padding(start = 14.dp)) { Text("Tus tickets se almacenan localmente", fontWeight = FontWeight.SemiBold); Text("ReceiptBox no necesita una cuenta para conservar tus datos en el dispositivo.", color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
        }
    }
}

private fun readBackupImage(path: String): ByteArray? {
    val file = File(path)
    if (!file.isFile) return null
    if (file.length() > MAX_BACKUP_IMAGE_BYTES) {
        throw IllegalArgumentException("Una imagen de la copia supera el tamaño máximo permitido")
    }
    return FileInputStream(file).use { input ->
        val bytes = ByteArray(file.length().toInt())
        var offset = 0
        while (offset < bytes.size) {
            val count = input.read(bytes, offset, bytes.size - offset)
            if (count < 0) break
            offset += count
        }
        if (offset == bytes.size) bytes else bytes.copyOf(offset)
    }
}

@Composable private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary); Column(Modifier.padding(start = 14.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun SettingAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) { Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(ReceiptBoxDesign.CORNER_MEDIUM), color = MaterialTheme.colorScheme.surfaceVariant) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary); Column(Modifier.weight(1f).padding(horizontal = 14.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } } }
