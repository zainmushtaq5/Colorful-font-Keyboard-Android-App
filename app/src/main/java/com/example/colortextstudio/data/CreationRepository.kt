package com.example.colortextstudio.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreationRepository @Inject constructor(
    private val dao: CreationDao
) {
    fun observeAll(): Flow<List<CreationEntity>> = dao.observeAll()

    suspend fun add(imageUri: String) {
        dao.insert(CreationEntity(imageUri = imageUri, createdAt = System.currentTimeMillis()))
    }

    suspend fun remove(entity: CreationEntity) = dao.delete(entity)
}
