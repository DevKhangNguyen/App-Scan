package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.util.AdMobConfig
import com.example.util.AdMobManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: DocumentViewModel,
    onNavigateToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isProUnlocked by viewModel.isProUnlocked.collectAsState()

    var showAdMobGuideDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Công cụ & Tiện ích",
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
            AdBannerView(modifier = Modifier.testTag("tools_ad_banner"))
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Rewarded Ad Pro Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isProUnlocked) ScannerEmerald.copy(alpha = 0.12f) else ScannerBlue.copy(alpha = 0.08f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isProUnlocked) Icons.Default.CheckCircle else Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = if (isProUnlocked) ScannerEmerald else Color(0xFFF59E0B),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isProUnlocked) "ScanDoc Pro: Đang Hoạt Động" else "Mở Khoá ScanDoc Pro Miễn Phí",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isProUnlocked)
                                "Bạn đã kích hoạt chế độ Pro: Hỗ trợ xuất file văn bản TXT không giới hạn và xuất PDF độ phân giải cao nhất."
                            else
                                "Xem một video quảng cáo ngắn tự nguyện (AdMob Rewarded) để mở khoá toàn bộ tính năng cao cấp mà không tốn phí.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!isProUnlocked) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (activity != null) {
                                        AdMobManager.showRewardedAd(
                                            activity = activity,
                                            onUserEarnedReward = {
                                                viewModel.unlockProFeature()
                                                Toast.makeText(context, "Đã kích hoạt ScanDoc Pro thành công!", Toast.LENGTH_SHORT).show()
                                            },
                                            onClosed = {}
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("watch_rewarded_ad_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Xem Video Để Mở Khoá Pro")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Tính năng quét nhanh",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            item {
                ToolItemRow(
                    icon = Icons.Default.TextFields,
                    iconColor = ScannerCyan,
                    title = "Nhận dạng văn bản (Offline OCR)",
                    description = "Trích xuất chữ viết tiếng Việt & tiếng Anh không cần kết nối mạng.",
                    onClick = {
                        viewModel.startNewScanSession()
                        onNavigateToCamera()
                    }
                )
            }

            item {
                ToolItemRow(
                    icon = Icons.Default.PictureAsPdf,
                    iconColor = ScannerBlue,
                    title = "Tạo PDF nhiều trang",
                    description = "Ghép nhiều ảnh chụp thành file PDF chuẩn A4, A5, Letter.",
                    onClick = {
                        viewModel.startNewScanSession()
                        onNavigateToCamera()
                    }
                )
            }

            item {
                ToolItemRow(
                    icon = Icons.Default.Security,
                    iconColor = ScannerEmerald,
                    title = "Chính sách bảo mật (Privacy Policy)",
                    description = "100% tài liệu được lưu nội bộ trên máy bạn. Không upload lên cloud.",
                    onClick = { showPrivacyPolicyDialog = true }
                )
            }

            item {
                ToolItemRow(
                    icon = Icons.Default.Info,
                    iconColor = Color(0xFF8B5CF6),
                    title = "Cấu hình Google AdMob & Google Play",
                    description = "Hướng dẫn thay ID thật khi phát hành app kiếm tiền trên Play Store.",
                    onClick = { showAdMobGuideDialog = true }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = ScannerEmerald) },
            title = { Text("Chính sách Quyền Riêng Tư", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "1. Xử lý cục bộ: Mọi hình ảnh, tài liệu scan và văn bản OCR được lưu hoàn toàn trên bộ nhớ thiết bị của bạn.\n\n" +
                            "2. Không có máy chủ: Nhà phát triển không có máy chủ lưu trữ và không bao giờ thu thập, đọc hoặc chia sẻ tài liệu của bạn.\n\n" +
                            "3. Quảng cáo Google AdMob: Ứng dụng tích hợp SDK quảng cáo của Google để duy trì hoạt động miễn phí theo đúng tiêu chuẩn Google Play Store.\n\n" +
                            "4. Hoạt động offline: Bạn có thể tắt hoàn toàn Wi-Fi/4G để scan tài liệu an toàn tuyệt đối.",
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyPolicyDialog = false }) {
                    Text("Đã hiểu")
                }
            }
        )
    }

    if (showAdMobGuideDialog) {
        AlertDialog(
            onDismissRequest = { showAdMobGuideDialog = false },
            icon = { Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = ScannerBlue) },
            title = { Text("Hướng Dẫn Cấu Hình AdMob", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Ứng dụng hiện đang sử dụng Test IDs của Google AdMob:\n\n" +
                            "1. Tạo tài khoản Google AdMob tại: admob.google.com\n" +
                            "2. Tạo App Android và lấy App ID thật (ca-app-pub-xxx~yyy).\n" +
                            "3. Dán App ID vào file `AndroidManifest.xml`:\n" +
                            "   <meta-data android:name=\"com.google.android.gms.ads.APPLICATION_ID\" android:value=\"...\" />\n" +
                            "4. Mở file `AdMobConfig.kt` và thay các Ad Unit ID:\n" +
                            "   - BANNER_AD_UNIT_ID\n" +
                            "   - INTERSTITIAL_AD_UNIT_ID\n" +
                            "   - REWARDED_AD_UNIT_ID\n" +
                            "5. Chuyển `IS_TEST_MODE = false` trước khi xuất bản AAB lên Google Play Store!",
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAdMobGuideDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }
}

@Composable
fun ToolItemRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
