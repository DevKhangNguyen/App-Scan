package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    fun getScansDirectory(context: Context): File {
        val dir = File(context.filesDir, "scans")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getPdfsDirectory(context: Context): File {
        val dir = File(context.filesDir, "pdfs")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun createTempImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(getScansDirectory(context), "IMG_${timeStamp}.jpg")
    }

    fun createPdfFile(context: Context, documentTitle: String): File {
        val cleanTitle = documentTitle.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(getPdfsDirectory(context), "${cleanTitle}.pdf")
    }

    fun createTxtFile(context: Context, documentTitle: String): File {
        val cleanTitle = documentTitle.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(getPdfsDirectory(context), "${cleanTitle}.txt")
    }

    fun shareFile(context: Context, file: File, mimeType: String, subject: String = "Chia sẻ tài liệu ScanDoc") {
        if (!file.exists()) return
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua"))
    }

    fun openPdfWithExternalViewer(context: Context, file: File) {
        if (!file.exists()) return
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Mở file PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
