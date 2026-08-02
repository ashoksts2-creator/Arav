package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.DocumentDatabase
import com.example.data.model.DocumentEntity
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ViewMode {
    GRID, LIST
}

data class StorageStats(
    val totalCount: Int = 0,
    val totalSizeBytes: Long = 0,
    val categoryBreakdown: Map<String, Long> = emptyMap(),
    val pdfCount: Int = 0,
    val imageCount: Int = 0,
    val starredCount: Int = 0,
    val lockedCount: Int = 0
) {
    fun getFormattedTotalSize(): String {
        return when {
            totalSizeBytes < 1024 -> "$totalSizeBytes B"
            totalSizeBytes < 1024 * 1024 -> "${totalSizeBytes / 1024} KB"
            else -> String.format("%.1f MB", totalSizeBytes / (1024.0 * 1024.0))
        }
    }
}

class DocumentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DocumentRepository

    val categories = listOf(
        "All", "Starred", "Personal IDs", "Receipts", "Bills & Invoices", "Legal", "Medical", "Academic", "Other"
    )

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.GRID)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _selectedDocument = MutableStateFlow<DocumentEntity?>(null)
    val selectedDocument: StateFlow<DocumentEntity?> = _selectedDocument.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _showScanner = MutableStateFlow(false)
    val showScanner: StateFlow<Boolean> = _showScanner.asStateFlow()

    private val _showStatsSheet = MutableStateFlow(false)
    val showStatsSheet: StateFlow<Boolean> = _showStatsSheet.asStateFlow()

    private val _unlockedDocumentId = MutableStateFlow<Int?>(null)
    val unlockedDocumentId: StateFlow<Int?> = _unlockedDocumentId.asStateFlow()

    private val _pinPromptForDoc = MutableStateFlow<DocumentEntity?>(null)
    val pinPromptForDoc: StateFlow<DocumentEntity?> = _pinPromptForDoc.asStateFlow()

    private val _userPin = MutableStateFlow("1234") // Default security pin
    val userPin: StateFlow<String> = _userPin.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    private val _userNotification = MutableStateFlow<String?>(null)
    val userNotification: StateFlow<String?> = _userNotification.asStateFlow()

    init {
        val database = DocumentDatabase.getDatabase(application, viewModelScope)
        repository = DocumentRepository(database.documentDao())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val documents: StateFlow<List<DocumentEntity>> = combine(
        _selectedCategory,
        _searchQuery
    ) { category, query ->
        Pair(category, query)
    }.flatMapLatest { (category, query) ->
        when {
            query.isNotBlank() -> repository.searchDocuments(query)
            category == "Starred" -> repository.starredDocuments
            category == "All" -> repository.allDocuments
            else -> repository.getDocumentsByCategory(category)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val storageStats: StateFlow<StorageStats> = repository.allDocuments.combine(MutableStateFlow(Unit)) { docs, _ ->
        val totalSize = docs.sumOf { it.fileSize }
        val categoryMap = docs.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.fileSize } }
        val pdfs = docs.count { it.fileType.equals("PDF", ignoreCase = true) }
        val images = docs.count { it.fileType.equals("JPG", true) || it.fileType.equals("PNG", true) }
        val starred = docs.count { it.isStarred }
        val locked = docs.count { it.isLocked }

        StorageStats(
            totalCount = docs.size,
            totalSizeBytes = totalSize,
            categoryBreakdown = categoryMap,
            pdfCount = pdfs,
            imageCount = images,
            starredCount = starred,
            lockedCount = locked
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StorageStats()
    )

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
    }

    fun openDocument(document: DocumentEntity) {
        if (document.isLocked && _unlockedDocumentId.value != document.id) {
            _pinPromptForDoc.value = document
            _pinError.value = null
        } else {
            _selectedDocument.value = document
        }
    }

    fun closeDocument() {
        _selectedDocument.value = null
    }

    fun verifyPin(enteredPin: String) {
        if (enteredPin == _userPin.value) {
            val doc = _pinPromptForDoc.value
            if (doc != null) {
                _unlockedDocumentId.value = doc.id
                _selectedDocument.value = doc
                _pinPromptForDoc.value = null
                _pinError.value = null
            }
        } else {
            _pinError.value = "Incorrect Security PIN. Default PIN is 1234."
        }
    }

    fun dismissPinPrompt() {
        _pinPromptForDoc.value = null
        _pinError.value = null
    }

    fun showAddDocumentDialog(show: Boolean) {
        _showAddDialog.value = show
    }

    fun showScannerScreen(show: Boolean) {
        _showScanner.value = show
    }

    fun showStatsSheet(show: Boolean) {
        _showStatsSheet.value = show
    }

    fun toggleStar(document: DocumentEntity) {
        viewModelScope.launch {
            repository.toggleStar(document.id, !document.isStarred)
            // Update selected document if open
            if (_selectedDocument.value?.id == document.id) {
                _selectedDocument.value = _selectedDocument.value?.copy(isStarred = !document.isStarred)
            }
            showNotification(if (!document.isStarred) "Starred ${document.title}" else "Unstarred ${document.title}")
        }
    }

    fun toggleLock(document: DocumentEntity) {
        viewModelScope.launch {
            val newLockState = !document.isLocked
            repository.toggleLock(document.id, newLockState)
            if (_selectedDocument.value?.id == document.id) {
                _selectedDocument.value = _selectedDocument.value?.copy(isLocked = newLockState)
            }
            showNotification(if (newLockState) "Document locked with PIN" else "Document lock removed")
        }
    }

    fun addDocument(
        title: String,
        category: String,
        fileType: String,
        fileSize: Long,
        notes: String,
        tags: String,
        isStarred: Boolean,
        isLocked: Boolean,
        ocrText: String = "",
        pageCount: Int = 1,
        colorHex: String = "#2563EB"
    ) {
        viewModelScope.launch {
            val newDoc = DocumentEntity(
                title = title.ifBlank { "Untitled Document" },
                category = category,
                fileType = fileType,
                fileSize = fileSize,
                dateAdded = System.currentTimeMillis(),
                notes = notes,
                tags = tags,
                isStarred = isStarred,
                isLocked = isLocked,
                ocrText = ocrText,
                pageCount = pageCount,
                colorHex = colorHex
            )
            repository.insertDocument(newDoc)
            _showAddDialog.value = false
            showNotification("Added '${newDoc.title}' to Document Drive!")
        }
    }

    fun updateDocumentNotesAndTags(documentId: Int, notes: String, tags: String) {
        viewModelScope.launch {
            val currentDoc = _selectedDocument.value
            if (currentDoc != null && currentDoc.id == documentId) {
                val updated = currentDoc.copy(notes = notes, tags = tags)
                repository.updateDocument(updated)
                _selectedDocument.value = updated
                showNotification("Document details updated")
            }
        }
    }

    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(document)
            if (_selectedDocument.value?.id == document.id) {
                _selectedDocument.value = null
            }
            showNotification("Deleted '${document.title}'")
        }
    }

    fun showNotification(msg: String) {
        _userNotification.value = msg
    }

    fun dismissNotification() {
        _userNotification.value = null
    }

    fun changeUserPin(oldPin: String, newPin: String): Boolean {
        return if (oldPin == _userPin.value && newPin.length == 4) {
            _userPin.value = newPin
            showNotification("Security PIN updated successfully")
            true
        } else {
            false
        }
    }
}
