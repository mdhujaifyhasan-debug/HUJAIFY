package com.example.ui.calculator

import com.example.data.local.CalculationHistoryEntity

data class CalculatorUiState(
  val expression: String = "",
  val livePreview: String? = null,
  val errorMessage: String? = null,
  val justEvaluated: Boolean = false,
  val isScientificMode: Boolean = false,
  val isDegreeMode: Boolean = true,
  val isInverseMode: Boolean = false,
  val isDarkMode: Boolean? = null,
  val historyItems: List<CalculationHistoryEntity> = emptyList(),
)
