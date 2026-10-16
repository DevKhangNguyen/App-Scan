package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DocumentEntity
import com.example.ui.DocumentViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.theme.ScannerBlue
import com.example.ui.theme.ScannerEmerald
import com.example.util.AdBannerView
import com.example.util.FileUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    viewModel: DocumentViewModel,
    onNavigateToDocumentDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val documents by viewModel.documents.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var docToDelete by remember { mutableStateOf<DocumentEntity?>(null) }
    var docToRename by remember { mutableStateOf<DocumentEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tất cả tài liệu",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AdBannerView(modifier = Modifier.testTag("docs_ad_banner"))
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("document_search_input"),
                placeholder = { Text("Tìm theo tên hoặc nội dung chữ...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Tìm kiếm")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Xóa")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Results count
            Text(
                text = "${documents.size} tài liệu tìm thấy",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            if (documents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Không tìm thấy tài liệu phù hợp" else "Chưa có tài liệu",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(documents, key = { it.id }) { doc ->
                        DocumentRowItem(
                            document = doc,
                            onClick = { onNavigateToDocumentDetail(doc.id) },
                            onSharePdf = {
                                doc.pdfPath?.let { path ->
                                    val f = File(path)
                                    if (f.exists()) {
                                        FileUtils.shareFile(context, f, "application/pdf", doc.title)
                                    }
                                }
                            },
                            onRename = {
                                docToRename = doc
                                renameInputText = doc.title
                            },
                            onDelete = {
                                docToDelete = doc
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    docToDelete?.let { doc ->
        ConfirmDeleteDialog(
            title = "Xóa tài liệu?",
            message = "Bạn có chắc muốn xóa \"${doc.title}\"? Thao tác này sẽ xóa các trang đã scan khỏi máy.",
            onConfirm = {
                viewModel.deleteDocument(doc.id)
                docToDelete = null
            },
            onDismiss = {
                docToDelete = null
            }
        )
    }

    // Rename Dialog
    docToRename?.let { doc ->
        AlertDialog(
            onDismissRequest = { docToRename = null },
            title = { Text("Đổi tên tài liệu", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    singleLine = true,
                    label = { Text("Tên tài liệu mới") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameDocument(doc.id, renameInputText)
                        docToRename = null
                    }
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { docToRename = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun DocumentRowItem(
    document: DocumentEntity,
    onClick: () -> Unit,
    onSharePdf: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("doc_item_${document.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (document.thumbnailPath != null && File(document.thumbnailPath).exists()) {
                    AsyncImage(
                        model = File(document.thumbnailPath),
                        contentDescription = "Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = ScannerBlue,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ScannerBlue.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "${document.pageCount} trang",
                            fontSize = 11.sp,
                            color = ScannerBlue,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(document.updatedAt))
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!document.ocrSummary.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = document.ocrSummary,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Tùy chọn")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Chia sẻ PDF") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSharePdf()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Đổi tên") },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Xóa", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
