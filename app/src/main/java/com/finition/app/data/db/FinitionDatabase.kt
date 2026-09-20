package com.finition.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.finition.app.data.model.InvoiceEntity
import com.finition.app.data.model.InvoiceLineItemEntity
import com.finition.app.data.model.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        InvoiceEntity::class,
        InvoiceLineItemEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class FinitionDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun invoiceDao(): InvoiceDao

    companion object {
        @Volatile
        private var instance: FinitionDatabase? = null

        fun get(context: Context): FinitionDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinitionDatabase::class.java,
                    "finition.db",
                ).build().also { instance = it }
            }
    }
}
