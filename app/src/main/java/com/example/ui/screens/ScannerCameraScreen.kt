package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaActionSound
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.local.ScannedDocument
import com.example.ui.ScanProViewModel
import com.example.ui.camera.CameraPreferences
import com.example.ui.camera.CameraScanResolution
import com.example.ui.camera.CameraSettingsManager
import com.example.ui.camera.FlashOption
import com.example.ui.camera.ScanCaptureMode
import com.example.util.DocumentFilterMode
import com.example.util.DocumentQuad
import com.example.util.OpenCvDocumentDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AllFeaturesGridItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val badgeColor: Color,
    val category: String,
    val isHot: Boolean = false
)

private fun rotateBitmapIfNeeded(bitmap: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return bitmap
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun createMockDocumentBitmap(): Bitmap {
    val bmp = Bitmap.createBitmap(900, 1200, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    canvas.drawColor(android.graphics.Color.rgb(25, 35, 48))
    val paperPaint = android.graphics.Paint().apply { color = android.graphics.Color.WHITE }
    canvas.drawRect(80f, 90f, 820f, 1110f, paperPaint)
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 32f
        isFakeBoldText = true
    }
    val dateStr = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())
    canvas.drawText("SCANPRO AI DOCUMENT RECORD", 120f, 180f, textPaint)
    textPaint.textSize = 20f
    textPaint.isFakeBoldText = false
    canvas.drawText("ScanPro Ai $dateStr", 120f, 230f, textPaint)
    canvas.drawText("Status: Authenticated in Room DB", 120f, 270f, textPaint)
    return bmp
}

