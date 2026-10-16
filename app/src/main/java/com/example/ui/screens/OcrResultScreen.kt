package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DocumentViewModel
import com.example.ui.theme.ScannerBlue
import com.example.ui.theme.ScannerCyan
import com.example.ui.theme.ScannerEmerald
import com.example.util.AdBannerView
import com.example.util.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrResultScreen(
    viewModel: DocumentViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentDocWithPages by viewModel.currentDocumentWithPages.collectAsState()

    BackHandler {
        onNavigateBack()
    }

    val doc = currentDocWithPages?.document
    val pages = currentDocWithPages?.pages ?: emptyList()

    val initialText = remember(pages) {
        val sb = StringBuilder()
        pages.forEachIndexed { i, p ->
            sb.append("--- TRANG ${i + 1} ---\n")
            if (!p.ocrText.isNullOrBlank()) {
                sb.append(p.ocrText)
            } else {
                sb.append("(Chưa có văn bản nhận dạng cho trang này)")
            }
            sb.append("\n\n")
        }
        sb.toString().trim()
    }

    var recognizedText by remember(initialText) { mutableStateOf(initialText) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Văn bản trích xuất (OCR)",
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
                    // Copy to clipboard
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("ScanDoc OCR Text", recognizedText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Đã sao chép văn bản vào bộ nhớ tạm!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Sao chép"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            AdBannerView(modifier = Modifier.testTag("ocr_ad_banner"))
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ScannerCyan.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mô hình nhận dạng văn bản tiếng Việt & tiếng Anh trên thiết bị. Bạn có thể chỉnh sửa, sao chép hoặc chia sẻ dưới dạng file TXT.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Editable Text Area
            OutlinedTextField(
                value = recognizedText,
                onValueChange = { recognizedText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("ocr_result_text_field"),
                shape = RoundedCornerShape(12.dp),
                label = { Text("Nội dung văn bản") }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row: Copy & Export TXT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("ScanDoc OCR Text", recognizedText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Đã sao chép vào bộ nhớ tạm!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sao chép")
                }

                Button(
                    onClick = {
                        doc?.let { d ->
                            viewModel.exportDocumentAsTxt(d.id) { file ->
                                if (file != null && file.exists()) {
                                    FileUtils.shareFile(context, file, "text/plain", "ScanDoc_${d.title}.txt")
                                } else {
                                    Toast.makeText(context, "Lỗi tạo file TXT", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1.2f).height(48.dp).testTag("share_txt_file_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScannerEmerald)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chia sẻ TXT", color = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    }
}
