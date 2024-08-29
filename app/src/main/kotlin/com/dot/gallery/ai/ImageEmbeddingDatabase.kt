/**
 * Copyright 2023 Viacheslav Barkov
 */

package com.dot.gallery.ai

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import java.nio.FloatBuffer

@Entity(tableName = "image_embeddings")
@TypeConverters(Converters::class)
data class ImageEmbedding(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val date: Long,
    val embedding: FloatArray
)

@Dao
interface ImageEmbeddingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addImageEmbedding(imageEmbedding: ImageEmbedding)

    @Query("SELECT * FROM image_embeddings WHERE id = :id LIMIT 1")
    suspend fun getRecord(id: Long): ImageEmbedding
}

@Database(entities = [ImageEmbedding::class], version = 1, exportSchema = false)
abstract class ImageEmbeddingDatabase : RoomDatabase() {
    abstract fun imageEmbeddingDao(): ImageEmbeddingDao

    companion object {
        @Volatile
        private var INSTANCE: ImageEmbeddingDatabase? = null

        fun getDatabase(context: Context): ImageEmbeddingDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) return tempInstance
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ImageEmbeddingDatabase::class.java,
                    "image_embedding_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}

class ImageData(val id: Long, val embedding: FloatArray, val date: Long, val bucket: String)

class ImageEmbeddingRepository(private val imageEmbeddingDao: ImageEmbeddingDao) {
    suspend fun addImageEmbedding(imageEmbedding: ImageEmbedding) {
        imageEmbeddingDao.addImageEmbedding(imageEmbedding)
    }

    suspend fun getRecord(id: Long): ImageEmbedding {
        return imageEmbeddingDao.getRecord(id)
    }
}