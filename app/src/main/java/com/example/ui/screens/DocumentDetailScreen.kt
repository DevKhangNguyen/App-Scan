package com.example.ui.screens

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.DocumentViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.ProcessingOverlay
import com.example.ui.theme.ScannerBlue
import com.example.ui.theme.ScannerCyan
import com.example.ui.theme.ScannerEmerald
import com.example.util.AdBannerView
import com.example.util.FileUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    documentId: Long,
    viewModel: DocumentViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToOcrResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentDocWithPages by viewModel.currentDocumentWithPages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val processingMessage by viewModel.processingMessage.collectAsState()

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameTitleInput by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    BackHandler {
        onNavigateBack()
    }

    val doc = currentDocWithPages?.document
    val pages = currentDocWithPages?.pages ?: emptyList()

    // SAF Document Exporter
    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { targetUri ->
            doc?.pdfPath?.let { path ->
                viewModel.exportPdfToUri(path, targetUri) { success ->
                    if (success) {
                        Toast.makeText(context, "Đã xuất và lưu file PDF thành công!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Lỗi khi lưu file PDF", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = doc?.title ?: "Chi tiết tài liệu",
                        maxLines = 1,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        renameTitleInput = doc?.title ?: ""
                        showRenameDialog = true
                    }) {
                        Icon(imageVector = Icons.Default.DriveFileRenameOutline, contentDescription = "Đổi tên")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Xóa",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AdBannerView(modifier = Modifier.testTag("detail_ad_banner"))
        }
    ) { innerPadding ->
        if (doc == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Đang tải tài liệu...")
            }
            return@Scaffold
        }

        val pdfFile = doc.pdfPath?.let { File(it) }
        val fileSizeKb = if (pdfFile != null && pdfFile.exists()) pdfFile.length() / 1024 else 0

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
        ) {
            // Header Info Card
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ScannerBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = ScannerBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = doc.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                val dateStr = SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault()).format(Date(doc.updatedAt))
                                Text(
                                    text = "$dateStr • ${doc.pageCount} trang • ${fileSizeKb} KB",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Share PDF
                            Button(
                                onClick = {
                                    if (pdfFile != null && pdfFile.exists()) {
                                        FileUtils.shareFile(context, pdfFile, "application/pdf", doc.title)
                                    } else {
                                        Toast.makeText(context, "Chưa tìm thấy file PDF", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("share_pdf_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ScannerBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chia sẻ")
                            }

                            // Open PDF Viewer
                            OutlinedButton(
                                onClick = {
                                    if (pdfFile != null && pdfFile.exists()) {
                                        FileUtils.openPdfWithExternalViewer(context, pdfFile)
                                    } else {
                                        Toast.makeText(context, "Chưa tìm thấy file PDF", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mở PDF")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Second Row: Save to Device & OCR Text
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Save to SAF
                            OutlinedButton(
                                onClick = {
                                    val safeName = "${doc.title}.pdf"
                                    exportPdfLauncher.launch(safeName)
                                },
                                modifier = Modifier.weight(1f).testTag("save_to_device_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lưu vào máy")
                            }

                            // OCR Text Viewer
                            Button(
                                onClick = onNavigateToOcrResult,
                                modifier = Modifier.weight(1f).testTag("view_ocr_text_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ScannerCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Đọc chữ OCR")
                            }
                        }
                    }
                }
            }

            // Pages Section Title
            item {
                Text(
                    text = "Các trang trong tài liệu (${pages.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Pages List
            itemsIndexed(pages) { index, page ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ScannerBlue.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "Trang ${index + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ScannerBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = File(page.croppedImagePath),
                                contentDescription = "Page ${index + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bộ lọc: ${page.filterType}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!page.ocrText.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = page.ocrText,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Đổi tên tài liệu", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameTitleInput,
                    onValueChange = { renameTitleInput = it },
                    singleLine = true,
                    label = { Text("Tên tài liệu") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameDocument(documentId, renameTitleInput)
                        showRenameDialog = false
                    }
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRenameDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Delete Dialog
    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = "Xóa tài liệu?",
            message = "Bạn có chắc chắn muốn xóa tài liệu này? Toàn bộ file scan trên máy sẽ bị xóa.",
            onConfirm = {
                viewModel.deleteDocument(documentId) {
                    showDeleteConfirm = false
                    onNavigateBack()
                }
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (isProcessing) {
        ProcessingOverlay(message = processingMessage)
    }
}
