package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import com.example.data.local.ScannedDocument
import com.example.ui.ScanProViewModel
import com.example.util.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class ExportFormatType {
    PDF,
    JPG,
    PNG,
    ALL_IMAGES
}

enum class PdfPageSizeOption {
    A4,
    LETTER,
    ORIGINAL
}

enum class PageOrientationOption {
    PORTRAIT,
    LANDSCAPE
}

@Composable
fun DocumentExportScreen(
    viewModel: ScanProViewModel,
    pages: List<Bitmap>,
    initialTitle: String,
    onBackToEditor: () -> Unit,
    onDoneOpenDocument: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var docTitle by remember { mutableStateOf(initialTitle) }
    var isRenaming by remember { mutableStateOf(false) }
    var tempRenameText by remember { mutableStateOf(docTitle) }

    var selectedFormat by remember { mutableStateOf(ExportFormatType.PDF) }
    var selectedPageSize by remember { mutableStateOf(PdfPageSizeOption.A4) }
    var selectedOrientation by remember { mutableStateOf(PageOrientationOption.PORTRAIT) }
    var imageQualityPercent by remember { mutableFloatStateOf(85f) }
    var isCompressPdf by remember { mutableStateOf(false) }

    var isExporting by remember { mutableStateOf(false) }
    var exportStatusNote by remember { mutableStateOf<String?>(null) }
    var savedResultDocument by remember { mutableStateOf<ScannedDocument?>(null) }

    val totalPages = pages.size
    val firstPreviewBitmap = pages.firstOrNull()

    val estimatedKb = remember(pages, selectedFormat, imageQualityPercent) {
        val baseKbPerPage = when (selectedFormat) {
            ExportFormatType.PDF -> 260
            ExportFormatType.PNG -> 580
            ExportFormatType.JPG -> (220 * (imageQualityPercent / 100f)).toInt()
            ExportFormatType.ALL_IMAGES -> (220 * (imageQualityPercent / 100f)).toInt() * totalPages
        }
        if (selectedFormat == ExportFormatType.ALL_IMAGES) baseKbPerPage else baseKbPerPage * totalPages
    }

    val safFileSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            if (selectedFormat == ExportFormatType.PDF) "application/pdf"
            else if (selectedFormat == ExportFormatType.PNG) "image/png"
            else "image/jpeg"
        )
    ) { uri: Uri? ->
        if (uri != null && savedResultDocument != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val srcFile = File(savedResultDocument!!.filePath)
                    context.contentResolver.openOutputStream(uri)?.use { outStream ->
                        srcFile.inputStream().use { inStream -> inStream.copyTo(outStream) }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Saved file to device storage successfully!", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Storage save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    BackHandler {
        onBackToEditor()
    }

    if (isRenaming) {
        AlertDialog(
            onDismissRequest = { isRenaming = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Rename Document", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempRenameText,
                    onValueChange = { tempRenameText = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempRenameText.isNotBlank()) {
                            docTitle = tempRenameText.trim()
                        }
                        isRenaming = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5))
                ) {
                    Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isRenaming = false }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E17))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToEditor,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Editor",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Export Document", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("PDF, Image & Print Settings", color = Color(0xFF16C7B5), fontSize = 11.sp)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E2A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF16C7B5).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (firstPreviewBitmap != null) {
                        Image(
                            bitmap = firstPreviewBitmap.asImageBitmap(),
                            contentDescription = "Preview",
                            modifier = Modifier
                                .size(width = 64.dp, height = 80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = docTitle,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename",
                                tint = Color(0xFF16C7B5),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        tempRenameText = docTitle
                                        isRenaming = true
                                    }
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Total Pages: $totalPages", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text("Estimated Size: ~$estimatedKb KB", color = Color(0xFF16C7B5), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1724)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("1. Choose Export Format", color = Color(0xFF16C7B5), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val formats = listOf(
                            ExportFormatType.PDF to "PDF Document",
                            ExportFormatType.JPG to "JPG Image",
                            ExportFormatType.PNG to "PNG Image",
                            ExportFormatType.ALL_IMAGES to "All Pages (JPGs)"
                        )

                        formats.forEach { (type, label) ->
                            val isSel = selectedFormat == type
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedFormat = type },
                                label = { Text(label, fontSize = 11.sp) },
                                leadingIcon = if (isSel) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF16C7B5),
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1724)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (selectedFormat == ExportFormatType.PDF) {
                        Text("2. PDF Page Dimensions & Orientation", color = Color(0xFF16C7B5), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                PdfPageSizeOption.A4 to "A4 Fit",
                                PdfPageSizeOption.LETTER to "Letter",
                                PdfPageSizeOption.ORIGINAL to "Original"
                            ).forEach { (size, label) ->
                                val isSel = selectedPageSize == size
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedPageSize = size },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF16C7B5),
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color(0xFF1E293B),
                                        labelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                PageOrientationOption.PORTRAIT to "Portrait",
                                PageOrientationOption.LANDSCAPE to "Landscape"
                            ).forEach { (orient, label) ->
                                val isSel = selectedOrientation == orient
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedOrientation = orient },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF16C7B5),
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color(0xFF1E293B),
                                        labelColor = Color.White
                                    )
                                )
                            }
                        }
                    } else {
                        Text("2. Image Quality (${imageQualityPercent.toInt()}%)", color = Color(0xFF16C7B5), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Slider(
                            value = imageQualityPercent,
                            onValueChange = { imageQualityPercent = it },
                            valueRange = 30f..100f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF16C7B5), activeTrackColor = Color(0xFF16C7B5))
                        )
                    }
                }
            }

            if (isExporting) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E2A)),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF16C7B5), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = Color(0xFF16C7B5), modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = exportStatusNote ?: "Compiling document pages...",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (savedResultDocument != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1724)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color(0xFF16C7B5), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ready in Local Vault!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Text(
                            text = "Saved: ${savedResultDocument!!.title} (${savedResultDocument!!.fileSizeBytes / 1024} KB)",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val ext = if (selectedFormat == ExportFormatType.PDF) "pdf"
                                    else if (selectedFormat == ExportFormatType.PNG) "png"
                                    else "jpg"
                                    safFileSaverLauncher.launch("${docTitle.replace(" ", "_")}.$ext")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save to Storage", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val file = File(savedResultDocument!!.filePath)
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    val mime = if (selectedFormat == ExportFormatType.PDF) "application/pdf" else "image/*"
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = mime
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Document"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val printHelper = PrintHelper(context).apply {
                                        scaleMode = PrintHelper.SCALE_MODE_FIT
                                    }
                                    val bmp = pages.firstOrNull()
                                    if (bmp != null) {
                                        printHelper.printBitmap(docTitle, bmp)
                                    } else {
                                        Toast.makeText(context, "No printable bitmap available", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Print", fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = { onDoneOpenDocument(savedResultDocument!!) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5), contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open in Vault", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    isExporting = true
                    exportStatusNote = "Rendering ${pages.size} pages & saving..."
                    scope.launch(Dispatchers.IO) {
                        try {
                            val cleanName = docTitle.replace(" ", "_")
                            val docsDir = File(context.filesDir, "scans").apply { if (!exists()) mkdirs() }

                            val outputFile = if (selectedFormat == ExportFormatType.PDF) {
                                PdfEngine.createPdfFromBitmaps(
                                    context = context,
                                    bitmaps = pages,
                                    outputFileName = "${cleanName}.pdf"
                                )
                            } else {
                                val isPng = selectedFormat == ExportFormatType.PNG
                                val ext = if (isPng) "png" else "jpg"
                                val format = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                                val imgFile = File(docsDir, "${cleanName}_Page1.$ext")
                                FileOutputStream(imgFile).use { out ->
                                    pages.first().compress(format, imageQualityPercent.toInt(), out)
                                }
                                imgFile
                            }

                            val doc = ScannedDocument(
                                title = outputFile.name,
                                filePath = outputFile.absolutePath,
                                fileType = if (selectedFormat == ExportFormatType.PDF) "PDF" else "IMAGE",
                                fileSizeBytes = outputFile.length(),
                                pageCount = pages.size,
                                category = "Documents",
                                createdAt = System.currentTimeMillis()
                            )

                            viewModel.addNewDocument(doc)
                            viewModel.clearCapturedBitmaps()

                            withContext(Dispatchers.Main) {
                                isExporting = false
                                savedResultDocument = doc
                                Toast.makeText(context, "Exported successfully to ScanPro AI Vault!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isExporting = false
                                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = !isExporting && pages.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5), contentColor = Color.Black),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp))
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedFormat == ExportFormatType.PDF) "Save as PDF" else "Save Image File",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
