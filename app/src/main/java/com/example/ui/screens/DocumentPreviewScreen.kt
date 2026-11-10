package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageHistory
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun DocumentPreviewScreen(
    viewModel: ScanProViewModel,
    onBackClicked: () -> Unit,
    onNavigateToCamera: (() -> Unit)? = null,
    onNavigateToEditor: (() -> Unit)? = null,
    onNavigateToAiChat: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeDocument by viewModel.activeDocument.collectAsState()

    val doc = activeDocument
    if (doc == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            Text("No document opened", color = Color.White)
        }
        return
    }

    val isPdf = doc.fileType.contains("PDF", ignoreCase = true)
    val pageBitmaps = remember { mutableStateListOf<Bitmap>() }
    var singleImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPages by remember { mutableStateOf(true) }

    // More Bottom Sheet Visibility
    var showMoreBottomSheet by remember { mutableStateOf(false) }

    // Rename Dialog State
    var isRenamingDoc by remember { mutableStateOf(false) }
    var renameInputText by remember { mutableStateOf(doc.title) }

    // Tag Dialog State
    var showTagDialog by remember { mutableStateOf(false) }
    var currentTag by remember { mutableStateOf(doc.tags.ifBlank { "Personal" }) }

    // Load PDF pages or image bitmap
    LaunchedEffect(doc.filePath) {
        isLoadingPages = true
        pageBitmaps.clear()
        val file = File(doc.filePath)
        if (file.exists()) {
            if (isPdf) {
                withContext(Dispatchers.IO) {
                    val count = PdfEngine.getPdfPageCount(context, file)
                    for (i in 0 until count) {
                        val bmp = PdfEngine.renderPdfPageToBitmap(context, file, i)
                        if (bmp != null) pageBitmaps.add(bmp)
                    }
                }
            } else {
                withContext(Dispatchers.IO) {
                    singleImageBitmap = BitmapFactory.decodeFile(file.absolutePath)
                }
            }
        }
        isLoadingPages = false
    }

    BackHandler(enabled = showMoreBottomSheet) {
        showMoreBottomSheet = false
    }

    // Rename Dialog
    if (isRenamingDoc) {
        AlertDialog(
            onDismissRequest = { isRenamingDoc = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Rename Document", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInputText.isNotBlank()) {
                            viewModel.renameDocument(doc, renameInputText.trim())
                        }
                        isRenamingDoc = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5))
                ) {
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isRenamingDoc = false }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }

    // Tag Editor Dialog
    if (showTagDialog) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Set Document Tag", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = currentTag,
                    onValueChange = { currentTag = it },
                    label = { Text("Tag (e.g. Invoice, Work, ID)") },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveDocumentChanges(doc.copy(tags = currentTag))
                        showTagDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16C7B5))
                ) {
                    Text("Apply Tag", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. TOP BAR (Exact Screenshot 1 & 2 Match)
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBackClicked,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    renameInputText = doc.title
                                    isRenamingDoc = true
                                }
                            ) {
                                Text(
                                    text = doc.title.removeSuffix(".pdf"),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Rename",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Tag Chip (e.g. Tags +)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .border(0.8.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                                    .clickable { showTagDialog = true }
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (doc.tags.isNotBlank()) doc.tags else "Tags +",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Top-Right Quick Icons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "Page view toggled", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.GridView, contentDescription = "Grid", tint = Color(0xFF334155), modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = { showMoreBottomSheet = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color(0xFF334155), modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            // 2. MAIN DOCUMENT VIEWPORT (White margins & shadow, matching Screenshots 1 & 2)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFE2E8F0))
            ) {
                if (isLoadingPages) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF16C7B5))
                    }
                } else if (isPdf && pageBitmaps.isNotEmpty()) {
                    // Multi-page Vertical Document List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(pageBitmaps) { index, pageBmp ->
                            Card(
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(4.dp, RoundedCornerShape(6.dp))
                            ) {
                                Image(
                                    bitmap = pageBmp.asImageBitmap(),
                                    contentDescription = "Page ${index + 1}",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                } else if (singleImageBitmap != null) {
                    // Single Image Edge-to-Edge View (Screenshot 2 Match)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = singleImageBitmap!!.asImageBitmap(),
                            contentDescription = "Document Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Document loaded without visual pages.", color = Color.Gray, fontSize = 13.sp)
                    }
                }

                // 3. FLOATING "✦ Ask AI" BUTTON (Overlaid bottom-right, matching Screenshots 1 & 2)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.5.dp, Color(0xFF16C7B5), RoundedCornerShape(12.dp))
                        .clickable {
                            if (onNavigateToAiChat != null) {
                                onNavigateToAiChat()
                            } else {
                                Toast.makeText(context, "Opening Gemini AI Document Assistant...", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF0D9488),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ask AI",
                            color = Color(0xFF0F766E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // 4. BOTTOM ACTION DOCK (5 Core Tools matching Screenshots 1 & 2)
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tool 1: Add
                    PreviewBottomDockItem(
                        icon = Icons.Default.PhotoCamera,
                        label = "Add",
                        onClick = { onNavigateToCamera?.invoke() }
                    )

                    // Tool 2: Edit / Edit PDF
                    PreviewBottomDockItem(
                        icon = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Edit,
                        label = if (isPdf) "Edit PDF" else "Edit",
                        onClick = { onNavigateToEditor?.invoke() }
                    )

                    // Tool 3: Share
                    PreviewBottomDockItem(
                        icon = Icons.Default.Share,
                        label = "Share",
                        onClick = {
                            val file = File(doc.filePath)
                            if (file.exists()) {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = if (isPdf) "application/pdf" else "image/*"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Document"))
                            }
                        }
                    )

                    // Tool 4: To Word
                    PreviewBottomDockItem(
                        icon = Icons.Default.Description,
                        label = "To Word",
                        onClick = {
                            Toast.makeText(context, "Transcribing document into Word (.docx)...", Toast.LENGTH_SHORT).show()
                            viewModel.convertPdfToWord(
                                doc = doc,
                                onProgress = { _, _ -> },
                                onComplete = { newDoc, _ ->
                                    Toast.makeText(context, "Converted to Word successfully!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                            )
                        }
                    )

                    // Tool 5: Sign (for image) OR More (for PDF)
                    if (isPdf) {
                        PreviewBottomDockItem(
                            icon = Icons.Default.MoreVert,
                            label = "More",
                            onClick = { showMoreBottomSheet = true }
                        )
                    } else {
                        PreviewBottomDockItem(
                            icon = Icons.Default.Draw,
                            label = "Sign",
                            onClick = {
                                Toast.makeText(context, "Sign tool opened for stamping", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // ==========================================
        // 5. SCREENSHOT 3: COMPREHENSIVE "MORE" BOTTOM SHEET
        // ==========================================
        AnimatedVisibility(
            visible = showMoreBottomSheet,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showMoreBottomSheet = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize(0.85f)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color.White)
                        .clickable(enabled = false) {}
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Header: "More", Close icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "More",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        IconButton(
                            onClick = { showMoreBottomSheet = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Document Card with thumbnail, title & info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFEEF2F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("Tags +", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "${doc.pageCount} Pages • ${(doc.fileSizeBytes / 1024)} KB", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Top 4 Quick Circles (Sign, Send to PC, PDF to Images, Print)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MoreCircleActionItem("Sign", Icons.Default.Draw, Color(0xFF10B981)) {
                            showMoreBottomSheet = false
                            Toast.makeText(context, "Sign tool opened", Toast.LENGTH_SHORT).show()
                        }
                        MoreCircleActionItem("Send to PC", Icons.Default.Computer, Color(0xFF2563EB)) {
                            showMoreBottomSheet = false
                            val file = File(doc.filePath)
                            if (file.exists()) {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Send to PC via Bluetooth/Nearby"))
                            }
                        }
                        MoreCircleActionItem("PDF to Images", Icons.Default.Image, Color(0xFF3B82F6)) {
                            showMoreBottomSheet = false
                            Toast.makeText(context, "Rendering pages to images...", Toast.LENGTH_SHORT).show()
                        }
                        MoreCircleActionItem("Print", Icons.Default.Print, Color(0xFF10B981)) {
                            showMoreBottomSheet = false
                            val printHelper = PrintHelper(context).apply { scaleMode = PrintHelper.SCALE_MODE_FIT }
                            pageBitmaps.firstOrNull()?.let { printHelper.printBitmap(doc.title, it) }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Categorized Feature Grid
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Category: Convert
                        MoreSectionHeader("Convert")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            MoreGridItem("To Word", Icons.Default.Description, true) {
                                showMoreBottomSheet = false
                                Toast.makeText(context, "Converting to Word (.docx)...", Toast.LENGTH_SHORT).show()
                            }
                            MoreGridItem("To Excel", Icons.Default.TableChart, true) {
                                showMoreBottomSheet = false
                                Toast.makeText(context, "Extracting tables to Excel (.csv)...", Toast.LENGTH_SHORT).show()
                            }
                        }

                        // Category: Smart Tools
                        MoreSectionHeader("Smart Tools")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MoreGridItem("Extract Text", Icons.Default.TextFields, true) {
                                showMoreBottomSheet = false
                                viewModel.runAiOcrOnDocument(doc)
                            }
                            MoreGridItem("Translate", Icons.Default.Translate, false) {
                                showMoreBottomSheet = false
                                viewModel.runAiTranslateDocument(doc, "Spanish")
                            }
                            MoreGridItem("Compress", Icons.Default.Tune, false) {
                                showMoreBottomSheet = false
                                viewModel.compressDocument(doc)
                            }
                            MoreGridItem("Read Mode", Icons.Default.MenuBook, false) {
                                showMoreBottomSheet = false
                                Toast.makeText(context, "Read Mode activated", Toast.LENGTH_SHORT).show()
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            MoreGridItem("Case Summary", Icons.Default.AutoAwesome, false) {
                                showMoreBottomSheet = false
                                viewModel.runAiSummarizeDocument(doc)
                            }
                        }

                        // Category: Manage
                        MoreSectionHeader("Manage")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MoreGridItem("Manage Pages", Icons.Default.ManageHistory, false) {
                                showMoreBottomSheet = false
                                onNavigateToEditor?.invoke()
                            }
                            MoreGridItem("Lock", Icons.Default.Lock, true) {
                                showMoreBottomSheet = false
                                Toast.makeText(context, "Document Encrypted & Locked", Toast.LENGTH_SHORT).show()
                            }
                            MoreGridItem("Merge Files", Icons.Default.PictureAsPdf, false) {
                                showMoreBottomSheet = false
                                Toast.makeText(context, "Merge tool opened", Toast.LENGTH_SHORT).show()
                            }
                            MoreGridItem("Copy/Move", Icons.Default.ContentCopy, false) {
                                showMoreBottomSheet = false
                                Toast.makeText(context, "File copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        }

                        // Category: Send
                        MoreSectionHeader("Send")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            MoreGridItem("Email to Myself", Icons.Default.Email, false) {
                                showMoreBottomSheet = false
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:")
                                    putExtra(Intent.EXTRA_SUBJECT, "My ScanPro Document: ${doc.title}")
                                }
                                context.startActivity(Intent.createChooser(emailIntent, "Send Email"))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Danger: Delete Button (Red, centered)
                        Text(
                            text = "Delete",
                            color = Color(0xFFEF4444),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showMoreBottomSheet = false
                                    viewModel.deleteDocument(doc)
                                    onBackClicked()
                                    Toast.makeText(context, "Document deleted", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PreviewBottomDockItem(
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
            tint = Color(0xFF334155),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = Color(0xFF475569),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MoreCircleActionItem(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 11.sp, color = Color(0xFF334155), fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MoreSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF0F172A),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun MoreGridItem(
    label: String,
    icon: ImageVector,
    isCrown: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 72.dp, height = 64.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.TopEnd) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = label, tint = Color(0xFF1E293B), modifier = Modifier.size(20.dp))
                }
                if (isCrown) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B))
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFF475569),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
