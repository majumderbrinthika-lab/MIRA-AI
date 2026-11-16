package com.mira.memory.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mira.memory.model.CharacterProfileEntity
import com.mira.memory.model.ChatMessageEntity
import com.mira.memory.model.LocalModelEntity
import com.mira.memory.model.MemoryFactEntity
import com.mira.memory.model.StudioImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY isPermanentMira DESC, createdAt ASC")
    fun observeAllCharacters(): Flow<List<CharacterProfileEntity>>

    @Query("SELECT * FROM characters WHERE id = :id LIMIT 1")
    fun observeCharacter(id: String): Flow<CharacterProfileEntity?>

    @Query("SELECT * FROM characters WHERE id = :id LIMIT 1")
    suspend fun getCharacterOnce(id: String): CharacterProfileEntity?

    @Query("SELECT COUNT(*) FROM characters")
    suspend fun countCharacters(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCharacter(character: CharacterProfileEntity)

    @Update
    suspend fun updateCharacter(character: CharacterProfileEntity)

    @Query("DELETE FROM characters WHERE id = :id AND isPermanentMira = 0")
    suspend fun deleteNonPermanentCharacter(id: String)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE characterId = :characterId ORDER BY timestamp ASC")
    fun observeMessagesForCharacter(characterId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE characterId = :characterId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessagesOnce(characterId: String, limit: Int = 16): List<ChatMessageEntity>

    @Query("SELECT COUNT(*) FROM chat_messages")
    fun observeTotalMessageCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE characterId = :characterId")
    suspend fun clearConversation(characterId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllConversations()
}

@Dao
interface MemoryFactDao {
    @Query("SELECT * FROM memory_facts ORDER BY importance DESC, updatedAt DESC")
    fun observeAllMemories(): Flow<List<MemoryFactEntity>>

    @Query("SELECT * FROM memory_facts WHERE characterId = :characterId OR characterId = 'global' ORDER BY importance DESC, updatedAt DESC")
    fun observeMemoriesForCharacter(characterId: String): Flow<List<MemoryFactEntity>>

    @Query("SELECT * FROM memory_facts WHERE characterId = :characterId OR characterId = 'global' ORDER BY importance DESC, updatedAt DESC")
    suspend fun getMemoriesOnce(characterId: String): List<MemoryFactEntity>

    @Query("SELECT * FROM memory_facts WHERE factKey = :factKey AND characterId = :characterId LIMIT 1")
    suspend fun findByKey(characterId: String, factKey: String): MemoryFactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemory(memory: MemoryFactEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryFactEntity)

    @Query("DELETE FROM memory_facts WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM memory_facts")
    suspend fun deleteAllMemories()
}

@Dao
interface StudioImageDao {
    @Query("SELECT * FROM studio_images ORDER BY createdAt DESC")
    fun observeAllImages(): Flow<List<StudioImageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: StudioImageEntity): Long

    @Query("DELETE FROM studio_images WHERE id = :id")
    suspend fun deleteImageById(id: Long)
}

@Dao
interface LocalModelDao {
    @Query("SELECT * FROM local_models ORDER BY isLoaded DESC, addedAt DESC")
    fun observeAllModels(): Flow<List<LocalModelEntity>>

    @Query("SELECT * FROM local_models WHERE isLoaded = 1 LIMIT 1")
    suspend fun getLoadedModelOnce(): LocalModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertModel(model: LocalModelEntity)

    @Query("UPDATE local_models SET isLoaded = 0")
    suspend fun unloadAllModels()

    @Query("UPDATE local_models SET isLoaded = CASE WHEN id = :modelId THEN 1 ELSE 0 END")
    suspend fun setLoadedModel(modelId: String)

    @Query("DELETE FROM local_models WHERE id = :modelId")
    suspend fun deleteModel(modelId: String)
}
