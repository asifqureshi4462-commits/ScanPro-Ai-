package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PointF
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ManageHistory
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScannedDocument
import com.example.ui.ScanProViewModel
import com.example.util.DocumentFilterMode
import com.example.util.DocumentQuad
import com.example.util.OpenCvDocumentDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class FilterOption(
    val id: String,
    val name: String,
    val mode: DocumentFilterMode
)

data class EditablePageItem(
    val originalBitmap: Bitmap,
    val displayBitmap: Bitmap, // Scaled down for 60fps instant UI response
    val thumbnailBitmap: Bitmap, // Tiny bitmap for bottom strip
    var filterMode: DocumentFilterMode = DocumentFilterMode.ORIGINAL,
    var brightness: Float = 0f,
    var contrast: Float = 1f,
    var saturation: Float = 1f,
    var quad: DocumentQuad = OpenCvDocumentDetector.defaultQuad()
)

private fun createOptimizedBitmaps(original: Bitmap): Pair<Bitmap, Bitmap> {
    val origW = original.width
    val origH = original.height

    // 1. Display bitmap (max 1080px for smooth rendering)
    val maxDisplay = 1080
    val displayScale = if (origW > maxDisplay || origH > maxDisplay) {
        maxDisplay.toFloat() / max(origW, origH)
    } else {
        1.0f
    }
    val dispW = max(30, (origW * displayScale).toInt())
    val dispH = max(30, (origH * displayScale).toInt())
    val displayBmp = if (displayScale < 1.0f) {
        Bitmap.createScaledBitmap(original, dispW, dispH, true)
    } else {
        original
    }

    // 2. Thumbnail bitmap (120px for instant filter strip scrolling)
    val thumbScale = 120f / max(origW, origH)
    val thumbW = max(20, (origW * thumbScale).toInt())
    val thumbH = max(20, (origH * thumbScale).toInt())
    val thumbBmp = Bitmap.createScaledBitmap(original, thumbW, thumbH, true)

    return Pair(displayBmp, thumbBmp)
}

