package com.pagrey.receiptbox.ui

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.pagrey.receiptbox.data.Receipt
import com.pagrey.receiptbox.ocr.OcrResult
import com.pagrey.receiptbox.ocr.PocketScanImageEnhancer
import com.pagrey.receiptbox.ocr.ReceiptOcrProcessor
import com.pagrey.receiptbox.util.parseReceiptAmount
import java.io.File

private val scannerCategories = listOf("Alimentación", "Hogar", "Transporte", "Salud", "Tecnología", "Ocio", "Ropa", "Otros")

@Composable
fun DocumentScannerReceiptScreen(
    viewModel: ReceiptBoxViewModel,
    onSaved: (Long) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var imageFile by remember { mutableStateOf<File?>(null) }
    var ocr by remember { mutableStateOf<OcrResult?>(null) }
    var processing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var scannerStarted by remember { mutableStateOf(false) }

    val options = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }
    val scanner = remember { GmsDocumentScanning.getClient(options) }
    val launcher = rememberLauncherForActivityResult(StartIntentSenderForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            onCancel()
            return@rememberLauncherForActivityResult
        }
        val scanResult = result.data?.let { GmsDocumentScanningResult.fromActivityResultIntent(it) }
        val uri = scanResult?.pages?.firstOrNull()?.imageUri
        if (uri == null) {
            error = "No se ha podido obtener la imagen escaneada."
        } else {
            imageFile = copyScannerUriToFile(context, uri)
            if (imageFile == null) error = "No se ha podido guardar el escaneo."
        }
    }

    LaunchedEffect(activity) {
        if (activity == null || scannerStarted || imageFile != null) return@LaunchedEffect
        scannerStarted = true
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                launcher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { throwable ->
                error = throwable.message ?: "No se ha podido iniciar el escáner."
                scannerStarted = false
            }
    }

    LaunchedEffect(imageFile) {
        val file = imageFile ?: return@LaunchedEffect
        processing = true
        error = null
        val processor = ReceiptOcrProcessor()
        ocr = processor.process(context, file).getOrElse {
            error = it.message ?: "No se ha podido analizar el ticket."
            null
        }
        processor.close()
        processing = false
    }

    when {
        imageFile == null && error == null -> {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Text("Preparando escáner de tickets…", modifier = Modifier.padding(top = ReceiptBoxDesign.ITEM_SPACING))
                Text("Detectará, recortará y mejorará automáticamente el ticket.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = ReceiptBoxDesign.COMPACT_SPACING))
                Button(onClick = onCancel, modifier = Modifier.padding(top = ReceiptBoxDesign.SECTION_SPACING).height(ReceiptBoxDesign.PRIMARY_BUTTON_HEIGHT)) { Text("Cancelar") }
            }
        }
        imageFile == null -> {
            Column(Modifier.fillMaxSize().padding(ReceiptBoxDesign.SCREEN_PADDING), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("No se ha podido iniciar el escáner.", style = MaterialTheme.typography.titleLarge)
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = ReceiptBoxDesign.COMPACT_SPACING))
                Button(onClick = onCancel, modifier = Modifier.padding(top = ReceiptBoxDesign.SECTION_SPACING).height(ReceiptBoxDesign.PRIMARY_BUTTON_HEIGHT)) { Text("Volver") }
            }
        }
        processing -> {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Text("Analizando ticket…", modifier = Modifier.padding(top = ReceiptBoxDesign.ITEM_SPACING))
            }
        }
        else -> ScannerReviewReceipt(ocr, imageFile!!, error, onCancel) { edited -> viewModel.save(edited, onSaved) }
    }
}

