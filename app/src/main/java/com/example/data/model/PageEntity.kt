package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["documentId"])]
)
data class PageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val pageNumber: Int,
    val originalImagePath: String,
    val croppedImagePath: String,
    val filterType: String = "DOCUMENT", // ORIGINAL, AUTO, DOCUMENT, BW, GRAYSCALE, COLOR
    val rotationAngle: Int = 0,
    val ocrText: String? = null
)
