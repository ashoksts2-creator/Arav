package com.example.data.repository

import com.example.data.dao.DocumentDao
import com.example.data.model.DocumentEntity
import kotlinx.coroutines.flow.Flow

class DocumentRepository(private val documentDao: DocumentDao) {

    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()

    val starredDocuments: Flow<List<DocumentEntity>> = documentDao.getStarredDocuments()

    fun getDocumentsByCategory(category: String): Flow<List<DocumentEntity>> {
        return documentDao.getDocumentsByCategory(category)
    }

    fun searchDocuments(query: String): Flow<List<DocumentEntity>> {
        return documentDao.searchDocuments(query)
    }

    fun getDocumentById(id: Int): Flow<DocumentEntity?> {
        return documentDao.getDocumentById(id)
    }

    suspend fun insertDocument(document: DocumentEntity): Long {
        return documentDao.insertDocument(document)
    }

    suspend fun updateDocument(document: DocumentEntity) {
        documentDao.updateDocument(document)
    }

    suspend fun deleteDocument(document: DocumentEntity) {
        documentDao.deleteDocument(document)
    }

    suspend fun deleteDocumentById(id: Int) {
        documentDao.deleteDocumentById(id)
    }

    suspend fun toggleStar(id: Int, isStarred: Boolean) {
        documentDao.updateStarredStatus(id, isStarred)
    }

    suspend fun toggleLock(id: Int, isLocked: Boolean) {
        documentDao.updateLockedStatus(id, isLocked)
    }
}