@Composable
private fun ScannerReviewReceipt(
    ocr: OcrResult?,
    file: File,
    error: String?,
    onCancel: () -> Unit,
    onSave: (Receipt) -> Unit
) {
    val parsed = ocr?.parsed
    var merchant by remember(parsed) { mutableStateOf(parsed?.merchant.orEmpty()) }
    var date by remember(parsed) { mutableStateOf(parsed?.date.orEmpty()) }
    var total by remember(parsed) { mutableStateOf(parsed?.total?.toString().orEmpty()) }
    var tax by remember(parsed) { mutableStateOf(parsed?.tax?.toString().orEmpty()) }
    var number by remember(parsed) { mutableStateOf(parsed?.receiptNumber.orEmpty()) }
    var category by remember(parsed) { mutableStateOf(scannerSuggestCategory(parsed?.merchant.orEmpty())) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val totalValue = parseReceiptAmount(total)
    val merchantInvalid = merchant.isBlank() || merchant.trim().length < 2
    val totalInvalid = totalValue == null || totalValue <= 0.0
    val dateInvalid = date.isBlank()
    val missing = listOf(merchantInvalid, dateInvalid, totalInvalid).count { it }
    val bitmap = remember(file.absolutePath) { BitmapFactory.decodeFile(file.absolutePath) }
    val scrollState = rememberScrollState()

    Column(Modifier.fillMaxSize().verticalScroll(scrollState).imePadding().padding(ReceiptBoxDesign.SCREEN_PADDING)) {
        Text("Revisar ticket", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Comprueba los datos detectados antes de guardarlo. Puedes corregir cualquier campo.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = ReceiptBoxDesign.COMPACT_SPACING))

        if (bitmap != null) {
            Spacer(Modifier.height(ReceiptBoxDesign.ITEM_SPACING))
            Card(Modifier.fillMaxWidth()) {
                Image(bitmap.asImageBitmap(), "Ticket escaneado", Modifier.fillMaxWidth().height(ReceiptBoxDesign.PREVIEW_HEIGHT).padding(ReceiptBoxDesign.REVIEW_CARD_PADDING))
            }
        }

        Spacer(Modifier.height(ReceiptBoxDesign.SECTION_SPACING))
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(ReceiptBoxDesign.REVIEW_CARD_PADDING), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (missing == 0) Icons.Default.CheckCircle else Icons.Default.WarningAmber, null, Modifier.size(28.dp), tint = if (missing == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(ReceiptBoxDesign.COMPACT_SPACING))
                Column(Modifier.weight(1f)) {
                    Text(if (missing == 0) "Datos detectados" else "Revisión necesaria", fontWeight = FontWeight.SemiBold)
                    Text(if (missing == 0) "Los campos principales están completos." else "Hay $missing campos que necesitan revisión.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = ReceiptBoxDesign.ITEM_SPACING))

        Text("Datos principales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = ReceiptBoxDesign.SECTION_SPACING))
        OutlinedTextField(merchant, { merchant = it }, Modifier.fillMaxWidth().padding(top = ReceiptBoxDesign.COMPACT_SPACING), label = { Text("Comercio") }, singleLine = true, isError = merchantInvalid)
        OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth().padding(top = ReceiptBoxDesign.FIELD_SPACING), label = { Text("Fecha") }, supportingText = { Text("Ejemplo: 13/09/2026") }, singleLine = true, isError = dateInvalid)
        OutlinedTextField(total, { total = it }, Modifier.fillMaxWidth().padding(top = ReceiptBoxDesign.FIELD_SPACING), label = { Text("Total") }, singleLine = true, isError = totalInvalid)
        if (totalInvalid) Text("Introduce un total válido mayor que 0 €.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

        Text("Información adicional", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = ReceiptBoxDesign.SECTION_SPACING))
        OutlinedTextField(tax, { tax = it }, Modifier.fillMaxWidth().padding(top = ReceiptBoxDesign.COMPACT_SPACING), label = { Text("IVA") }, singleLine = true)
        OutlinedTextField(number, { number = it }, Modifier.fillMaxWidth().padding(top = ReceiptBoxDesign.FIELD_SPACING), label = { Text("N.º de ticket") }, singleLine = true)
        Spacer(Modifier.height(ReceiptBoxDesign.FIELD_SPACING))
        androidx.compose.foundation.layout.Box {
            Button(onClick = { categoryExpanded = true }, modifier = Modifier.fillMaxWidth().height(ReceiptBoxDesign.PRIMARY_BUTTON_HEIGHT)) { Text("Categoría: $category") }
            DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                scannerCategories.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { category = item; categoryExpanded = false }) }
            }
        }

        Spacer(Modifier.height(ReceiptBoxDesign.SECTION_SPACING))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ReceiptBoxDesign.COMPACT_SPACING)) {
            Button(onClick = onCancel, modifier = Modifier.weight(1f).height(ReceiptBoxDesign.PRIMARY_BUTTON_HEIGHT)) { Text("Cancelar") }
            Button(
                onClick = {
                    val parsedTotal = parseReceiptAmount(total)
                    if (!merchantInvalid && !dateInvalid && parsedTotal != null && parsedTotal > 0.0) {
                        onSave(Receipt(merchant = merchant.trim(), date = date.trim(), total = parsedTotal, tax = parseReceiptAmount(tax), receiptNumber = number.trim(), category = category, imagePath = file.absolutePath, rawText = ocr?.rawText.orEmpty()))
                    }
                },
                enabled = !merchantInvalid && !dateInvalid && !totalInvalid,
                modifier = Modifier.weight(1f).height(ReceiptBoxDesign.PRIMARY_BUTTON_HEIGHT)
            ) { Text("Guardar") }
        }
        Spacer(Modifier.height(ReceiptBoxDesign.ITEM_SPACING))
    }
}

private fun scannerSuggestCategory(merchant: String): String {
    val value = merchant.lowercase()
    return when {
        listOf("mercadona", "carrefour", "dia", "lidl", "aldi", "alcampo", "supermercado", "market", "grocery", "panaderia", "panadería", "carniceria", "carnicería").any(value::contains) -> "Alimentación"
        listOf("ikea", "leroy", "bricomart", "ferreteria", "ferretería", "hogar", "muebles").any(value::contains) -> "Hogar"
        listOf("uber", "cabify", "repsol", "cepsa", "gasolinera", "parking", "aparcam").any(value::contains) -> "Transporte"
        listOf("farmacia", "hospital", "clinica", "clínica", "salud", "dentista", "optica", "óptica").any(value::contains) -> "Salud"
        listOf("mediamarkt", "media markt", "pccomponentes", "apple", "fnac", "informatica", "informática", "electro").any(value::contains) -> "Tecnología"
        listOf("cine", "teatro", "spotify", "netflix", "ocio", "parque").any(value::contains) -> "Ocio"
        listOf("zara", "h&m", "primark", "mango", "decathlon", "sprinter", "ropa").any(value::contains) -> "Ropa"
        else -> "Otros"
    }
}

private fun copyScannerUriToFile(context: Context, uri: Uri): File? = runCatching {
    val dir = File(context.filesDir, "receipts").apply { mkdirs() }
    val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)!!.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
    val enhanced = File(dir, file.nameWithoutExtension + "_ocr.jpg")
    if (PocketScanImageEnhancer.enhanceToJpeg(context, Uri.fromFile(file), enhanced)) {
        if (file.delete()) enhanced.renameTo(file) else enhanced.delete()
    } else {
        enhanced.delete()
    }
    file
}.getOrNull()
