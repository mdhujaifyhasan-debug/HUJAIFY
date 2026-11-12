package com.example.data.repository

import com.example.data.local.CalculationHistoryDao
import com.example.data.local.CalculationHistoryEntity
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: CalculationHistoryDao) {
  val historyItems: Flow<List<CalculationHistoryEntity>> = dao.getAllHistory()

  suspend fun saveCalculation(expression: String, result: String) {
    dao.insert(
      CalculationHistoryEntity(
        expression = expression,
        result = result,
        timestamp = System.currentTimeMillis(),
      )
    )
  }

  suspend fun deleteItem(id: Long) {
    dao.deleteById(id)
  }

  suspend fun clearAll() {
    dao.clearAll()
  }
}
