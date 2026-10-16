package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DocumentViewModel
import com.example.ui.theme.ScannerBlue
import com.example.ui.theme.ScannerCyan
import com.example.util.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.hypot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropAdjustScreen(
    viewModel: DocumentViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val draftPages by viewModel.draftPages.collectAsState()
    val activeIndex by viewModel.activeDraftIndex.collectAsState()

    BackHandler {
        onNavigateBack()
    }

    if (draftPages.isEmpty() || activeIndex !in draftPages.indices) {
        onNavigateBack()
        return
    }

    val currentDraft = draftPages[activeIndex]
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Screen display corners (normalized 0..1 to view coordinates)
    val cornerPoints = remember { mutableStateListOf<Offset>() }
    var draggingIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(currentDraft.originalImagePath, currentDraft.rotationAngle) {
        withContext(Dispatchers.IO) {
            val loaded = ImageProcessor.loadBitmap(context, currentDraft.originalImagePath, 1600, 1600)
            val rotated = if (loaded != null && currentDraft.rotationAngle != 0) {
                ImageProcessor.rotateBitmap(loaded, currentDraft.rotationAngle.toFloat())
            } else {
                loaded
            }
            bitmap = rotated
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "Chỉnh sửa 4 góc (${activeIndex + 1}/${draftPages.size})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = Color.White
                    )
                }
            },
            actions = {
                // Rotate 90 degrees
                IconButton(
                    onClick = {
                        viewModel.rotateDraft(activeIndex)
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.RotateRight,
                        contentDescription = "Xoay",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
        )

        // Interactive Canvas Container
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            val containerW = maxWidth.value
            val containerH = maxHeight.value

            val currentBitmap = bitmap
            if (currentBitmap != null) {
                // Compute aspect ratio scaling
                val bmpW = currentBitmap.width.toFloat()
                val bmpH = currentBitmap.height.toFloat()

                val viewW = maxWidth.value
                val viewH = maxHeight.value

                val scale = minOf(viewW / bmpW, viewH / bmpH)
                val renderW = bmpW * scale
                val renderH = bmpH * scale
                val offsetX = (viewW - renderW) / 2f
                val offsetY = (viewH - renderH) / 2f

                // Initialize corner handles in view coordinates if empty
                LaunchedEffect(renderW, renderH, currentDraft.corners) {
                    cornerPoints.clear()
                    if (currentDraft.corners.size == 4) {
                        currentDraft.corners.forEach { pt ->
                            val screenX = pt.x * (renderW / bmpW) + offsetX
                            val screenY = pt.y * (renderH / bmpH) + offsetY
                            cornerPoints.add(Offset(screenX, screenY))
                        }
                    } else {
                        // Default 8% inset
                        val padX = renderW * 0.08f
                        val padY = renderH * 0.08f
                        cornerPoints.add(Offset(offsetX + padX, offsetY + padY))
                        cornerPoints.add(Offset(offsetX + renderW - padX, offsetY + padY))
                        cornerPoints.add(Offset(offsetX + renderW - padX, offsetY + renderH - padY))
                        cornerPoints.add(Offset(offsetX + padX, offsetY + renderH - padY))
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(renderW, renderH) {
                            detectDragGestures(
                                onDragStart = { startPos ->
                                    // Find closest corner handle within touch radius
                                    val touchThreshold = 48.dp.toPx()
                                    var closestIdx = -1
                                    var minDist = Float.MAX_VALUE
                                    cornerPoints.forEachIndexed { i, pt ->
                                        val d = hypot((pt.x - startPos.x).toDouble(), (pt.y - startPos.y).toDouble()).toFloat()
                                        if (d < touchThreshold && d < minDist) {
                                            minDist = d
                                            closestIdx = i
                                        }
                                    }
                                    draggingIndex = closestIdx
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (draggingIndex in cornerPoints.indices) {
                                        val currentPt = cornerPoints[draggingIndex]
                                        val newX = (currentPt.x + dragAmount.x).coerceIn(offsetX, offsetX + renderW)
                                        val newY = (currentPt.y + dragAmount.y).coerceIn(offsetY, offsetY + renderH)
                                        cornerPoints[draggingIndex] = Offset(newX, newY)
                                    }
                                },
                                onDragEnd = {
                                    draggingIndex = -1
                                    // Save back to draft
                                    val mappedBack = cornerPoints.map { screenPt ->
                                        val bmpX = (screenPt.x - offsetX) * (bmpW / renderW)
                                        val bmpY = (screenPt.y - offsetY) * (bmpH / renderH)
                                        PointF(bmpX, bmpY)
                                    }
                                    viewModel.updateDraftCorners(activeIndex, mappedBack)
                                },
                                onDragCancel = {
                                    draggingIndex = -1
                                }
                            )
                        }
                ) {
                    // 1. Draw scaled bitmap
                    drawImage(
                        image = currentBitmap.asImageBitmap(),
                        dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
                        dstSize = IntSize(renderW.toInt(), renderH.toInt())
                    )

                    // 2. Draw quadrilateral boundary
                    if (cornerPoints.size == 4) {
                        val path = Path().apply {
                            moveTo(cornerPoints[0].x, cornerPoints[0].y)
                            lineTo(cornerPoints[1].x, cornerPoints[1].y)
                            lineTo(cornerPoints[2].x, cornerPoints[2].y)
                            lineTo(cornerPoints[3].x, cornerPoints[3].y)
                            close()
                        }
                        // Semi-transparent blue/cyan fill
                        drawPath(path, color = Color(0x3306B6D4))
                        // Neon outline
                        drawPath(path, color = Color(0xFF06B6D4), style = Stroke(width = 3.dp.toPx()))

                        // 3. Draw 4 corner handles
                        val handleRadius = 14.dp.toPx()
                        cornerPoints.forEachIndexed { idx, pt ->
                            val isBeingDragged = idx == draggingIndex
                            drawCircle(
                                color = if (isBeingDragged) Color.Yellow else Color.White,
                                radius = if (isBeingDragged) handleRadius * 1.3f else handleRadius,
                                center = pt
                            )
                            drawCircle(
                                color = ScannerBlue,
                                radius = (if (isBeingDragged) handleRadius * 1.3f else handleRadius) * 0.65f,
                                center = pt
                            )
                        }
                    }
                }

                // Loupe indicator when dragging
                if (draggingIndex != -1 && cornerPoints.size == 4) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = "Kéo để căn chỉnh chuẩn viền giấy",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Bottom Action Bar
        Surface(
            color = Color(0xFF1E293B),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Auto Detect Corners
                    OutlinedButton(
                        onClick = {
                            val curBmp = bitmap ?: return@OutlinedButton
                            val bmpW = curBmp.width.toFloat()
                            val bmpH = curBmp.height.toFloat()
                            // Re-run auto detect
                            viewModel.addCapturedImage(currentDraft.originalImagePath) { newCorners ->
                                viewModel.updateDraftCorners(activeIndex, newCorners)
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(imageVector = Icons.Default.CropFree, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tự động")
                    }

                    // Reset to Full Image
                    OutlinedButton(
                        onClick = {
                            val curBmp = bitmap ?: return@OutlinedButton
                            val fullCorners = listOf(
                                PointF(0f, 0f),
                                PointF(curBmp.width.toFloat(), 0f),
                                PointF(curBmp.width.toFloat(), curBmp.height.toFloat()),
                                PointF(0f, curBmp.height.toFloat())
                            )
                            viewModel.updateDraftCorners(activeIndex, fullCorners)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(imageVector = Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Toàn ảnh")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Continue to Filter Button
                Button(
                    onClick = onNavigateToFilter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("proceed_to_filter_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScannerBlue)
                ) {
                    Text("Tiếp tục bộ lọc", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}
