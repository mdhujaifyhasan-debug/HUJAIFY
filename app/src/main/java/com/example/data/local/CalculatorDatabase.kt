package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CalculationHistoryEntity::class], version = 1, exportSchema = false)
abstract class CalculatorDatabase : RoomDatabase() {
  abstract fun historyDao(): CalculationHistoryDao

  companion object {
    @Volatile private var INSTANCE: CalculatorDatabase? = null

    fun getInstance(context: Context): CalculatorDatabase {
      return INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                CalculatorDatabase::class.java,
                "smart_calc_db",
              )
              .build()
              .also { INSTANCE = it }
        }
    }
  }
}