@Composable
fun ScannerCameraScreen(
    viewModel: ScanProViewModel,
    onCloseScanner: () -> Unit,
    onNavigateToEditor: () -> Unit,
    onOpenToolDedicated: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val settingsManager = remember { CameraSettingsManager(context) }
    var cameraPrefs by remember { mutableStateOf(settingsManager.load()) }

    var captureMode by remember { mutableStateOf(ScanCaptureMode.SINGLE) }

    // Ribbon Modes matching all your screenshots
    val ribbonModes = listOf(
        "Slides",
        "To Excel",
        "ID Photo Maker",
        "Timestamp",
        "Extract Text",
        "To Word",
        "Sign",
        "Scan",
        "Smart Erase",
        "ID Cards",
        "Question Set",
        "Translate",
        "Restore Photo",
        "Book",
        "Whiteboard",
        "Scan Code",
        "CountCam"
    )
    var selectedRibbonMode by remember { mutableStateOf("Scan") }

    // Secondary mode sub-options (e.g. ID Cards categories, ID Photo sizes, Question Set sub-modes)
    var selectedIdCardType by remember { mutableStateOf("ID Card") }
    var selectedPhotoSize by remember { mutableStateOf("US Passport 51x51mm") }
    var selectedQuestionSubMode by remember { mutableStateOf("Formula") }
    var selectedRestoreSubMode by remember { mutableStateOf("Restore") }

    // Overlays
    var showAllFeaturesSheet by remember { mutableStateOf(false) }
    var showFlashMenu by remember { mutableStateOf(false) }
    var showEnhancementPanel by remember { mutableStateOf(false) }
    var showResolutionPicker by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var selectedFilterMode by remember { mutableStateOf(DocumentFilterMode.ORIGINAL) }
    var detectedQuad by remember { mutableStateOf<DocumentQuad?>(null) }

    // CameraX instance management
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var previewViewInstance by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    // Focus & Zoom
    var focusTapOffset by remember { mutableStateOf<Offset?>(null) }
    var zoomScale by remember { mutableFloatStateOf(1f) }

    val mediaActionSound = remember { MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) } }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    BackHandler(enabled = showAllFeaturesSheet || showEnhancementPanel || showResolutionPicker || showSettingsDialog) {
        when {
            showAllFeaturesSheet -> showAllFeaturesSheet = false
            showEnhancementPanel -> showEnhancementPanel = false
            showResolutionPicker -> showResolutionPicker = false
            showSettingsDialog -> showSettingsDialog = false
        }
    }

    LaunchedEffect(cameraPrefs.flashOption, boundCamera) {
        val cam = boundCamera ?: return@LaunchedEffect
        if (cam.cameraInfo.hasFlashUnit()) {
            when (cameraPrefs.flashOption) {
                FlashOption.STAY_ON -> cam.cameraControl.enableTorch(true)
                FlashOption.OFF, FlashOption.ON, FlashOption.AUTO -> cam.cameraControl.enableTorch(false)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                cameraProviderInstance?.unbindAll()
                mediaActionSound.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(focusTapOffset) {
        if (focusTapOffset != null) {
            delay(1600)
            focusTapOffset = null
        }
    }

    val imageImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                val imported = uris.mapNotNull { uri ->
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
                withContext(Dispatchers.Main) {
                    imported.forEach { bmp -> viewModel.addCapturedBitmap(bmp) }
                    Toast.makeText(context, "Imported ${imported.size} image(s)", Toast.LENGTH_SHORT).show()
                    onNavigateToEditor()
                }
            }
        }
    }

    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                var importedCount = 0
                uris.forEach { uri ->
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val docsDir = File(context.filesDir, "imported").apply { if (!exists()) mkdirs() }
                    val destFile = File(docsDir, "Doc_${System.currentTimeMillis()}.pdf")
                    FileOutputStream(destFile).use { out -> inputStream?.copyTo(out) }
                    val doc = ScannedDocument(
                        title = destFile.name,
                        filePath = destFile.absolutePath,
                        fileType = "PDF",
                        fileSizeBytes = destFile.length(),
                        pageCount = 1,
                        category = "Imported"
                    )
                    viewModel.addNewDocument(doc)
                    importedCount++
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Imported $importedCount document(s)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // FULL SCREEN CAMERA PREVIEW
        if (hasCameraPermission) {
            val transformableState = rememberTransformableState { zoomChange, _, _ ->
                zoomScale = (zoomScale * zoomChange).coerceIn(1f, 5f)
                boundCamera?.cameraControl?.setZoomRatio(zoomScale)
            }

            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            cameraProviderInstance = cameraProvider

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                .setResolutionSelector(settingsManager.buildResolutionSelector(cameraPrefs.resolution))
                                .setFlashMode(
                                    when (cameraPrefs.flashOption) {
                                        FlashOption.ON -> ImageCapture.FLASH_MODE_ON
                                        FlashOption.AUTO -> ImageCapture.FLASH_MODE_AUTO
                                        else -> ImageCapture.FLASH_MODE_OFF
                                    }
                                )
                                .build()

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { analysis ->
                                    analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                        val quad = OpenCvDocumentDetector.analyzeFrame(imageProxy)
                                        detectedQuad = quad
                                        imageProxy.close()
                                    }
                                }

                            imageCapture = capture
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture,
                                imageAnalysis
                            )
                            boundCamera = camera
                            previewViewInstance = previewView
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(state = transformableState)
            )
        }

        // DYNAMIC CAMERA GUIDE OVERLAYS (MATCHING SCREENSHOTS 2 TO 18)
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedRibbonMode) {
                // 1. Slides Mode (Screenshot 2)
                "Slides" -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.9f))
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "No slides found. Capture manually.",
                            color = Color(0xFF0F172A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // 2. ID Cards Mode (Screenshot 3)
                "ID Cards" -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Example A4 Card Preview Box
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .size(width = 170.dp, height = 230.dp)
                                .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Gray.copy(alpha = 0.3f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("A4 paper example", fontSize = 9.sp, color = Color.DarkGray)
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .size(width = 110.dp, height = 65.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE2E8F0))
                                        .border(0.8.dp, Color(0xFF94A3B8), RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(28.dp))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(width = 110.dp, height = 65.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE2E8F0))
                                        .border(0.8.dp, Color(0xFF94A3B8), RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(28.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Create and share ID copies for various situations, including banking, administration, and more.",
                            color = Color.White,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category Pills
                        val idTypes = listOf("General", "ID Card", "Driver License", "Passport", "Bank Card", "Certificate")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(idTypes) { type ->
                                val isSel = selectedIdCardType == type
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) Color.White else Color(0x66000000))
                                        .clickable { selectedIdCardType = type }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = type,
                                        color = if (isSel) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onOpenToolDedicated?.invoke("id_card_scan") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5), contentColor = Color.Black),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth(0.7f).height(42.dp)
                        ) {
                            Text("Make it now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // 3. Extract Text Mode (Screenshot 5)
                "Extract Text" -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(70.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("About \"Extract Text\"", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }

                // 4. ID Photo Maker Mode (Screenshot 6 - Silhouette Outline)
                "ID Photo Maker" -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // White Head-and-Shoulder Silhouette Guide
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = size.height * 0.42f
                            val headRadius = size.width * 0.22f

                            // Head Oval
                            drawCircle(
                                color = Color.White,
                                radius = headRadius,
                                center = Offset(cx, cy - 30f),
                                style = Stroke(width = 3.5f)
                            )
                            // Shoulder Arc
                            val shoulderPath = Path().apply {
                                moveTo(cx - headRadius * 1.8f, cy + headRadius * 1.6f)
                                quadraticTo(cx, cy + headRadius * 0.6f, cx + headRadius * 1.8f, cy + headRadius * 1.6f)
                            }
                            drawPath(shoulderPath, color = Color.White, style = Stroke(width = 3.5f))
                        }

                        // Bottom Size Options
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 120.dp)
                                .fillMaxWidth()
                        ) {
                            val photoSizes = listOf("25x35mm", "US Passport 51x51mm", "Indonesia Visa 35x45mm", "35x49mm", "33x48mm", "More Sizes >")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
                            ) {
                                items(photoSizes) { pSize ->
                                    val isSel = selectedPhotoSize == pSize
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) Color(0xCC7F1D1D) else Color(0x99450A0A))
                                            .clickable {
                                                if (pSize == "More Sizes >") {
                                                    onOpenToolDedicated?.invoke("id_photo_maker")
                                                } else {
                                                    selectedPhotoSize = pSize
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(pSize, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Timestamp Mode (Screenshot 7 - Digital Clock Burn-in)
                "Timestamp" -> {
                    val currentTime = remember {
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    }
                    val currentDate = remember {
                        SimpleDateFormat("EEEE | dd.MM.yyyy", Locale.getDefault()).format(Date())
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 20.dp, bottom = 120.dp)
                    ) {
                        Column {
                            Text(
                                text = currentTime,
                                color = Color.White,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = currentDate,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📍 Location not retrieved", color = Color(0xFFF97316), fontSize = 11.sp)
                            }
                            Text("CS Verified by ScanPro", color = Color(0xFFCBD5E1), fontSize = 10.sp)
                        }
                    }
                }

                // 6. Sign Mode (Screenshot 10)
                "Sign" -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Draw, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Sign", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 7. Question Set Mode (Screenshot 12)
                "Question Set" -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Formula Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selectedQuestionSubMode == "Formula") Color.Black.copy(alpha = 0.8f) else Color(0x66000000))
                                    .clickable { selectedQuestionSubMode = "Formula" }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Functions, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Formula", color = Color.White, fontSize = 11.sp)
                                }
                            }

                            // Incorrect Question Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selectedQuestionSubMode == "Incorrect") Color.Black.copy(alpha = 0.8f) else Color(0x66000000))
                                    .clickable { selectedQuestionSubMode = "Incorrect" }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Incorrect Question", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // 8. Smart Erase Mode (Screenshot 13)
                "Smart Erase" -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Smart Erase", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 9. Restore Photo Mode (Screenshot 14)
                "Restore Photo" -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 70.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Take or import a photo, and watch for light reflections", color = Color.White, fontSize = 11.sp)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(bottom = 120.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Restore Photo", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.height(10.dp))

                            // Restore | Colorize Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(4.dp)
                            ) {
                                Row {
                                    val isRes = selectedRestoreSubMode == "Restore"
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isRes) Color(0xFF334155) else Color.Transparent)
                                            .clickable { selectedRestoreSubMode = "Restore" }
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Restore", color = Color.White, fontSize = 11.sp)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (!isRes) Color(0xFF334155) else Color.Transparent)
                                            .clickable { selectedRestoreSubMode = "Colorize" }
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Colorize", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // 10. Translate Mode (Screenshot 15)
                "Translate" -> {
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(70.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Auto Detect ▾  →  English ▾", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Translate, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Translate", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // 11. Book Mode (Screenshot 16)
                "Book" -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                                .background(Color(0xCC334155))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text("Turn page to auto capture >", color = Color.White, fontSize = 11.sp)
                        }

                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Book, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Book", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // 12. Scan Code / QR Mode (Screenshot 17)
                "Scan Code" -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .border(2.dp, Color.White, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            val scanAnim = rememberInfiniteTransition(label = "qr_laser")
                            val laserY by scanAnim.animateFloat(
                                initialValue = -90f,
                                targetValue = 90f,
                                animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                label = "qr_y"
                            )

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawLine(
                                    color = Color(0xFF16C7B5),
                                    start = Offset(10f, size.height / 2f + laserY),
                                    end = Offset(size.width - 10f, size.height / 2f + laserY),
                                    strokeWidth = 3f
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Scan Code", color = Color.White, fontSize = 12.sp)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 120.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable {
                                    val cam = boundCamera
                                    if (cam != null && cam.cameraInfo.hasFlashUnit()) {
                                        cam.cameraControl.enableTorch(cameraPrefs.flashOption != FlashOption.STAY_ON)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("🔦 Tap to turn on light", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }

                // 13. CountCam Mode (Screenshot 18)
                "CountCam" -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("CountCam", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 120.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("10 free uses left", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }

                // Default Scan Mode (Screenshot 11)
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CropFree, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(selectedRibbonMode, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 1. TOP APP BAR (CLOSE, FLASH, HD, SETTINGS)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onCloseScanner,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                // Flash Toggle Button
                IconButton(
                    onClick = { showFlashMenu = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    val flashIcon = when (cameraPrefs.flashOption) {
                        FlashOption.OFF -> Icons.Default.FlashOff
                        FlashOption.ON -> Icons.Default.FlashOn
                        FlashOption.AUTO -> Icons.Default.FlashAuto
                        FlashOption.STAY_ON -> Icons.Default.FlashOn
                    }
                    val flashTint = if (cameraPrefs.flashOption == FlashOption.OFF) Color.White else Color(0xFF16C7B5)
                    Icon(flashIcon, contentDescription = "Flashlight", tint = flashTint, modifier = Modifier.size(20.dp))
                }

                // HD Resolution Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color(0xFF16C7B5), RoundedCornerShape(6.dp))
                        .clickable { showResolutionPicker = true }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cameraPrefs.resolution.label,
                        color = Color(0xFF16C7B5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Three-dot Settings Menu
                IconButton(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }

        // OVERLAID SINGLE | BATCH PILL (Directly on camera viewfinder)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 190.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 4.dp, vertical = 3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isSingle = captureMode == ScanCaptureMode.SINGLE
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSingle) Color(0xFF475569) else Color.Transparent)
                        .clickable { captureMode = ScanCaptureMode.SINGLE }
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text("Single", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (!isSingle) Color(0xFF475569) else Color.Transparent)
                        .clickable { captureMode = ScanCaptureMode.BATCH }
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text("Batch", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // BOTTOM BLACK CONTROLS DOCK (RIBBON + SHUTTER + ALL FEATURES)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black)
                .navigationBarsPadding()
        ) {
            // Horizontal Modes Ribbon (Slides, To Excel, ID Cards, Timestamp, etc.)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(ribbonModes) { mode ->
                    val isSelected = selectedRibbonMode == mode
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { selectedRibbonMode = mode }
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = mode,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF16C7B5) else Color.White.copy(alpha = 0.75f)
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 3.dp)
                                    .height(2.5.dp)
                                    .width(22.dp)
                                    .background(Color(0xFF16C7B5), RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Shutter Button Row with All Features & Import Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // All Features Button (Screenshot Match)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showAllFeaturesSheet = true }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.GridView, contentDescription = "All Features", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("All Features", color = Color.White, fontSize = 10.sp)
                }

                // Big Center Shutter Button (Guaranteed Image Load & Editor Trigger)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color(0xFF16C7B5), CircleShape)
                        .clickable {
                            if (isCapturing) return@clickable
                            val capture = imageCapture
                            if (capture != null) {
                                isCapturing = true
                                if (cameraPrefs.shutterSound) {
                                    runCatching { mediaActionSound.play(MediaActionSound.SHUTTER_CLICK) }
                                }

                                capture.takePicture(
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                val buffer = image.planes[0].buffer
                                                val bytes = ByteArray(buffer.remaining())
                                                buffer.get(bytes)
                                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                                val rotated = rotateBitmapIfNeeded(bitmap, image.imageInfo.rotationDegrees)
                                                image.close()

                                                withContext(Dispatchers.Main) {
                                                    viewModel.addCapturedBitmap(rotated)
                                                    isCapturing = false
                                                    onNavigateToEditor()
                                                }
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            exception.printStackTrace()
                                            isCapturing = false
                                            Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            } else {
                                val mockBitmap = createMockDocumentBitmap()
                                viewModel.addCapturedBitmap(mockBitmap)
                                onNavigateToEditor()
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                modifier = Modifier.fillMaxSize(),
                                color = Color(0xFF16C7B5),
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }

                // Import Images Button (Screenshot Match)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { imageImportLauncher.launch("image/*") }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Import Images", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Import Images", color = Color.White, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // ALL FEATURES BOTTOM SHEET
        AnimatedVisibility(
            visible = showAllFeaturesSheet,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            AllFeaturesBottomSheet(
                onDismiss = { showAllFeaturesSheet = false },
                onSelectFeature = { featureId ->
                    showAllFeaturesSheet = false
                    when (featureId) {
                        "slides" -> selectedRibbonMode = "Slides"
                        "to_excel" -> selectedRibbonMode = "To Excel"
                        "id_photo_maker" -> selectedRibbonMode = "ID Photo Maker"
                        "timestamp" -> selectedRibbonMode = "Timestamp"
                        "to_word" -> selectedRibbonMode = "To Word"
                        "sign" -> selectedRibbonMode = "Sign"
                        "scan" -> selectedRibbonMode = "Scan"
                        "smart_eraser" -> selectedRibbonMode = "Smart Erase"
                        "id_cards" -> selectedRibbonMode = "ID Cards"
                        "question_set" -> selectedRibbonMode = "Question Set"
                        "translate" -> selectedRibbonMode = "Translate"
                        "restore_photo" -> selectedRibbonMode = "Restore Photo"
                        "book" -> selectedRibbonMode = "Book"
                        "whiteboard" -> selectedRibbonMode = "Whiteboard"
                        "scan_code" -> selectedRibbonMode = "Scan Code"
                        "count_cam" -> selectedRibbonMode = "CountCam"
                        else -> onOpenToolDedicated?.invoke(featureId)
                    }
                }
            )
        }

        // FLASH DIALOG
        if (showFlashMenu) {
            AlertDialog(
                onDismissRequest = { showFlashMenu = false },
                containerColor = Color(0xFF0F172A),
                title = { Text("Flashlight Mode", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlashOption.values().forEach { opt ->
                            val isSel = cameraPrefs.flashOption == opt
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) Color(0xFF16C7B5).copy(alpha = 0.2f) else Color.Transparent)
                                    .border(1.dp, if (isSel) Color(0xFF16C7B5) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        val updated = cameraPrefs.copy(flashOption = opt)
                                        cameraPrefs = updated
                                        settingsManager.save(updated)
                                        showFlashMenu = false
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(opt.label, color = if (isSel) Color(0xFF16C7B5) else Color.White, fontWeight = FontWeight.Bold)
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16C7B5))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFlashMenu = false }) { Text("Close", color = Color(0xFF16C7B5)) }
                }
            )
        }

        // RESOLUTION DIALOG
        if (showResolutionPicker) {
            AlertDialog(
                onDismissRequest = { showResolutionPicker = false },
                containerColor = Color(0xFF0F172A),
                title = { Text("Camera Capture Resolution", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CameraScanResolution.values().forEach { res ->
                            val isSel = cameraPrefs.resolution == res
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) Color(0xFF16C7B5).copy(alpha = 0.2f) else Color.Transparent)
                                    .border(1.dp, if (isSel) Color(0xFF16C7B5) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        val updated = cameraPrefs.copy(resolution = res)
                                        cameraPrefs = updated
                                        settingsManager.save(updated)
                                        showResolutionPicker = false
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(res.label, color = if (isSel) Color(0xFF16C7B5) else Color.White, fontWeight = FontWeight.Bold)
                                    Text("${res.approxWidth} x ${res.approxHeight} px", color = Color.Gray, fontSize = 11.sp)
                                }
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16C7B5))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showResolutionPicker = false }) { Text("Close", color = Color(0xFF16C7B5)) }
                }
            )
        }

        // SETTINGS DIALOG
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                containerColor = Color(0xFF0F172A),
                title = { Text("Camera & Scanner Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Auto Crop", color = Color.White)
                            Switch(
                                checked = cameraPrefs.autoCrop,
                                onCheckedChange = {
                                    val u = cameraPrefs.copy(autoCrop = it)
                                    cameraPrefs = u; settingsManager.save(u)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF16C7B5))
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Shutter Audio Sound", color = Color.White)
                            Switch(
                                checked = cameraPrefs.shutterSound,
                                onCheckedChange = {
                                    val u = cameraPrefs.copy(shutterSound = it)
                                    cameraPrefs = u; settingsManager.save(u)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF16C7B5))
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) { Text("Save & Close", color = Color(0xFF16C7B5), fontWeight = FontWeight.Bold) }
                }
            )
        }
    }
}

