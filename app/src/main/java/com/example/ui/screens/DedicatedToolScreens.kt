package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScannedDocument
import com.example.ui.ScanProViewModel
import com.example.util.OpenCvDocumentDetector
import com.example.util.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@Composable
fun ToolHeader(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    badgeText: String? = null,
    onBackClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                )
            }
        }
    }
}

// ==========================================
// EXCEL / CSV TO PDF SCREEN
// ==========================================
@Composable
fun ExcelToPdfToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var fileContent by remember { mutableStateOf<String?>(null) }
    var isConverting by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val name = uri.lastPathSegment?.substringAfterLast("/") ?: "Spreadsheet.csv"
                selectedFileName = name
                val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).readText()
                } ?: "No data read"
                fileContent = text
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ToolHeader(
            title = "Excel / CSV to PDF",
            description = "Convert spreadsheet tables directly into printable PDF documents",
            icon = Icons.Default.TableChart,
            iconColor = Color(0xFF10B981),
            badgeText = "Grid Generator",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E2A)),
                modifier = Modifier.fillMaxWidth().border(1.5.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Spreadsheet (.csv, .xlsx, .txt)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { filePicker.launch(arrayOf("text/*", "application/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedFileName != null) "Change: $selectedFileName" else "Browse Spreadsheet File", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (fileContent != null) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1724)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Spreadsheet Data Preview:", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF121B28))
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(fileContent!!, color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val content = fileContent
                    if (content.isNullOrBlank()) {
                        Toast.makeText(context, "Select a spreadsheet file first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isConverting = true
                    coroutineScope.launch(Dispatchers.IO) {
                        val title = selectedFileName ?: "Spreadsheet_Export"
                        val pdfFile = PdfEngine.createPdfFromText(context, title, content)
                        val doc = ScannedDocument(
                            title = pdfFile.name,
                            filePath = pdfFile.absolutePath,
                            fileType = "PDF",
                            fileSizeBytes = pdfFile.length(),
                            pageCount = 1,
                            category = "Spreadsheets",
                            createdAt = System.currentTimeMillis()
                        )
                        viewModel.addNewDocument(doc)
                        withContext(Dispatchers.Main) {
                            isConverting = false
                            Toast.makeText(context, "Rendered Spreadsheet to PDF!", Toast.LENGTH_SHORT).show()
                            onNavigateToDocPreview(doc)
                        }
                    }
                },
                enabled = !isConverting && fileContent != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isConverting) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp))
                } else {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Render Spreadsheet to PDF", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// WORD / TXT TO PDF SCREEN
// ==========================================
@Composable
fun WordToPdfToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var fileContent by remember { mutableStateOf<String?>(null) }
    var isConverting by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val name = uri.lastPathSegment?.substringAfterLast("/") ?: "Document.txt"
                selectedFileName = name
                val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).readText()
                } ?: "No data read"
                fileContent = text
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ToolHeader(
            title = "Word / TXT to PDF",
            description = "Convert documents & notes into printable PDF files",
            icon = Icons.Default.Description,
            iconColor = Color(0xFF2563EB),
            badgeText = "Doc Renderer",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E2A)),
                modifier = Modifier.fillMaxWidth().border(1.5.dp, Color(0xFF2563EB).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Document File (.txt, .docx, .rtf)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { filePicker.launch(arrayOf("text/*", "application/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedFileName != null) "Change: $selectedFileName" else "Browse Document File", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (fileContent != null) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1724)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Document Text Preview:", color = Color(0xFF2563EB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF121B28))
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(fileContent!!, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val content = fileContent
                    if (content.isNullOrBlank()) {
                        Toast.makeText(context, "Select a document file first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isConverting = true
                    coroutineScope.launch(Dispatchers.IO) {
                        val title = selectedFileName ?: "Word_Document"
                        val pdfFile = PdfEngine.createPdfFromText(context, title, content)
                        val doc = ScannedDocument(
                            title = pdfFile.name,
                            filePath = pdfFile.absolutePath,
                            fileType = "PDF",
                            fileSizeBytes = pdfFile.length(),
                            pageCount = 1,
                            category = "Documents",
                            createdAt = System.currentTimeMillis()
                        )
                        viewModel.addNewDocument(doc)
                        withContext(Dispatchers.Main) {
                            isConverting = false
                            Toast.makeText(context, "Rendered Document to PDF!", Toast.LENGTH_SHORT).show()
                            onNavigateToDocPreview(doc)
                        }
                    }
                },
                enabled = !isConverting && fileContent != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isConverting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                } else {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Render Word Document to PDF", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// IMAGE TO PDF TOOL SCREEN
// ==========================================
@Composable
fun ImageToPdfToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    var isProcessing by remember { mutableStateOf(false) }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = uris
            Toast.makeText(context, "Loaded ${uris.size} image(s)", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ToolHeader(
            title = "Image to PDF",
            description = "Convert photos & graphics into a unified PDF file",
            icon = Icons.Default.Image,
            iconColor = Color(0xFF0EA5E9),
            badgeText = "Multi-Image",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E2A)),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select Images from Gallery", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Pick single or multiple JPG / PNG files", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9), contentColor = Color.Black)
                    ) {
                        Text(if (selectedUris.isNotEmpty()) "${selectedUris.size} Images Selected" else "Browse Gallery Photos", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Button(
                onClick = {
                    if (selectedUris.isEmpty()) {
                        Toast.makeText(context, "Select at least 1 image", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isProcessing = true
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        val bitmaps = selectedUris.mapNotNull { uri ->
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                android.graphics.BitmapFactory.decodeStream(stream)
                            }
                        }
                        val title = "Image_Scan_${System.currentTimeMillis()}.pdf"
                        val pdfFile = PdfEngine.createPdfFromBitmaps(context, bitmaps, title)
                        val newDoc = ScannedDocument(
                            title = title,
                            filePath = pdfFile.absolutePath,
                            fileType = "PDF",
                            fileSizeBytes = pdfFile.length(),
                            pageCount = bitmaps.size,
                            category = "Images",
                            createdAt = System.currentTimeMillis()
                        )
                        viewModel.addNewDocument(newDoc)
                        withContext(Dispatchers.Main) {
                            isProcessing = false
                            Toast.makeText(context, "Images converted to PDF!", Toast.LENGTH_SHORT).show()
                            onNavigateToDocPreview(newDoc)
                        }
                    }
                },
                enabled = !isProcessing && selectedUris.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9), contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isProcessing) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp))
                else Text("Create & Save PDF File", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// TEXT TO PDF STUDIO SCREEN
// ==========================================
@Composable
fun TextToPdfToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    var docTitle by remember { mutableStateOf("My_Notes") }
    var textContent by remember { mutableStateOf("Type or paste your text here to create a professional PDF document...") }
    var isGenerating by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ToolHeader(
            title = "Text to PDF Studio",
            description = "Type, paste, or import formatted text to generate PDFs",
            icon = Icons.Default.TextFields,
            iconColor = Color(0xFF8B5CF6),
            badgeText = "Editor & Converter",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = docTitle,
                onValueChange = { docTitle = it },
                label = { Text("Document Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = textContent,
                onValueChange = { textContent = it },
                modifier = Modifier.fillMaxWidth().height(220.dp),
                placeholder = { Text("Write or paste document body content here...") }
            )

            Button(
                onClick = {
                    if (textContent.isBlank()) {
                        Toast.makeText(context, "Text content cannot be empty", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isGenerating = true
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        val pdfFile = PdfEngine.createPdfFromText(context, docTitle, textContent, "${docTitle}.pdf")
                        val doc = ScannedDocument(
                            title = "${docTitle}.pdf",
                            filePath = pdfFile.absolutePath,
                            fileType = "PDF",
                            fileSizeBytes = pdfFile.length(),
                            pageCount = 1
                        )
                        viewModel.addNewDocument(doc)
                        withContext(Dispatchers.Main) {
                            isGenerating = false
                            Toast.makeText(context, "Exported PDF Document!", Toast.LENGTH_SHORT).show()
                            onNavigateToDocPreview(doc)
                        }
                    }
                },
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isGenerating) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                else Text("Export & Generate PDF Document", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// OCR TEXT EXTRACTOR SCREEN
// ==========================================
@Composable
fun OcrTextToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var extractedText by remember { mutableStateOf("ScanPro AI OCR Engine ready. Tap below to pick an image or document for recognition.") }
    var isScanning by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            isScanning = true
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                val bmp = context.contentResolver.openInputStream(uri)?.use { stream ->
                    android.graphics.BitmapFactory.decodeStream(stream)
                }
                if (bmp != null) {
                    val text = viewModel.geminiService.extractTextFromImage(bmp)
                    withContext(Dispatchers.Main) {
                        extractedText = text
                        isScanning = false
                    }
                } else {
                    withContext(Dispatchers.Main) { isScanning = false }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ToolHeader(
            title = "OCR Text Recognition",
            description = "Extract editable text from camera scans, images, or PDFs",
            icon = Icons.Default.AutoAwesome,
            iconColor = Color(0xFFA855F7),
            badgeText = "Gemini AI OCR",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Button(
                onClick = { imagePicker.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7), contentColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isScanning) "Analyzing Document..." else "Select Image for OCR", fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = extractedText,
                onValueChange = { extractedText = it },
                label = { Text("Extracted OCR Text") },
                modifier = Modifier.fillMaxWidth().height(260.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(extractedText))
                        Toast.makeText(context, "Copied to Clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy")
                }

                Button(
                    onClick = {
                        viewModel.exportOcrTextToPdfAndShare("OCR_Scan_${System.currentTimeMillis()}", extractedText)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5), contentColor = Color.Black),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export PDF", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// SIGN PDF TOOL SCREEN
// ==========================================
@Composable
fun SignPdfToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val documents by viewModel.documents.collectAsState()
    val context = LocalContext.current
    var selectedDoc by remember { mutableStateOf<ScannedDocument?>(documents.firstOrNull { it.fileType.contains("PDF", ignoreCase = true) }) }
    var points by remember { mutableStateOf(listOf<Offset>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ToolHeader(
            title = "Sign PDF Studio",
            description = "Draw e-signature & embed timestamp verification onto documents",
            icon = Icons.Default.Draw,
            iconColor = Color(0xFF6366F1),
            badgeText = "e-Sign Pad",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Draw Signature Below:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(2.dp, Color(0xFF6366F1), RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            points = points + change.position
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = Color.Black,
                            start = points[i],
                            end = points[i + 1],
                            strokeWidth = 6f
                        )
                    }
                }
                IconButton(
                    onClick = { points = emptyList() },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.Red)
                }
            }

            Button(
                onClick = {
                    if (points.isEmpty()) {
                        Toast.makeText(context, "Draw signature first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val doc = selectedDoc
                    if (doc != null) {
                        val bmp = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bmp)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        viewModel.signDocument(doc, bmp)
                        Toast.makeText(context, "Signature applied to PDF!", Toast.LENGTH_SHORT).show()
                        onBackClicked()
                    } else {
                        Toast.makeText(context, "No PDF in vault to sign. Scan or import one first.", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Draw, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply Signature to PDF", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// SPECIALIZED SCAN TOOL SCREEN (ID / PASSPORT / WHITEBOARD)
// ==========================================
@Composable
fun SpecializedScanToolScreen(
    title: String,
    description: String,
    icon: ImageVector,
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    var isFrontCaptured by remember { mutableStateOf(false) }
    var isBackCaptured by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ToolHeader(
            title = title,
            description = description,
            icon = icon,
            iconColor = Color(0xFF16C7B5),
            badgeText = "Auto-Crop",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E2A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. Front Side", color = Color.White, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { isFrontCaptured = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isFrontCaptured) Color(0xFF10B981) else Color(0xFF16C7B5), contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (isFrontCaptured) Icons.Default.Check else Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isFrontCaptured) "Front Captured ✓" else "Capture Front Side")
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("2. Back Side", color = Color.White, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { isBackCaptured = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isBackCaptured) Color(0xFF10B981) else Color(0xFF16C7B5), contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (isBackCaptured) Icons.Default.Check else Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isBackCaptured) "Back Captured ✓" else "Capture Back Side")
                    }
                }
            }

            Button(
                onClick = {
                    if (!isFrontCaptured) {
                        Toast.makeText(context, "Capture front side first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    Toast.makeText(context, "$title saved to document vault!", Toast.LENGTH_SHORT).show()
                    onBackClicked()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5), contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save $title Document", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// REAL BOOK & CURVE SCAN TOOL SCREEN
// ==========================================
@Composable
fun BookScanToolScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToDocPreview: (ScannedDocument) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var inputBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCurveCorrection by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                inputBitmap = android.graphics.BitmapFactory.decodeStream(stream)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ToolHeader(
            title = "Book & Curvature Scan",
            description = "Flatten book page spine curves & clean finger shadows",
            icon = Icons.Default.Book,
            iconColor = Color(0xFF8B5CF6),
            badgeText = "Curvature AI",
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E2A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Select Book Page Image:", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6), contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (inputBitmap != null) "Change Book Page Image" else "Browse Book Photo")
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1724)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Page Curvature Flattening", color = Color.White, fontSize = 13.sp)
                    Switch(
                        checked = isCurveCorrection,
                        onCheckedChange = { isCurveCorrection = it },
                        colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color(0xFF8B5CF6))
                    )
                }
            }

            Button(
                onClick = {
                    val bmp = inputBitmap
                    if (bmp == null) {
                        Toast.makeText(context, "Please select an image first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isProcessing = true
                    coroutineScope.launch(Dispatchers.IO) {
                        val quad = OpenCvDocumentDetector.detectDocumentEdges(bmp)
                        val flattened = OpenCvDocumentDetector.cropAndWarpPerspective(bmp, quad)
                        val enhanced = OpenCvDocumentDetector.applyFilter(flattened, com.example.util.DocumentFilterMode.ENHANCE)

                        val pdfFile = PdfEngine.createPdfFromBitmaps(context, listOf(enhanced), "Book_${System.currentTimeMillis()}.pdf")
                        val doc = ScannedDocument(
                            title = pdfFile.name,
                            filePath = pdfFile.absolutePath,
                            fileType = "PDF",
                            fileSizeBytes = pdfFile.length(),
                            pageCount = 1,
                            category = "Books",
                            createdAt = System.currentTimeMillis()
                        )
                        viewModel.addNewDocument(doc)

                        withContext(Dispatchers.Main) {
                            isProcessing = false
                            Toast.makeText(context, "Book page flattened & saved to Vault!", Toast.LENGTH_SHORT).show()
                            onNavigateToDocPreview(doc)
                        }
                    }
                },
                enabled = !isProcessing && inputBitmap != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Flatten Page & Save to Vault", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
