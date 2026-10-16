package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentWithPages
import com.example.data.model.PageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE title LIKE '%' || :query || '%' OR ocrSummary LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :documentId")
    fun getDocumentWithPages(documentId: Long): Flow<DocumentWithPages?>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :documentId")
    suspend fun getDocumentWithPagesSync(documentId: Long): DocumentWithPages?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :documentId")
    suspend fun deleteDocumentById(documentId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<PageEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: PageEntity): Long

    @Update
    suspend fun updatePage(page: PageEntity)

    @Delete
    suspend fun deletePage(page: PageEntity)

    @Query("DELETE FROM pages WHERE id = :pageId")
    suspend fun deletePageById(pageId: Long)

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY pageNumber ASC")
    suspend fun getPagesForDocument(documentId: Long): List<PageEntity>

    @Query("UPDATE documents SET title = :newTitle, updatedAt = :updatedAt WHERE id = :documentId")
    suspend fun renameDocument(documentId: Long, newTitle: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE pages SET ocrText = :ocrText WHERE id = :pageId")
    suspend fun updatePageOcr(pageId: Long, ocrText: String)

    @Query("UPDATE documents SET ocrSummary = :summary WHERE id = :documentId")
    suspend fun updateDocumentOcrSummary(documentId: Long, summary: String)
}
