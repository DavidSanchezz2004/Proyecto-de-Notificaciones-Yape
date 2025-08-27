package com.yape.yapenotificaciones.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface YapeoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(yapeo: Yapeo): Long

    @Query("DELETE FROM yapeos")
    suspend fun clearAll()

    @Query("SELECT * FROM yapeos ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Yapeo>>

    @Query("""
        SELECT * FROM yapeos 
        WHERE timestamp BETWEEN :startInclusive AND :endInclusive
        ORDER BY timestamp DESC
    """)
    suspend fun getBetween(startInclusive: Long, endInclusive: Long): List<Yapeo>

    @Query("""
        SELECT * FROM yapeos 
        WHERE direction = 'RECEIVED' AND timestamp BETWEEN :startInclusive AND :endInclusive
        ORDER BY timestamp DESC
    """)
    suspend fun getReceivedBetween(startInclusive: Long, endInclusive: Long): List<Yapeo>
}
