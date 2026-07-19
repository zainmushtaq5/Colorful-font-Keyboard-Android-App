package com.example.colortextstudio.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "creations")
data class CreationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imageUri: String,
    val createdAt: Long
)

@Dao
interface CreationDao {
    @Query("SELECT * FROM creations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CreationEntity>>

    @Insert
    suspend fun insert(entity: CreationEntity): Long

    @Delete
    suspend fun delete(entity: CreationEntity)
}

@Database(entities = [CreationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun creationDao(): CreationDao
}
