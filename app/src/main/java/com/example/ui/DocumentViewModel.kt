package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ScanDocDatabase
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentWithPages
import com.example.data.model.PageEntity
import com.example.util.FileUtils
import com.example.util.ImageProcessor
import com.example.util.OcrManager
import com.example.util.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class DraftPage(
    val originalImagePath: String,
    var croppedImagePath: String? = null,
    var corners: List<PointF> = emptyList(),
    var filterMode: ImageProcessor.FilterMode = ImageProcessor.FilterMode.DOCUMENT,
    var rotationAngle: Int = 0,
    var ocrText: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ScanDocDatabase.getDatabase(application)
    private val dao = db.documentDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val documents: StateFlow<List<DocumentEntity>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            dao.getAllDocuments()
        } else {
            dao.searchDocuments(query.trim())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active document detail
    private val _currentDocumentWithPages = MutableStateFlow<DocumentWithPages?>(null)
    val currentDocumentWithPages: StateFlow<DocumentWithPages?> = _currentDocumentWithPages.asStateFlow()

    // Active scan session
    private val _draftPages = MutableStateFlow<List<DraftPage>>(emptyList())
    val draftPages: StateFlow<List<DraftPage>> = _draftPages.asStateFlow()

    private val _activeDraftIndex = MutableStateFlow(0)
    val activeDraftIndex: StateFlow<Int> = _activeDraftIndex.asStateFlow()

    // Processing indicator
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processingMessage = MutableStateFlow("")
    val processingMessage: StateFlow<String> = _processingMessage.asStateFlow()

    // Reward / Pro feature status
    private val _isProUnlocked = MutableStateFlow(false)
    val isProUnlocked: StateFlow<Boolean> = _isProUnlocked.asStateFlow()

    fun unlockProFeature() {
        _isProUnlocked.value = true
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadDocument(documentId: Long) {
        viewModelScope.launch {
            dao.getDocumentWithPages(documentId).collect {
                _currentDocumentWithPages.value = it
            }
        }
    }

    fun startNewScanSession() {
        _draftPages.value = emptyList()
        _activeDraftIndex.value = 0
    }

    fun addCapturedImage(imagePath: String, onCornersDetected: (List<PointF>) -> Unit) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Đang nhận diện tài liệu..."

            val bmp = ImageProcessor.loadBitmap(getApplication(), imagePath, 1200, 1200)
            val detectedCorners = if (bmp != null) {
                val corners = ImageProcessor.detectCorners(bmp)
                bmp.recycle()
                corners
            } else {
                listOf(
                    PointF(50f, 50f),
                    PointF(500f, 50f),
                    PointF(500f, 700f),
                    PointF(50f, 700f)
                )
            }

            val newDraft = DraftPage(
                originalImagePath = imagePath,
                corners = detectedCorners
            )

            val updated = _draftPages.value.toMutableList().apply { add(newDraft) }
            _draftPages.value = updated
            _activeDraftIndex.value = updated.size - 1
            _isProcessing.value = false

            onCornersDetected(detectedCorners)
        }
    }

    fun updateDraftCorners(index: Int, corners: List<PointF>) {
        val current = _draftPages.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(corners = corners)
            _draftPages.value = current
        }
    }

    fun updateDraftFilter(index: Int, filter: ImageProcessor.FilterMode) {
        val current = _draftPages.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(filterMode = filter)
            _draftPages.value = current
        }
    }

    fun rotateDraft(index: Int) {
        val current = _draftPages.value.toMutableList()
        if (index in current.indices) {
            val newAngle = (current[index].rotationAngle + 90) % 360
            current[index] = current[index].copy(rotationAngle = newAngle)
            _draftPages.value = current
        }
    }

    fun setActiveDraftIndex(index: Int) {
        if (index in _draftPages.value.indices) {
            _activeDraftIndex.value = index
        }
    }

    fun removeDraftPage(index: Int) {
        val current = _draftPages.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _draftPages.value = current
            if (_activeDraftIndex.value >= current.size && current.isNotEmpty()) {
                _activeDraftIndex.value = current.size - 1
            }
        }
    }

    /**
     * Finalizes current scan session, warps and filters all pages, generates initial PDF,
     * performs quick OCR summary, and saves to Room database.
     */
    fun saveScanSession(
        title: String,
        pageSize: PdfGenerator.PageSize = PdfGenerator.PageSize.A4,
        quality: PdfGenerator.PdfQuality = PdfGenerator.PdfQuality.HIGH,
        onSuccess: (Long) -> Unit
    ) {
        val drafts = _draftPages.value
        if (drafts.isEmpty()) return

        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Đang xử lý & lưu tài liệu..."

            val processedPageEntities = mutableListOf<PageEntity>()
            val finalImagePaths = mutableListOf<String>()
            val ocrSummaryBuilder = StringBuilder()

            drafts.forEachIndexed { idx, draft ->
                _processingMessage.value = "Đang tối ưu trang ${idx + 1}/${drafts.size}..."

                val originalBmp = ImageProcessor.loadBitmap(getApplication(), draft.originalImagePath, 2500, 2500)
                if (originalBmp != null) {
                    // 1. Perspective crop
                    val warpedBmp = if (draft.corners.size == 4) {
                        ImageProcessor.cropAndWarpPerspective(originalBmp, draft.corners)
                    } else {
                        originalBmp
                    }

                    // 2. Rotate if needed
                    val rotatedBmp = if (draft.rotationAngle != 0) {
                        ImageProcessor.rotateBitmap(warpedBmp, draft.rotationAngle.toFloat())
                    } else {
                        warpedBmp
                    }

                    // 3. Apply filter
                    val filteredBmp = ImageProcessor.applyFilter(rotatedBmp, draft.filterMode)

                    // 4. Save to filesDir
                    val croppedFile = FileUtils.createTempImageFile(getApplication())
                    ImageProcessor.saveBitmapToFile(filteredBmp, croppedFile)
                    finalImagePaths.add(croppedFile.absolutePath)

                    // 5. Run on-device OCR
                    val ocrResult = OcrManager.recognizeText(filteredBmp).getOrDefault("")
                    if (ocrResult.isNotBlank()) {
                        ocrSummaryBuilder.append(ocrResult.take(150)).append(" ")
                    }

                    processedPageEntities.add(
                        PageEntity(
                            documentId = 0, // Will be set after doc insertion
                            pageNumber = idx + 1,
                            originalImagePath = draft.originalImagePath,
                            croppedImagePath = croppedFile.absolutePath,
                            filterType = draft.filterMode.name,
                            rotationAngle = draft.rotationAngle,
                            ocrText = ocrResult
                        )
                    )

                    // Recycle bitmaps
                    if (originalBmp != warpedBmp) originalBmp.recycle()
                    if (warpedBmp != rotatedBmp) warpedBmp.recycle()
                    if (rotatedBmp != filteredBmp) rotatedBmp.recycle()
                    filteredBmp.recycle()
                }
            }

            if (finalImagePaths.isEmpty()) {
                _isProcessing.value = false
                return@launch
            }

            // Generate PDF
            _processingMessage.value = "Đang tạo file PDF..."
            val pdfFile = FileUtils.createPdfFile(getApplication(), title)
            PdfGenerator.generatePdf(
                context = getApplication(),
                imagePaths = finalImagePaths,
                outputFile = pdfFile,
                pageSize = pageSize,
                quality = quality
            )

            // Insert into Room
            val docEntity = DocumentEntity(
                title = title.ifBlank { "ScanDoc_${SimpleDateFormat("ddMMyy_HHmm", Locale.getDefault()).format(Date())}" },
                pageCount = finalImagePaths.size,
                thumbnailPath = finalImagePaths.firstOrNull(),
                pdfPath = pdfFile.absolutePath,
                ocrSummary = ocrSummaryBuilder.toString().trim().take(300),
                pageSizePreference = pageSize.name
            )

            val docId = dao.insertDocument(docEntity)

            val pagesWithDocId = processedPageEntities.map { it.copy(documentId = docId) }
            dao.insertPages(pagesWithDocId)

            _isProcessing.value = false
            _draftPages.value = emptyList()
            onSuccess(docId)
        }
    }

    fun deleteDocument(documentId: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val docWithPages = dao.getDocumentWithPagesSync(documentId)
            docWithPages?.pages?.forEach { page ->
                try {
                    File(page.croppedImagePath).delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            docWithPages?.document?.pdfPath?.let {
                try { File(it).delete() } catch (e: Exception) {}
            }
            dao.deleteDocumentById(documentId)
            onDone()
        }
    }

    fun renameDocument(documentId: Long, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            dao.renameDocument(documentId, newTitle.trim())
            loadDocument(documentId)
        }
    }

    fun runOcrForPage(pageId: Long, imagePath: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Đang nhận dạng văn bản..."
            val result = OcrManager.recognizeTextFromFile(imagePath).getOrDefault("")
            dao.updatePageOcr(pageId, result)
            _isProcessing.value = false
            onResult(result)
        }
    }

    fun exportPdfToUri(pdfPath: String, targetUri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val file = File(pdfPath)
            if (!file.exists()) {
                onResult(false)
                return@launch
            }
            val success = PdfGenerator.writePdfToUri(getApplication(), file, targetUri)
            onResult(success)
        }
    }

    fun exportDocumentAsTxt(documentId: Long, onResult: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val doc = dao.getDocumentWithPagesSync(documentId) ?: run {
                withContext(Dispatchers.Main) { onResult(null) }
                return@launch
            }

            val txtFile = FileUtils.createTxtFile(getApplication(), doc.document.title)
            val fullText = StringBuilder()
            fullText.append("=== TÀI LIỆU SCANDOC: ${doc.document.title} ===\n\n")

            doc.pages.forEach { page ->
                fullText.append("--- TRANG ${page.pageNumber} ---\n")
                if (!page.ocrText.isNullOrBlank()) {
                    fullText.append(page.ocrText)
                } else {
                    val extracted = OcrManager.recognizeTextFromFile(page.croppedImagePath).getOrDefault("")
                    fullText.append(extracted.ifBlank { "(Không có văn bản nhận dạng được)" })
                }
                fullText.append("\n\n")
            }

            txtFile.writeText(fullText.toString())
            withContext(Dispatchers.Main) {
                onResult(txtFile)
            }
        }
    }
}
