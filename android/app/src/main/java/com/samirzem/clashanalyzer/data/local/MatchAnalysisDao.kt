package com.samirzem.clashanalyzer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchAnalysisDao {

    @Insert
    suspend fun insert(entity: MatchAnalysisEntity): Long

    @Query("SELECT * FROM match_analysis ORDER BY createdAtMs DESC")
    fun observeAll(): Flow<List<MatchAnalysisEntity>>

    @Query("SELECT * FROM match_analysis WHERE id = :id")
    suspend fun getById(id: Long): MatchAnalysisEntity?

    @Query("DELETE FROM match_analysis WHERE id = :id")
    suspend fun delete(id: Long)
}
