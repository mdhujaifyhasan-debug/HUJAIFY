package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculationHistoryDao {
  @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC")
  fun getAllHistory(): Flow<List<CalculationHistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(entry: CalculationHistoryEntity)

  @Query("DELETE FROM calculation_history WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM calculation_history")
  suspend fun clearAll()
}