// Ultra-fast Hardware GPU Shader ColorMatrix Builder
fun getGpuColorMatrix(
    filterMode: DocumentFilterMode,
    brightness: Float,
    contrast: Float,
    saturation: Float
): ColorMatrix {
    val cm: ColorMatrix = when (filterMode) {
        DocumentFilterMode.ORIGINAL -> ColorMatrix()
        DocumentFilterMode.GRAYSCALE -> ColorMatrix().apply { setToSaturation(0f) }
        DocumentFilterMode.BLACK_AND_WHITE -> {
            ColorMatrix(
                floatArrayOf(
                    3.0f, 3.0f, 3.0f, 0f, -380f,
                    3.0f, 3.0f, 3.0f, 0f, -380f,
                    3.0f, 3.0f, 3.0f, 0f, -380f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        DocumentFilterMode.MAGIC_COLOR -> {
            ColorMatrix(
                floatArrayOf(
                    1.4f, 0f, 0f, 0f, 18f,
                    0f, 1.4f, 0f, 0f, 18f,
                    0f, 0f, 1.4f, 0f, 18f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        DocumentFilterMode.LIGHTEN -> {
            ColorMatrix(
                floatArrayOf(
                    1.25f, 0f, 0f, 0f, 35f,
                    0f, 1.25f, 0f, 0f, 35f,
                    0f, 0f, 1.25f, 0f, 35f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        DocumentFilterMode.ENHANCE -> {
            ColorMatrix(
                floatArrayOf(
                    1.5f, 0f, 0f, 0f, -25f,
                    0f, 1.5f, 0f, 0f, -25f,
                    0f, 0f, 1.5f, 0f, -25f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        DocumentFilterMode.NO_SHADOW -> {
            ColorMatrix(
                floatArrayOf(
                    1.3f, 0f, 0f, 0f, 25f,
                    0f, 1.3f, 0f, 0f, 25f,
                    0f, 0f, 1.3f, 0f, 25f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        DocumentFilterMode.NO_WATERMARK -> {
            ColorMatrix(
                floatArrayOf(
                    1.6f, 0f, 0f, 0f, -50f,
                    0f, 1.6f, 0f, 0f, -50f,
                    0f, 0f, 1.6f, 0f, -50f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        DocumentFilterMode.NO_HANDWRITING -> {
            ColorMatrix(
                floatArrayOf(
                    2.2f, 1.6f, -0.6f, 0f, -240f,
                    2.2f, 1.6f, -0.6f, 0f, -240f,
                    2.2f, 1.6f, -0.6f, 0f, -240f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        DocumentFilterMode.ECO -> {
            val ecoBase = ColorMatrix().apply { setToSaturation(0f) }
            val ecoMatrix = ColorMatrix(
                floatArrayOf(
                    0.85f, 0f, 0f, 0f, 40f,
                    0f, 0.85f, 0f, 0f, 40f,
                    0f, 0f, 0.85f, 0f, 40f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ecoBase.timesAssign(ecoMatrix)
            ecoBase
        }
        DocumentFilterMode.INVERT -> {
            ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
    }

    // Apply Live Fine-Tune Adjustments
    if (brightness != 0f || contrast != 1f || saturation != 1f) {
        val t = (1.0f - contrast) * 128f + brightness
        val adjMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, t,
                0f, contrast, 0f, 0f, t,
                0f, 0f, contrast, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )

        if (saturation != 1f) {
            val satMatrix = ColorMatrix().apply { setToSaturation(saturation) }
            adjMatrix.timesAssign(satMatrix)
        }

        cm.timesAssign(adjMatrix)
    }

    return cm
}

@Composable
fun DocumentEditorScreen(
    viewModel: ScanProViewModel,
    initialDocTitle: String? = null,
    onBackToCamera: () -> Unit,
    onAddMorePages: () -> Unit,
    onProceedToExport: (List<Bitmap>, String) -> Unit,
    onExtractTextClicked: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val capturedBitmaps by viewModel.capturedBitmaps.collectAsState()
    val pageItems = remember { mutableStateListOf<EditablePageItem>() }

    // Initialize optimized non-destructive page models
    LaunchedEffect(capturedBitmaps.size) {
        if (capturedBitmaps.isNotEmpty()) {
            if (pageItems.isEmpty()) {
                capturedBitmaps.forEach { bmp ->
                    val (dispBmp, thumbBmp) = createOptimizedBitmaps(bmp)
                    val detected = OpenCvDocumentDetector.detectDocumentEdges(bmp)
                    pageItems.add(
                        EditablePageItem(
                            originalBitmap = bmp,
                            displayBitmap = dispBmp,
                            thumbnailBitmap = thumbBmp,
                            filterMode = DocumentFilterMode.ORIGINAL,
                            quad = detected
                        )
                    )
                }
            } else if (capturedBitmaps.size > pageItems.size) {
                for (i in pageItems.size until capturedBitmaps.size) {
                    val bmp = capturedBitmaps[i]
                    val (dispBmp, thumbBmp) = createOptimizedBitmaps(bmp)
                    val detected = OpenCvDocumentDetector.detectDocumentEdges(bmp)
                    pageItems.add(
                        EditablePageItem(
                            originalBitmap = bmp,
                            displayBitmap = dispBmp,
                            thumbnailBitmap = thumbBmp,
                            filterMode = DocumentFilterMode.ORIGINAL,
                            quad = detected
                        )
                    )
                }
            }
        }
    }

    if (pageItems.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0E17)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFF16C7B5))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Loading captured document...", color = Color.White, fontSize = 13.sp)
            }
        }
        return
    }

    var currentPageIndex by remember { mutableIntStateOf(0) }
    val currentSafeIndex = currentPageIndex.coerceIn(0, (pageItems.size - 1).coerceAtLeast(0))
    val currentPage = pageItems[currentSafeIndex]

    var isDedicatedCropScreenOpen by remember { mutableStateOf(false) }

    val defaultName = remember {
        val dateStr = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())
        initialDocTitle ?: "ScanPro Ai $dateStr"
    }
    var documentTitle by remember { mutableStateOf(defaultName) }
    var isRenamingTitle by remember { mutableStateOf(false) }
    var tempRenameText by remember { mutableStateOf(documentTitle) }

    val filterOptions = remember {
        listOf(
            FilterOption("orig", "Original", DocumentFilterMode.ORIGINAL),
            FilterOption("light", "Lighten", DocumentFilterMode.LIGHTEN),
            FilterOption("enhance", "Enhance", DocumentFilterMode.ENHANCE),
            FilterOption("magic", "✦ Magic Pro", DocumentFilterMode.MAGIC_COLOR),
            FilterOption("no_wm", "No Watermark", DocumentFilterMode.NO_WATERMARK),
            FilterOption("no_shadow", "No Shadow", DocumentFilterMode.NO_SHADOW),
            FilterOption("no_hw", "No Handwriting", DocumentFilterMode.NO_HANDWRITING),
            FilterOption("bw", "B&W", DocumentFilterMode.BLACK_AND_WHITE),
            FilterOption("eco", "Eco", DocumentFilterMode.ECO),
            FilterOption("gray", "Grayscale", DocumentFilterMode.GRAYSCALE),
            FilterOption("invert", "Invert", DocumentFilterMode.INVERT)
        )
    }

    var isCompareModeActive by remember { mutableStateOf(false) }
    var showAdjustSliders by remember { mutableStateOf(false) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPageManagerDialog by remember { mutableStateOf(false) }
    var showAddPageSourceDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                uris.forEach { uri ->
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val importedBmp = BitmapFactory.decodeStream(stream)
                        if (importedBmp != null) {
                            val (dispBmp, thumbBmp) = createOptimizedBitmaps(importedBmp)
                            val detected = OpenCvDocumentDetector.detectDocumentEdges(importedBmp)
                            withContext(Dispatchers.Main) {
                                pageItems.add(
                                    EditablePageItem(
                                        originalBitmap = importedBmp,
                                        displayBitmap = dispBmp,
                                        thumbnailBitmap = thumbBmp,
                                        filterMode = DocumentFilterMode.ORIGINAL,
                                        quad = detected
                                    )
                                )
                                viewModel.addCapturedBitmap(importedBmp)
                            }
                        }
                    }
                }
            }
        }
    }

    // INSTANT 60FPS HARDWARE GPU FILTER
    val liveColorFilter = remember(
        currentPage.filterMode,
        currentPage.brightness,
        currentPage.contrast,
        currentPage.saturation,
        isCompareModeActive
    ) {
        if (isCompareModeActive) {
            null
        } else {
            val cm = getGpuColorMatrix(
                filterMode = currentPage.filterMode,
                brightness = currentPage.brightness,
                contrast = currentPage.contrast,
                saturation = currentPage.saturation
            )
            ColorFilter.colorMatrix(cm)
        }
    }

    BackHandler {
        if (isDedicatedCropScreenOpen) {
            isDedicatedCropScreenOpen = false
        } else {
            onBackToCamera()
        }
    }

    // DEDICATED SCREENSHOT 2 CROP SCREEN
    if (isDedicatedCropScreenOpen) {
        DedicatedCropViewScreen(
            sourceBitmap = currentPage.originalBitmap,
            initialQuad = currentPage.quad,
            onClose = { isDedicatedCropScreenOpen = false },
            onApplyCrop = { croppedBitmap, newQuad ->
                val (newDisp, newThumb) = createOptimizedBitmaps(croppedBitmap)
                pageItems[currentSafeIndex] = currentPage.copy(
                    originalBitmap = croppedBitmap,
                    displayBitmap = newDisp,
                    thumbnailBitmap = newThumb,
                    quad = newQuad
                )
                viewModel.updateCapturedBitmap(currentSafeIndex, croppedBitmap)
                isDedicatedCropScreenOpen = false
                Toast.makeText(context, "Document cropped accurately!", Toast.LENGTH_SHORT).show()
            }
        )
        return
    }

    // Rename Dialog
    if (isRenamingTitle) {
        AlertDialog(
            onDismissRequest = { isRenamingTitle = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Rename Document", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempRenameText,
                    onValueChange = { tempRenameText = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempRenameText.isNotBlank()) {
                            documentTitle = tempRenameText.trim()
                        }
                        isRenamingTitle = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5))
                ) {
                    Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isRenamingTitle = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Delete Page Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Delete this page?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to remove page ${currentSafeIndex + 1}?",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pageItems.size > 1) {
                            pageItems.removeAt(currentSafeIndex)
                            viewModel.removeCapturedBitmap(currentSafeIndex)
                            currentPageIndex = (currentSafeIndex - 1).coerceAtLeast(0)
                        } else {
                            pageItems.clear()
                            viewModel.clearCapturedBitmaps()
                            onBackToCamera()
                        }
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Add Page Dialog
    if (showAddPageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showAddPageSourceDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Add More Pages", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            showAddPageSourceDialog = false
                            onAddMorePages()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5), contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Capture with Camera", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            showAddPageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1E2A), contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF16C7B5), RoundedCornerShape(20.dp))
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color(0xFF16C7B5))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import from Gallery")
                    }
                }
            },
            confirmButton = {}
        )
    }

    // MANAGE PAGES DIALOG
    if (showPageManagerDialog) {
        AlertDialog(
            onDismissRequest = { showPageManagerDialog = false },
            containerColor = Color(0xFF0F172A),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Manage Pages (${pageItems.size})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    IconButton(onClick = { showPageManagerDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(pageItems) { idx, item ->
                            val isSelected = idx == currentSafeIndex
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF16C7B5).copy(alpha = 0.25f) else Color(0xFF1E293B)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF16C7B5) else Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        currentPageIndex = idx
                                        showPageManagerDialog = false
                                    }
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.78f)
                                            .clip(RoundedCornerShape(6.dp))
                                    ) {
                                        Image(
                                            bitmap = item.thumbnailBitmap.asImageBitmap(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        if (pageItems.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    pageItems.removeAt(idx)
                                                    viewModel.removeCapturedBitmap(idx)
                                                    currentPageIndex = (currentPageIndex - 1).coerceAtLeast(0)
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(26.dp)
                                                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Page ${idx + 1}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        if (isSelected) {
                                            Text("Active", color = Color(0xFF16C7B5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            showPageManagerDialog = false
                            showAddPageSourceDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5), contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add New Page", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {}
        )
    }

    // MAIN SCREEN CONTENT
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E17))
            .statusBarsPadding()
    ) {
        // 1. TOP TOOLBAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onBackToCamera) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            tempRenameText = documentTitle
                            isRenamingTitle = true
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = documentTitle,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            TextButton(
                onClick = { showAddPageSourceDialog = true }
            ) {
                Text(
                    text = "Add",
                    color = Color(0xFF16C7B5),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. CENTRAL DOCUMENT DISPLAY CANVAS (CLEAN PHOTO PREVIEW - NO CROP BORDER)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Instant 60fps Display
                Image(
                    bitmap = currentPage.displayBitmap.asImageBitmap(),
                    contentDescription = "Document Scan",
                    colorFilter = liveColorFilter,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Floating Controls: [Remove] | [ ◀ 1/3 ▶ ] | [Compare]
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable { showDeleteConfirmDialog = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remove", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Prev",
                            tint = if (currentSafeIndex > 0) Color.White else Color.Gray,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(enabled = currentSafeIndex > 0) {
                                    currentPageIndex--
                                }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${currentSafeIndex + 1}/${pageItems.size}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next",
                            tint = if (currentSafeIndex < pageItems.size - 1) Color.White else Color.Gray,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(enabled = currentSafeIndex < pageItems.size - 1) {
                                    currentPageIndex++
                                }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCompareModeActive) Color(0xFF16C7B5) else Color.Black.copy(alpha = 0.7f))
                        .clickable { isCompareModeActive = !isCompareModeActive }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = null,
                            tint = if (isCompareModeActive) Color.Black else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Compare",
                            color = if (isCompareModeActive) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Real-Time Adjust Sliders Panel
        AnimatedVisibility(
            visible = showAdjustSliders,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F1E2A))
                    .border(1.dp, Color(0xFF16C7B5).copy(alpha = 0.3f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fine-Tune Adjustments", color = Color(0xFF16C7B5), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                currentPage.brightness = 0f
                                currentPage.contrast = 1f
                                currentPage.saturation = 1f
                            }
                        ) {
                            Text("Reset", color = Color.Gray, fontSize = 11.sp)
                        }
                        IconButton(onClick = { showAdjustSliders = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // 1. Brightness Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Brightness", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(75.dp))
                    Slider(
                        value = currentPage.brightness,
                        onValueChange = { currentPage.brightness = it },
                        valueRange = -80f..80f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF16C7B5), activeTrackColor = Color(0xFF16C7B5)),
                        modifier = Modifier.weight(1f)
                    )
                    Text("${currentPage.brightness.toInt()}", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                }

                // 2. Contrast Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Contrast", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(75.dp))
                    Slider(
                        value = currentPage.contrast,
                        onValueChange = { currentPage.contrast = it },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF16C7B5), activeTrackColor = Color(0xFF16C7B5)),
                        modifier = Modifier.weight(1f)
                    )
                    Text(String.format(Locale.US, "%.1f", currentPage.contrast), color = Color.Gray, fontSize = 11.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                }

                // 3. Saturation Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Saturation", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(75.dp))
                    Slider(
                        value = currentPage.saturation,
                        onValueChange = { currentPage.saturation = it },
                        valueRange = 0f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF16C7B5), activeTrackColor = Color(0xFF16C7B5)),
                        modifier = Modifier.weight(1f)
                    )
                    Text(String.format(Locale.US, "%.1f", currentPage.saturation), color = Color.Gray, fontSize = 11.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                }
            }
        }

        // 3. ULTRA-FAST INSTANT FILTER STRIP (Uses tiny thumbnailBitmap for 0ms lag)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF070B12))
                .padding(vertical = 8.dp)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(filterOptions) { _, filter ->
                    val isSelected = currentPage.filterMode == filter.mode

                    val thumbFilter = remember(filter.mode) {
                        ColorFilter.colorMatrix(getGpuColorMatrix(filter.mode, 0f, 1f, 1f))
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                currentPage.filterMode = filter.mode
                            }
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 62.dp, height = 48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF16C7B5) else Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                        ) {
                            Image(
                                bitmap = currentPage.thumbnailBitmap.asImageBitmap(),
                                contentDescription = filter.name,
                                colorFilter = thumbFilter,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF16C7B5).copy(alpha = 0.25f))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = filter.name,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF16C7B5) else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // 4. BOTTOM EDITING TOOLS DOCK
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF04070D))
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditingDockItem(
                    icon = Icons.Default.PhotoCamera,
                    label = "Retake",
                    onClick = onBackToCamera
                )

                EditingDockItem(
                    icon = Icons.Default.RotateLeft,
                    label = "Left",
                    onClick = {
                        val matrix = Matrix().apply { postRotate(-90f) }
                        val rotatedOrig = Bitmap.createBitmap(
                            currentPage.originalBitmap, 0, 0,
                            currentPage.originalBitmap.width, currentPage.originalBitmap.height,
                            matrix, true
                        )
                        val (newDisp, newThumb) = createOptimizedBitmaps(rotatedOrig)
                        pageItems[currentSafeIndex] = currentPage.copy(
                            originalBitmap = rotatedOrig,
                            displayBitmap = newDisp,
                            thumbnailBitmap = newThumb
                        )
                    }
                )

                EditingDockItem(
                    icon = Icons.Default.Crop,
                    label = "Crop",
                    onClick = {
                        isDedicatedCropScreenOpen = true
                    }
                )

                EditingDockItem(
                    icon = Icons.Default.Tune,
                    label = "Adjust",
                    onClick = { showAdjustSliders = !showAdjustSliders }
                )

                EditingDockItem(
                    icon = Icons.Default.TextFields,
                    label = "Extract Text",
                    onClick = {
                        val tempDoc = ScannedDocument(
                            title = documentTitle,
                            filePath = "",
                            fileType = "PDF"
                        )
                        viewModel.runAiOcrOnDocument(tempDoc)
                        onExtractTextClicked(tempDoc)
                    }
                )

                EditingDockItem(
                    icon = Icons.Default.Draw,
                    label = "Sign",
                    onClick = {
                        Toast.makeText(context, "Sign tool: add e-signature stamp", Toast.LENGTH_SHORT).show()
                    }
                )

                EditingDockItem(
                    icon = Icons.Default.ManageHistory,
                    label = "Manage Pages",
                    onClick = { showPageManagerDialog = true }
                )

                Box(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(width = 54.dp, height = 38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF16C7B5))
                        .clickable {
                            val renderedPages = pageItems.map { item ->
                                val base: Bitmap = if (item.filterMode == DocumentFilterMode.ORIGINAL) {
                                    item.originalBitmap
                                } else {
                                    OpenCvDocumentDetector.applyFilter(item.originalBitmap, item.filterMode)
                                }
                                OpenCvDocumentDetector.applyAdjustments(
                                    bitmap = base,
                                    brightness = item.brightness,
                                    contrast = item.contrast,
                                    exposure = 0f
                                )
                            }
                            onProceedToExport(renderedPages, documentTitle)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirm / Next",
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// DEDICATED SCREENSHOT 2 CROP SCREEN (ACCURATE ASPECT RATIO CROP)
// ==========================================
@Composable
fun DedicatedCropViewScreen(
    sourceBitmap: Bitmap,
    initialQuad: DocumentQuad,
    onClose: () -> Unit,
    onApplyCrop: (Bitmap, DocumentQuad) -> Unit
) {
    var workingBitmap by remember { mutableStateOf(sourceBitmap) }
    var currentQuad by remember { mutableStateOf(initialQuad) }
    var activeHandle by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text("Crop", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val containerWidth = maxWidth.value
            val containerHeight = maxHeight.value

            val bmpW = workingBitmap.width.toFloat()
            val bmpH = workingBitmap.height.toFloat()

            val scale = min(containerWidth / bmpW, containerHeight / bmpH)
            val renderedW = bmpW * scale
            val renderedH = bmpH * scale

            Box(
                modifier = Modifier
                    .size(width = renderedW.dp, height = renderedH.dp)
                    .background(Color(0xFF0F172A))
            ) {
                Image(
                    bitmap = workingBitmap.asImageBitmap(),
                    contentDescription = "Crop Target",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(workingBitmap) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    val threshold = 75f

                                    val tl = Offset(currentQuad.topLeft.x * w, currentQuad.topLeft.y * h)
                                    val tr = Offset(currentQuad.topRight.x * w, currentQuad.topRight.y * h)
                                    val br = Offset(currentQuad.bottomRight.x * w, currentQuad.bottomRight.y * h)
                                    val bl = Offset(currentQuad.bottomLeft.x * w, currentQuad.bottomLeft.y * h)

                                    val topMid = Offset((tl.x + tr.x) / 2f, (tl.y + tr.y) / 2f)
                                    val rightMid = Offset((tr.x + br.x) / 2f, (tr.y + br.y) / 2f)
                                    val bottomMid = Offset((bl.x + br.x) / 2f, (bl.y + br.y) / 2f)
                                    val leftMid = Offset((tl.x + bl.x) / 2f, (tl.y + bl.y) / 2f)

                                    val distances = mapOf(
                                        "TL" to dist(offset.x, offset.y, tl.x, tl.y),
                                        "TR" to dist(offset.x, offset.y, tr.x, tr.y),
                                        "BR" to dist(offset.x, offset.y, br.x, br.y),
                                        "BL" to dist(offset.x, offset.y, bl.x, bl.y),
                                        "T_MID" to dist(offset.x, offset.y, topMid.x, topMid.y),
                                        "R_MID" to dist(offset.x, offset.y, rightMid.x, rightMid.y),
                                        "B_MID" to dist(offset.x, offset.y, bottomMid.x, bottomMid.y),
                                        "L_MID" to dist(offset.x, offset.y, leftMid.x, leftMid.y)
                                    )

                                    val minEntry = distances.minByOrNull { it.value }
                                    activeHandle = if (minEntry != null && minEntry.value <= threshold) {
                                        minEntry.key
                                    } else null
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    if (w <= 0f || h <= 0f) return@detectDragGestures

                                    val dxN = dragAmount.x / w
                                    val dyN = dragAmount.y / h

                                    when (activeHandle) {
                                        "TL" -> currentQuad = currentQuad.copy(
                                            topLeft = PointF((currentQuad.topLeft.x + dxN).coerceIn(0f, 0.45f), (currentQuad.topLeft.y + dyN).coerceIn(0f, 0.45f))
                                        )
                                        "TR" -> currentQuad = currentQuad.copy(
                                            topRight = PointF((currentQuad.topRight.x + dxN).coerceIn(0.55f, 1f), (currentQuad.topRight.y + dyN).coerceIn(0f, 0.45f))
                                        )
                                        "BR" -> currentQuad = currentQuad.copy(
                                            bottomRight = PointF((currentQuad.bottomRight.x + dxN).coerceIn(0.55f, 1f), (currentQuad.bottomRight.y + dyN).coerceIn(0.55f, 1f))
                                        )
                                        "BL" -> currentQuad = currentQuad.copy(
                                            bottomLeft = PointF((currentQuad.bottomLeft.x + dxN).coerceIn(0f, 0.45f), (currentQuad.bottomLeft.y + dyN).coerceIn(0.55f, 1f))
                                        )
                                        "T_MID" -> currentQuad = currentQuad.copy(
                                            topLeft = PointF(currentQuad.topLeft.x, (currentQuad.topLeft.y + dyN).coerceIn(0f, 0.45f)),
                                            topRight = PointF(currentQuad.topRight.x, (currentQuad.topLeft.y + dyN).coerceIn(0f, 0.45f))
                                        )
                                        "B_MID" -> currentQuad = currentQuad.copy(
                                            bottomLeft = PointF(currentQuad.bottomLeft.x, (currentQuad.bottomLeft.y + dyN).coerceIn(0.55f, 1f)),
                                            bottomRight = PointF(currentQuad.bottomRight.x, (currentQuad.bottomLeft.y + dyN).coerceIn(0.55f, 1f))
                                        )
                                        "L_MID" -> currentQuad = currentQuad.copy(
                                            topLeft = PointF((currentQuad.topLeft.x + dxN).coerceIn(0f, 0.45f), currentQuad.topLeft.y),
                                            bottomLeft = PointF((currentQuad.bottomLeft.x + dxN).coerceIn(0f, 0.45f), currentQuad.bottomLeft.y)
                                        )
                                        "R_MID" -> currentQuad = currentQuad.copy(
                                            topRight = PointF((currentQuad.topRight.x + dxN).coerceIn(0.55f, 1f), currentQuad.topRight.y),
                                            bottomRight = PointF((currentQuad.bottomRight.x + dxN).coerceIn(0.55f, 1f), currentQuad.bottomRight.y)
                                        )
                                    }
                                },
                                onDragEnd = { activeHandle = null },
                                onDragCancel = { activeHandle = null }
                            )
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    val tl = Offset(currentQuad.topLeft.x * w, currentQuad.topLeft.y * h)
                    val tr = Offset(currentQuad.topRight.x * w, currentQuad.topRight.y * h)
                    val br = Offset(currentQuad.bottomRight.x * w, currentQuad.bottomRight.y * h)
                    val bl = Offset(currentQuad.bottomLeft.x * w, currentQuad.bottomLeft.y * h)

                    val topMid = Offset((tl.x + tr.x) / 2f, (tl.y + tr.y) / 2f)
                    val rightMid = Offset((tr.x + br.x) / 2f, (tr.y + br.y) / 2f)
                    val bottomMid = Offset((bl.x + br.x) / 2f, (bl.y + br.y) / 2f)
                    val leftMid = Offset((tl.x + bl.x) / 2f, (tl.y + bl.y) / 2f)

                    val path = Path().apply {
                        moveTo(tl.x, tl.y)
                        lineTo(tr.x, tr.y)
                        lineTo(br.x, br.y)
                        lineTo(bl.x, bl.y)
                        close()
                    }
                    drawPath(path, color = Color(0xFF16C7B5), style = Stroke(width = 3.5f))

                    listOf(tl, tr, br, bl).forEach { cornerPt ->
                        drawCircle(color = Color(0xFF16C7B5), radius = 14f, center = cornerPt)
                        drawCircle(color = Color.White, radius = 5f, center = cornerPt)
                    }

                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(topMid.x - 22f, topMid.y - 7f),
                        size = androidx.compose.ui.geometry.Size(44f, 14f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f, 7f)
                    )
                    drawRoundRect(
                        color = Color(0xFF16C7B5),
                        topLeft = Offset(topMid.x - 22f, topMid.y - 7f),
                        size = androidx.compose.ui.geometry.Size(44f, 14f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f, 7f),
                        style = Stroke(width = 2f)
                    )

                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(bottomMid.x - 22f, bottomMid.y - 7f),
                        size = androidx.compose.ui.geometry.Size(44f, 14f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f, 7f)
                    )
                    drawRoundRect(
                        color = Color(0xFF16C7B5),
                        topLeft = Offset(bottomMid.x - 22f, bottomMid.y - 7f),
                        size = androidx.compose.ui.geometry.Size(44f, 14f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f, 7f),
                        style = Stroke(width = 2f)
                    )

                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(leftMid.x - 7f, leftMid.y - 22f),
                        size = androidx.compose.ui.geometry.Size(14f, 44f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f, 7f)
                    )
                    drawRoundRect(
                        color = Color(0xFF16C7B5),
                        topLeft = Offset(leftMid.x - 7f, leftMid.y - 22f),
                        size = androidx.compose.ui.geometry.Size(14f, 44f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f, 7f),
                        style = Stroke(width = 2f)
                    )

                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(rightMid.x - 7f, rightMid.y - 22f),
                        size = androidx.compose.ui.geometry.Size(14f, 44f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f, 7f)
                    )
                    drawRoundRect(
                        color = Color(0xFF16C7B5),
                        topLeft = Offset(rightMid.x - 7f, rightMid.y - 22f),
                        size = androidx.compose.ui.geometry.Size(14f, 44f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f, 7f),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }

        Surface(
            color = Color(0xFF04070D),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        val m = Matrix().apply { postRotate(-90f) }
                        workingBitmap = Bitmap.createBitmap(workingBitmap, 0, 0, workingBitmap.width, workingBitmap.height, m, true)
                        currentQuad = OpenCvDocumentDetector.detectDocumentEdges(workingBitmap)
                    }
                ) {
                    Icon(Icons.Default.RotateLeft, contentDescription = "Left", tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Left", color = Color.White, fontSize = 11.sp)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        val m = Matrix().apply { postRotate(90f) }
                        workingBitmap = Bitmap.createBitmap(workingBitmap, 0, 0, workingBitmap.width, workingBitmap.height, m, true)
                        currentQuad = OpenCvDocumentDetector.detectDocumentEdges(workingBitmap)
                    }
                ) {
                    Icon(Icons.Default.RotateRight, contentDescription = "Right", tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Right", color = Color.White, fontSize = 11.sp)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        currentQuad = OpenCvDocumentDetector.detectDocumentEdges(workingBitmap)
                    }
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Auto Crop", tint = Color(0xFF16C7B5), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Auto Crop", color = Color(0xFF16C7B5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        currentQuad = OpenCvDocumentDetector.defaultQuad()
                    }
                ) {
                    Icon(Icons.Default.CropFree, contentDescription = "All", tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("All", color = Color.White, fontSize = 11.sp)
                }

                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF16C7B5))
                        .clickable {
                            val warped = OpenCvDocumentDetector.cropAndWarpPerspective(workingBitmap, currentQuad)
                            onApplyCrop(warped, OpenCvDocumentDetector.defaultQuad())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Confirm", tint = Color.Black, modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}

@Composable
fun EditingDockItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    return sqrt(dx * dx + dy * dy)
}
