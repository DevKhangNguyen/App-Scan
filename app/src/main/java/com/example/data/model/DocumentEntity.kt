package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val pageCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null,
    val pdfPath: String? = null,
    val ocrSummary: String? = null,
    val pageSizePreference: String = "A4", // A4, A5, Letter, Original
    val tag: String = "General"
)
