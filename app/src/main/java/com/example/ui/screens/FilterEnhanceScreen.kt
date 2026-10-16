package com.example.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DocumentViewModel
import com.example.ui.components.ProcessingOverlay
import com.example.ui.theme.ScannerBlue
import com.example.ui.theme.ScannerCyan
import com.example.ui.theme.ScannerEmerald
import com.example.util.AdMobManager
import com.example.util.ImageProcessor
import com.example.util.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FilterOption(
    val mode: ImageProcessor.FilterMode,
    val title: String,
    val icon: ImageVector
)

val filterOptions = listOf(
    FilterOption(ImageProcessor.FilterMode.DOCUMENT, "Tài liệu", Icons.Default.Description),
    FilterOption(ImageProcessor.FilterMode.AUTO_ENHANCE, "Tự động", Icons.Default.AutoFixHigh),
    FilterOption(ImageProcessor.FilterMode.ORIGINAL, "Ảnh gốc", Icons.Default.Image),
    FilterOption(ImageProcessor.FilterMode.BLACK_AND_WHITE, "Trắng đen", Icons.Default.Contrast),
    FilterOption(ImageProcessor.FilterMode.GRAYSCALE, "Ảnh xám", Icons.Default.Filter),
    FilterOption(ImageProcessor.FilterMode.COLOR_BOOST, "Màu tươi", Icons.Default.ColorLens)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterEnhanceScreen(
    viewModel: DocumentViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCameraForMore: () -> Unit,
    onDocumentSaved: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val draftPages by viewModel.draftPages.collectAsState()
    val activeIndex by viewModel.activeDraftIndex.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val processingMessage by viewModel.processingMessage.collectAsState()

    BackHandler {
        onNavigateBack()
    }

    if (draftPages.isEmpty() || activeIndex !in draftPages.indices) {
        onNavigateBack()
        return
    }

    val currentDraft = draftPages[activeIndex]
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isWarpingPreview by remember { mutableStateOf(false) }

    // Save Dialog State
    var showSaveDialog by remember { mutableStateOf(false) }
    var documentTitle by remember {
        mutableStateOf("ScanDoc_${SimpleDateFormat("ddMMyy_HHmm", Locale.getDefault()).format(Date())}")
    }
    var selectedPageSize by remember { mutableStateOf(PdfGenerator.PageSize.A4) }
    var selectedQuality by remember { mutableStateOf(PdfGenerator.PdfQuality.HIGH) }

    // Recompute warped & filtered preview whenever draft changes
    LaunchedEffect(currentDraft.originalImagePath, currentDraft.corners, currentDraft.filterMode, currentDraft.rotationAngle) {
        isWarpingPreview = true
        withContext(Dispatchers.IO) {
            val original = ImageProcessor.loadBitmap(context, currentDraft.originalImagePath, 1600, 1600)
            if (original != null) {
                val warped = if (currentDraft.corners.size == 4) {
                    ImageProcessor.cropAndWarpPerspective(original, currentDraft.corners)
                } else {
                    original
                }

                val rotated = if (currentDraft.rotationAngle != 0) {
                    ImageProcessor.rotateBitmap(warped, currentDraft.rotationAngle.toFloat())
                } else {
                    warped
                }

                val filtered = ImageProcessor.applyFilter(rotated, currentDraft.filterMode)
                previewBitmap = filtered
            }
        }
        isWarpingPreview = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Bộ lọc & Nâng cao (${activeIndex + 1}/${draftPages.size})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại chỉnh góc",
                        tint = Color.White
                    )
                }
            },
            actions = {
                // Delete current draft page if > 1 page
                if (draftPages.size > 1) {
                    IconButton(
                        onClick = { viewModel.removeDraftPage(activeIndex) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Xóa trang này",
                            tint = Color.White
                        )
                    }
                }

                // Add next page button
                IconButton(onClick = onNavigateToCameraForMore) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Chụp tiếp",
                        tint = ScannerCyan
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
        )

        // Main Document Preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val bmp = previewBitmap
            if (bmp != null && !isWarpingPreview) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Preview Document",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                CircularProgressIndicator(color = ScannerCyan)
            }
        }

        // Multi-page Pager Bar (if > 1 page)
        if (draftPages.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (activeIndex > 0) viewModel.setActiveDraftIndex(activeIndex - 1)
                    },
                    enabled = activeIndex > 0
                ) {
                    Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Trang trước", tint = Color.White)
                }
                Text(
                    text = "Trang ${activeIndex + 1} / ${draftPages.size}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                IconButton(
                    onClick = {
                        if (activeIndex < draftPages.size - 1) viewModel.setActiveDraftIndex(activeIndex + 1)
                    },
                    enabled = activeIndex < draftPages.size - 1
                ) {
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Trang sau", tint = Color.White)
                }
            }
        }

        // Filters Carousel
        Surface(
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
                ) {
                    items(filterOptions) { opt ->
                        val isSelected = currentDraft.filterMode == opt.mode
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.updateDraftFilter(activeIndex, opt.mode)
                                }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) ScannerCyan else Color(0xFF475569),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            color = if (isSelected) ScannerBlue.copy(alpha = 0.25f) else Color(0xFF334155),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = opt.icon,
                                    contentDescription = opt.title,
                                    tint = if (isSelected) ScannerCyan else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = opt.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) ScannerCyan else Color.LightGray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: Scan Next Page & Save
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToCameraForMore,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Thêm trang")
                    }

                    Button(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .testTag("save_scanned_document_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerEmerald)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lưu tài liệu", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }

    // Save & PDF Export Configuration Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(text = "Lưu tài liệu PDF", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = documentTitle,
                        onValueChange = { documentTitle = it },
                        label = { Text("Tên tài liệu") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("document_title_input")
                    )

                    Text(
                        text = "Khổ giấy:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(
                            PdfGenerator.PageSize.A4,
                            PdfGenerator.PageSize.A5,
                            PdfGenerator.PageSize.LETTER
                        ).forEach { sizeOption ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedPageSize == sizeOption) ScannerBlue else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { selectedPageSize = sizeOption }
                            ) {
                                Text(
                                    text = sizeOption.name,
                                    color = if (selectedPageSize == sizeOption) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Chất lượng:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(
                            PdfGenerator.PdfQuality.HIGH,
                            PdfGenerator.PdfQuality.MEDIUM,
                            PdfGenerator.PdfQuality.COMPACT
                        ).forEach { qualOption ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedQuality == qualOption) ScannerEmerald else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { selectedQuality = qualOption }
                            ) {
                                Text(
                                    text = qualOption.title,
                                    color = if (selectedQuality == qualOption) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveDialog = false
                        viewModel.saveScanSession(
                            title = documentTitle,
                            pageSize = selectedPageSize,
                            quality = selectedQuality,
                            onSuccess = { savedDocId ->
                                // Show polite interstitial ad after task completion if allowed by frequency capping
                                if (activity != null) {
                                    AdMobManager.showInterstitialIfAllowed(activity) {
                                        onDocumentSaved(savedDocId)
                                    }
                                } else {
                                    onDocumentSaved(savedDocId)
                                }
                            }
                        )
                    },
                    modifier = Modifier.testTag("confirm_save_pdf_button")
                ) {
                    Text("Tạo PDF & Lưu")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSaveDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (isProcessing) {
        ProcessingOverlay(message = processingMessage)
    }
}
