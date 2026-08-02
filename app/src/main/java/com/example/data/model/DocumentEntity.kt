package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val category: String, // e.g., "Personal IDs", "Receipts", "Bills & Invoices", "Legal", "Medical", "Academic", "Other"
    val fileType: String, // e.g., "PDF", "JPG", "PNG", "DOCX", "TXT"
    val fileSize: Long, // in bytes
    val dateAdded: Long = System.currentTimeMillis(),
    val fileUri: String = "",
    val notes: String = "",
    val tags: String = "", // Comma-separated, e.g., "tax,urgent,2026"
    val isStarred: Boolean = false,
    val isLocked: Boolean = false,
    val ocrText: String = "",
    val pageCount: Int = 1,
    val colorHex: String = "#2563EB"
) {
    fun getFormattedSize(): String {
        return when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
            else -> String.format("%.1f MB", fileSize / (1024.0 * 1024.0))
        }
    }

    fun getTagList(): List<String> {
        if (tags.isBlank()) return emptyList()
        return tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
