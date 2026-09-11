package com.pagrey.receiptbox.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.pagrey.receiptbox.data.Receipt
import com.pagrey.receiptbox.ocr.OcrResult
import com.pagrey.receiptbox.ocr.ReceiptOcrProcessor
import com.pagrey.receiptbox.util.parseReceiptAmount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun AddReceiptScreen(viewModel: ReceiptBoxViewModel, onSaved: (Long) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    var imageFile by remember { mutableStateOf<File?>(null) }
    var ocr by remember { mutableStateOf<OcrResult?>(null) }
    var processing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            copyUriToFile(context, uri)?.let { imageFile = it }
                ?: run { error = "No se ha podido importar la imagen." }
        }
    }

    LaunchedEffect(imageFile) {
        val file = imageFile ?: return@LaunchedEffect
        processing = true
        error = null
        val processor = ReceiptOcrProcessor()
        val bitmap = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file.absolutePath) }
        if (bitmap == null) {
            error = "No se ha podido leer la imagen."
        } else {
            ocr = processor.process(bitmap).getOrElse { error = it.message ?: "No se ha podido analizar el ticket."; null }
        }
        processor.close()
        processing = false
    }

    if (imageFile == null) {
        CameraCapture(
            onCaptured = { imageFile = it },
            onGallery = { gallery.launch("image/*") },
            onCancel = onCancel
        )
    } else if (processing) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
            Text("Analizando ticket…", modifier = Modifier.padding(top = 16.dp))
        }
    } else {
        ReviewReceipt(ocr, imageFile!!, error, onCancel) { edited -> viewModel.save(edited, onSaved) }
    }
}

@Composable
private fun ReviewReceipt(ocr: OcrResult?, file: File, error: String?, onCancel: () -> Unit, onSave: (Receipt) -> Unit) {
    val parsed = ocr?.parsed
    var merchant by remember(parsed) { mutableStateOf(parsed?.merchant.orEmpty()) }
    var date by remember(parsed) { mutableStateOf(parsed?.date.orEmpty()) }
    var total by remember(parsed) { mutableStateOf(parsed?.total?.toString().orEmpty()) }
    var tax by remember(parsed) { mutableStateOf(parsed?.tax?.toString().orEmpty()) }
    var number by remember(parsed) { mutableStateOf(parsed?.receiptNumber.orEmpty()) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Revisar ticket", style = MaterialTheme.typography.headlineMedium)
        Text("Comprueba los datos detectados antes de guardarlo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(merchant, { merchant = it }, Modifier.fillMaxWidth(), label = { Text("Comercio") }, singleLine = true)
        OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Fecha") }, singleLine = true)
        OutlinedTextField(total, { total = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Total") }, singleLine = true)
        OutlinedTextField(tax, { tax = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("IVA") }, singleLine = true)
        OutlinedTextField(number, { number = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("N.º de ticket") }, singleLine = true)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancelar") }
            Button(onClick = {
                onSave(Receipt(merchant = merchant.trim(), date = date.trim(), total = parseReceiptAmount(total), tax = parseReceiptAmount(tax), receiptNumber = number.trim(), imagePath = file.absolutePath, rawText = ocr?.rawText.orEmpty()))
            }, modifier = Modifier.weight(1f)) { Text("Guardar") }
        }
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Text("OCR original", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            Text(ocr?.rawText?.ifBlank { "Sin texto detectado" } ?: "Sin texto detectado", modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
        }
    }
}

@Composable
private fun CameraCapture(onCaptured: (File) -> Unit, onGallery: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) { if (!hasPermission) permission.launch(Manifest.permission.CAMERA) }

    if (!hasPermission) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Necesitamos acceso a la cámara para fotografiar el ticket.")
            Button(onClick = { permission.launch(Manifest.permission.CAMERA) }, modifier = Modifier.padding(top = 16.dp)) { Text("Permitir cámara") }
            Button(onClick = onGallery, modifier = Modifier.padding(top = 8.dp)) { Text("Importar imagen") }
            Button(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) { Text("Cancelar") }
        }
        return
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            runCatching {
                val provider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            }.onFailure { cameraError = "No se ha podido iniciar la cámara." }
        }, ContextCompat.getMainExecutor(context))
        onDispose { runCatching { cameraProviderFuture.get().unbindAll() } }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Column(Modifier.align(Alignment.BottomCenter).padding(24.dp)) {
            if (cameraError != null) Text(cameraError!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onGallery) { Text("Importar") }
                Button(onClick = {
                    val file = File(context.filesDir, "receipts").apply { mkdirs() }.let { File(it, "receipt_${System.currentTimeMillis()}.jpg") }
                    imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) { onCaptured(file) }
                        override fun onError(exception: ImageCaptureException) { cameraError = "No se ha podido guardar la foto." }
                    })
                }) { Text("Fotografiar") }
            }
            Button(onClick = onCancel, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Cancelar") }
        }
    }
}

private fun copyUriToFile(context: Context, uri: android.net.Uri): File? {
    return runCatching {
        val dir = File(context.filesDir, "receipts").apply { mkdirs() }
        val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)!!.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
        file
    }.getOrNull()
}
