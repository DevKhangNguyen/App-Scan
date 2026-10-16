package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DocumentEntity
import com.example.data.model.PageEntity

@Database(
    entities = [DocumentEntity::class, PageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ScanDocDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: ScanDocDatabase? = null

        fun getDatabase(context: Context): ScanDocDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScanDocDatabase::class.java,
                    "scandoc_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
