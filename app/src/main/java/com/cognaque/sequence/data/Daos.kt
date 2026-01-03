package com.cognaque.sequence.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isDone = 0")
    fun getActiveTasksFlow(): Flow<List<Task>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksOneShot(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Update
    suspend fun updateTasks(tasks: List<Task>)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): Task?

    @Query("SELECT * FROM tasks WHERE normalizedSignature = :sig AND isDone = 0 LIMIT 1")
    suspend fun findActiveTaskBySignature(sig: String): Task?

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("SELECT * FROM daily_chores")
    fun getDailyChoreTemplates(): Flow<List<DailyChore>>

    @Query("SELECT * FROM daily_chores")
    suspend fun getDailyChoreTemplatesOneShot(): List<DailyChore>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyChoreTemplate(chore: DailyChore)

    @Delete
    suspend fun deleteDailyChoreTemplate(chore: DailyChore)

    @Query("DELETE FROM daily_chores")
    suspend fun deleteAllDailyChores()

    @Query("SELECT COUNT(*) FROM tasks WHERE isDailyChore = 1 AND rawText = :text AND (isDone = 0 OR creationTimestamp >= :startOfDay)")
    suspend fun hasDailyChoreForToday(text: String, startOfDay: Long): Int
}

@Dao
interface LearningDao {
    @Query("SELECT * FROM keyword_weights WHERE keyword = :keyword")
    suspend fun getWeight(keyword: String): KeywordWeight?

    @Query("SELECT * FROM keyword_weights WHERE keyword IN (:keywords)")
    suspend fun getWeightsForKeywords(keywords: List<String>): List<KeywordWeight>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWeight(weight: KeywordWeight)

    @Query("DELETE FROM keyword_weights")
    suspend fun deleteAllWeights()

    @Query("DELETE FROM keyword_weights WHERE keyword NOT IN (SELECT keyword FROM keyword_weights ORDER BY count DESC, lastUpdated DESC LIMIT :limit)")
    suspend fun pruneExcessKeywords(limit: Int)
}
