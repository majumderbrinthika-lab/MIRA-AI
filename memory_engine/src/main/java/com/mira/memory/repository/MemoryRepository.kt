package com.mira.memory.repository

import com.mira.memory.dao.CharacterDao
import com.mira.memory.dao.ChatMessageDao
import com.mira.memory.dao.LocalModelDao
import com.mira.memory.dao.MemoryFactDao
import com.mira.memory.dao.StudioImageDao
import com.mira.memory.db.MiraDatabase
import com.mira.memory.model.CharacterProfileEntity
import com.mira.memory.model.ChatMessageEntity
import com.mira.memory.model.LocalModelEntity
import com.mira.memory.model.MemoryFactEntity
import com.mira.memory.model.StudioImageEntity
import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val db: MiraDatabase) {
    private val characterDao: CharacterDao = db.characterDao()
    private val chatMessageDao: ChatMessageDao = db.chatMessageDao()
    private val memoryFactDao: MemoryFactDao = db.memoryFactDao()
    private val studioImageDao: StudioImageDao = db.studioImageDao()
    private val localModelDao: LocalModelDao = db.localModelDao()

    fun observeAllCharacters(): Flow<List<CharacterProfileEntity>> =
        characterDao.observeAllCharacters()

    fun observeCharacter(id: String): Flow<CharacterProfileEntity?> =
        characterDao.observeCharacter(id)

    suspend fun getCharacterOnce(id: String): CharacterProfileEntity? =
        characterDao.getCharacterOnce(id)

    suspend fun countCharacters(): Int = characterDao.countCharacters()

    suspend fun upsertCharacter(character: CharacterProfileEntity) =
        characterDao.upsertCharacter(character)

    suspend fun updateCharacter(character: CharacterProfileEntity) =
        characterDao.updateCharacter(character)

    suspend fun deleteNonPermanentCharacter(id: String) {
        characterDao.deleteNonPermanentCharacter(id)
        if (id != "mira_permanent") {
            chatMessageDao.clearConversation(id)
        }
    }

    fun observeMessages(characterId: String): Flow<List<ChatMessageEntity>> =
        chatMessageDao.observeMessagesForCharacter(characterId)

    fun observeTotalMessageCount(): Flow<Int> = chatMessageDao.observeTotalMessageCount()

    suspend fun getRecentMessagesOnce(characterId: String, limit: Int = 16): List<ChatMessageEntity> =
        chatMessageDao.getRecentMessagesOnce(characterId, limit).reversed()

    suspend fun addMessage(message: ChatMessageEntity): Long =
        chatMessageDao.insertMessage(message)

    suspend fun clearConversation(characterId: String) =
        chatMessageDao.clearConversation(characterId)

    suspend fun clearAllConversations() = chatMessageDao.clearAllConversations()

    fun observeAllMemories(): Flow<List<MemoryFactEntity>> = memoryFactDao.observeAllMemories()

    fun observeMemoriesForCharacter(characterId: String): Flow<List<MemoryFactEntity>> =
        memoryFactDao.observeMemoriesForCharacter(characterId)

    suspend fun getMemoriesOnce(characterId: String): List<MemoryFactEntity> =
        memoryFactDao.getMemoriesOnce(characterId)

    suspend fun upsertMemory(memory: MemoryFactEntity): Long {
        val existing = memoryFactDao.findByKey(memory.characterId, memory.factKey)
        return if (existing != null) {
            val updated = existing.copy(
                factValue = memory.factValue,
                category = memory.category,
                importance = maxOf(existing.importance, memory.importance),
                updatedAt = System.currentTimeMillis()
            )
            memoryFactDao.updateMemory(updated)
            existing.id
        } else {
            memoryFactDao.upsertMemory(memory)
        }
    }

    suspend fun updateMemory(memory: MemoryFactEntity) =
        memoryFactDao.updateMemory(memory.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteMemoryById(id: Long) = memoryFactDao.deleteMemoryById(id)

    suspend fun deleteAllMemories() = memoryFactDao.deleteAllMemories()

    fun observeStudioImages(): Flow<List<StudioImageEntity>> = studioImageDao.observeAllImages()

    suspend fun saveStudioImage(image: StudioImageEntity): Long =
        studioImageDao.insertImage(image)

    suspend fun deleteStudioImage(id: Long) = studioImageDao.deleteImageById(id)

    fun observeLocalModels(): Flow<List<LocalModelEntity>> = localModelDao.observeAllModels()

    suspend fun getLoadedModelOnce(): LocalModelEntity? = localModelDao.getLoadedModelOnce()

    suspend fun upsertLocalModel(model: LocalModelEntity) = localModelDao.upsertModel(model)

    suspend fun setLoadedModel(modelId: String) = localModelDao.setLoadedModel(modelId)

    suspend fun unloadAllModels() = localModelDao.unloadAllModels()

    suspend fun deleteLocalModel(modelId: String) = localModelDao.deleteModel(modelId)
}
