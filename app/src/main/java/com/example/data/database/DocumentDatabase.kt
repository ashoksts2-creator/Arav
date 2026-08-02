package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.DocumentDao
import com.example.data.model.DocumentEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [DocumentEntity::class], version = 1, exportSchema = false)
abstract class DocumentDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: DocumentDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): DocumentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DocumentDatabase::class.java,
                    "document_drive_db"
                )
                .addCallback(DocumentDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DocumentDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.documentDao())
                    }
                }
            }

            suspend fun populateInitialData(dao: DocumentDao) {
                val initialDocs = listOf(
                    DocumentEntity(
                        title = "Aadhaar / National ID Card",
                        category = "Personal IDs",
                        fileType = "PDF",
                        fileSize = 1450000, // 1.4 MB
                        dateAdded = System.currentTimeMillis() - 86400000 * 2,
                        notes = "Government ID verified for official identity proof and address verification.",
                        tags = "id,identity,official,passport",
                        isStarred = true,
                        isLocked = true,
                        ocrText = "GOVERNMENT OF INDIA / GOVT ID\nNAME: Alex Sharma\nDOB: 15/08/1995\nGENDER: MALE\nUNIQUE ID: 4892 1029 3819\nADDRESS: 42 MG Road, Cyber City, Sector 18",
                        pageCount = 2,
                        colorHex = "#1D4ED8"
                    ),
                    DocumentEntity(
                        title = "Health Insurance Policy 2026",
                        category = "Medical",
                        fileType = "PDF",
                        fileSize = 3200000, // 3.2 MB
                        dateAdded = System.currentTimeMillis() - 86400000 * 5,
                        notes = "Comprehensive medical cover policy valid up to March 2027. Cashless hospital network details included.",
                        tags = "health,insurance,medical,claim",
                        isStarred = true,
                        isLocked = false,
                        ocrText = "STAR HEALTH & ALLIED INSURANCE\nPOLICY NO: SH-90281-2026\nSUM INSURED: $10,000 / Rs 5,00,000\nINSURED PERSONS: Alex Sharma, Priya Sharma\nToll-Free Customer Care: 1800-425-2255",
                        pageCount = 4,
                        colorHex = "#059669"
                    ),
                    DocumentEntity(
                        title = "Apartment Lease Agreement",
                        category = "Legal",
                        fileType = "PDF",
                        fileSize = 4800000, // 4.8 MB
                        dateAdded = System.currentTimeMillis() - 86400000 * 12,
                        notes = "Rental agreement for Flat 402, Sunshine Heights. Monthly rent $850 due on 1st of every month.",
                        tags = "lease,rent,legal,flat,housing",
                        isStarred = false,
                        isLocked = false,
                        ocrText = "RENTAL AGREEMENT\nBetween Landlord: Robert Vance & Tenant: Alex Sharma\nProperty Address: Flat 402 Sunshine Heights, Green Park\nMonthly Rent: $850 / Rs 25,000\nSecurity Deposit: $1700 / Rs 50,000",
                        pageCount = 6,
                        colorHex = "#D97706"
                    ),
                    DocumentEntity(
                        title = "Electricity & Utility Bill - July",
                        category = "Bills & Invoices",
                        fileType = "PDF",
                        fileSize = 650000, // 650 KB
                        dateAdded = System.currentTimeMillis() - 86400000 * 1,
                        notes = "Power supply bill for July 2026. Paid via UPI on 28th July.",
                        tags = "utility,electric,bill,invoice,paid",
                        isStarred = false,
                        isLocked = false,
                        ocrText = "STATE ELECTRICITY DISTRIBUTION CORP\nConsumer No: 109283741\nBill Period: 01-Jul-2026 to 31-Jul-2026\nTotal Units Consumed: 342 kWh\nTotal Due Amount: $74.50\nPayment Status: SUCCESSFUL",
                        pageCount = 1,
                        colorHex = "#7C3AED"
                    ),
                    DocumentEntity(
                        title = "B.Tech Computer Science Degree Certificate",
                        category = "Academic",
                        fileType = "PDF",
                        fileSize = 2800000, // 2.8 MB
                        dateAdded = System.currentTimeMillis() - 86400000 * 30,
                        notes = "Official university degree certificate with first class distinction honors.",
                        tags = "degree,certificate,university,education,resume",
                        isStarred = true,
                        isLocked = false,
                        ocrText = "NATIONAL INSTITUTE OF TECHNOLOGY\nBACHELOR OF TECHNOLOGY\nThis is to certify that Alex Sharma has successfully passed the degree in Computer Science & Engineering with First Class Distinction.\nCGPA: 8.92/10",
                        pageCount = 1,
                        colorHex = "#DC2626"
                    ),
                    DocumentEntity(
                        title = "Store Grocery Receipt & Warranty",
                        category = "Receipts",
                        fileType = "JPG",
                        fileSize = 820000, // 820 KB
                        dateAdded = System.currentTimeMillis() - 86400000 * 3,
                        notes = "Electronics store invoice for wireless headphones with 1 year warranty.",
                        tags = "receipt,shopping,warranty,electronics",
                        isStarred = false,
                        isLocked = false,
                        ocrText = "TECHWORLD SUPERSTORE\nReceipt #90812\n1x Noise Cancelling Headphones - $129.99\n1x USB-C Fast Charger - $19.99\nSubtotal: $149.98\nTax: $12.00\nTotal Paid: $161.98\nWarranty Serial: TW-89102-NC",
                        pageCount = 1,
                        colorHex = "#0891B2"
                    )
                )

                initialDocs.forEach { dao.insertDocument(it) }
            }
        }
    }
}