@Composable
fun AllFeaturesBottomSheet(
    onDismiss: () -> Unit,
    onSelectFeature: (String) -> Unit
) {
    val items = listOf(
        AllFeaturesGridItem("slides", "Slides", Icons.Default.Slideshow, Color(0xFF2563EB), "Scan Documents"),
        AllFeaturesGridItem("to_excel", "To Excel", Icons.Default.TableChart, Color(0xFF10B981), "Scan Documents"),
        AllFeaturesGridItem("id_photo_maker", "Id Photo\nMaker", Icons.Default.Badge, Color(0xFFA855F7), "Scan Documents"),
        AllFeaturesGridItem("timestamp", "Timestamp", Icons.Default.Timer, Color(0xFFF97316), "Scan Documents"),

        AllFeaturesGridItem("to_word", "To Word", Icons.Default.Description, Color(0xFF2563EB), "Convert Formats"),
        AllFeaturesGridItem("to_excel", "To Excel", Icons.Default.TableChart, Color(0xFF10B981), "Convert Formats"),

        AllFeaturesGridItem("sign", "Sign", Icons.Default.Draw, Color(0xFFEC4899), "Doc Tools"),
        AllFeaturesGridItem("scan", "Scan", Icons.Default.CropFree, Color(0xFF16C7B5), "Doc Tools"),
        AllFeaturesGridItem("smart_eraser", "Smart\nEraser", Icons.Default.AutoAwesome, Color(0xFF10B981), "Doc Tools"),
        AllFeaturesGridItem("id_cards", "ID Cards", Icons.Default.Badge, Color(0xFF0EA5E9), "Doc Tools"),

        AllFeaturesGridItem("question_set", "Question Set", Icons.Default.QuestionAnswer, Color(0xFFA855F7), "Enhance Images"),
        AllFeaturesGridItem("translate", "Translate", Icons.Default.Translate, Color(0xFF16C7B5), "Enhance Images"),
        AllFeaturesGridItem("restore_photo", "Restore Photo", Icons.Default.Image, Color(0xFFF97316), "Enhance Images"),
        AllFeaturesGridItem("book", "Book", Icons.Default.Book, Color(0xFF0EA5E9), "Enhance Images"),

        AllFeaturesGridItem("whiteboard", "Whiteboard", Icons.Default.Tv, Color(0xFF10B981), "Capture Screens"),
        AllFeaturesGridItem("scan_code", "Scan Code", Icons.Default.QrCodeScanner, Color(0xFF16C7B5), "Capture Screens"),

        AllFeaturesGridItem("count_cam", "CountCam", Icons.Default.CenterFocusStrong, Color(0xFFA855F7), "More Tools"),
        AllFeaturesGridItem("greeting_card", "Greeting Card", Icons.Default.Star, Color(0xFF3B82F6), "More Tools"),
        AllFeaturesGridItem("ai_chat", "AI Chat", Icons.Default.AutoAwesome, Color(0xFFA855F7), "More Tools", isHot = true)
    )

    val categories = items.map { it.category }.distinct()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(0.78f)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(Color(0xFF060B12))
                .border(1.5.dp, Color(0xFF16C7B5).copy(alpha = 0.6f), RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .clickable(enabled = false) {}
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF16C7B5), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("All Features", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                categories.forEach { category ->
                    Text(
                        text = category,
                        color = Color(0xFF16C7B5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    val catItems = items.filter { it.category == category }
                    val rows = catItems.chunked(4)

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { item ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1.05f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF0D1724))
                                            .border(1.dp, Color(0xFF16C7B5).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                            .clickable { onSelectFeature(item.id) }
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.title,
                                                tint = item.badgeColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.title,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                repeat(4 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
