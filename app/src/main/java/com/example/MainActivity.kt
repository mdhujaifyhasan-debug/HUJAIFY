package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.CalculatorDatabase
import com.example.data.repository.HistoryRepository
import com.example.data.repository.PreferencesRepository
import com.example.ui.calculator.CalculatorScreen
import com.example.ui.calculator.CalculatorViewModel
import com.example.ui.theme.SmartCalcTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val database = CalculatorDatabase.getInstance(applicationContext)
    val historyRepository = HistoryRepository(database.historyDao())
    val preferencesRepository = PreferencesRepository(applicationContext)
    val factory = CalculatorViewModel.Factory(historyRepository, preferencesRepository)

    setContent {
      val calculatorViewModel: CalculatorViewModel = viewModel(factory = factory)
      val uiState by calculatorViewModel.uiState.collectAsStateWithLifecycle()
      val isDark = uiState.isDarkMode ?: isSystemInDarkTheme()

      SmartCalcTheme(darkTheme = isDark) {
        CalculatorScreen(
          uiState = uiState,
          isDarkTheme = isDark,
          viewModel = calculatorViewModel,
        )
      }
    }
  }
}
