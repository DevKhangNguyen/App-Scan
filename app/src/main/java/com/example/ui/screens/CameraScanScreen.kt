package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.DocumentViewModel
import com.example.ui.theme.ScannerBlue
import com.example.ui.theme.ScannerCyan
import com.example.ui.theme.ScannerEmerald
import com.example.util.FileUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraScanScreen(
    viewModel: DocumentViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCrop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val draftPages by viewModel.draftPages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Ứng dụng cần quyền camera để chụp tài liệu.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    BackHandler {
        onNavigateBack()
    }

    // Photo picker fallback / gallery import
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val tempFile = File(context.cacheDir, "GALLERY_${timeStamp}.jpg")
                context.contentResolver.openInputStream(selectedUri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.addCapturedImage(tempFile.absolutePath) {
                    onNavigateToCrop()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Quyền truy cập Camera bị từ chối",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Vui lòng cấp quyền để ScanDoc chụp ảnh quét tài liệu, hoặc bạn có thể chọn ảnh từ thư viện.",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = ScannerBlue)
                ) {
                    Text("Cấp quyền Camera")
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Text("Chọn ảnh từ máy", color = Color.White)
                }
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
        // CameraX Viewfinder
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val provider = cameraProviderFuture.get()
                    cameraProvider = provider

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setFlashMode(flashMode)
                        .build()
                    imageCapture = capture

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        provider.unbindAll()
                        camera = provider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture
                        )
                    } catch (exc: Exception) {
                        exc.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Live Document Frame Guidelines Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val marginH = size.width * 0.08f
            val marginV = size.height * 0.18f
            val rectW = size.width - (marginH * 2)
            val rectH = size.height - (marginV * 2)

            // Outer dark mask
            drawRoundRect(
                color = Color.Cyan.copy(alpha = 0.8f),
                topLeft = Offset(marginH, marginV),
                size = Size(rectW, rectH),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )

            // 4 Corner brackets for scanner feel
            val bracketLen = 32.dp.toPx()
            val bracketStroke = 4.dp.toPx()

            // Top-left
            drawLine(Color.White, Offset(marginH, marginV), Offset(marginH + bracketLen, marginV), bracketStroke)
            drawLine(Color.White, Offset(marginH, marginV), Offset(marginH, marginV + bracketLen), bracketStroke)

            // Top-right
            val rX = marginH + rectW
            drawLine(Color.White, Offset(rX, marginV), Offset(rX - bracketLen, marginV), bracketStroke)
            drawLine(Color.White, Offset(rX, marginV), Offset(rX, marginV + bracketLen), bracketStroke)

            // Bottom-left
            val bY = marginV + rectH
            drawLine(Color.White, Offset(marginH, bY), Offset(marginH + bracketLen, bY), bracketStroke)
            drawLine(Color.White, Offset(marginH, bY), Offset(marginH, bY - bracketLen), bracketStroke)

            // Bottom-right
            drawLine(Color.White, Offset(rX, bY), Offset(rX - bracketLen, bY), bracketStroke)
            drawLine(Color.White, Offset(rX, bY), Offset(rX, bY - bracketLen), bracketStroke)
        }

        // Top Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = Color.White
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "Đặt tài liệu vào khung",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            // Flash Mode Toggle
            IconButton(
                onClick = {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                        ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                        else -> ImageCapture.FLASH_MODE_AUTO
                    }
                    imageCapture?.flashMode = flashMode
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                        else -> Icons.Default.FlashOff
                    },
                    contentDescription = "Flash",
                    tint = if (flashMode != ImageCapture.FLASH_MODE_OFF) Color(0xFFFBBF24) else Color.White
                )
            }
        }

        // Bottom Capture Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Photo Gallery Shortcut
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Chọn từ thư viện",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Shutter Capture Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                        .testTag("camera_shutter_button")
                        .clickable(enabled = !isProcessing) {
                            val capture = imageCapture ?: return@clickable
                            val photoFile = FileUtils.createTempImageFile(context)
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                            capture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        viewModel.addCapturedImage(photoFile.absolutePath) {
                                            onNavigateToCrop()
                                        }
                                    }

                                    override fun onError(exc: ImageCaptureException) {
                                        exc.printStackTrace()
                                        Toast.makeText(context, "Lỗi chụp ảnh: ${exc.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                // Page count & Done button
                if (draftPages.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = ScannerEmerald,
                        modifier = Modifier
                            .clickable { onNavigateToCrop() }
                            .testTag("finish_capture_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "${draftPages.size} trang",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Hoàn tất",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(52.dp))
                }
            }
        }

        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ScannerCyan)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Đang xử lý hình ảnh...",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
}


