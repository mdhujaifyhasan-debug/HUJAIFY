package com.example.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CalculationHistoryEntity
import com.example.data.repository.HistoryRepository
import com.example.data.repository.PreferencesRepository
import com.example.domain.CalculatorEngine
import com.example.domain.EvaluationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CalculatorViewModel(
  private val historyRepository: HistoryRepository,
  private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

  private val _state = MutableStateFlow(CalculatorUiState())

  val uiState: StateFlow<CalculatorUiState> =
    combine(
      _state,
      historyRepository.historyItems,
      preferencesRepository.darkModePreference,
      preferencesRepository.isDegreeMode,
    ) { state, history, darkModePref, degreeMode ->
      val preview =
        if (state.justEvaluated || state.errorMessage != null) {
          null
        } else {
          CalculatorEngine.evaluatePreview(state.expression, degreeMode)
        }
      state.copy(
        livePreview = preview,
        isDarkMode = darkModePref,
        isDegreeMode = degreeMode,
        historyItems = history,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = CalculatorUiState(),
    )

  fun onDigit(digit: String) {
    _state.update { current ->
      val base = if (current.justEvaluated || current.errorMessage != null) "" else current.expression
      val updated =
        if (base == "0" && digit != ".") {
          digit
        } else {
          base + digit
        }
      current.copy(expression = updated, errorMessage = null, justEvaluated = false)
    }
  }

  fun onDecimal() {
    _state.update { current ->
      val base = if (current.justEvaluated || current.errorMessage != null) "" else current.expression
      val trailingNumber = takeTrailingNumber(base)
      if (trailingNumber.contains('.')) {
        return@update current
      }
      val lastChar = base.lastOrNull()
      val updated =
        when {
          base.isEmpty() -> "0."
          lastChar != null && (lastChar.isDigit()) -> "$base."
          lastChar == ')' || lastChar == 'π' || lastChar == 'e' || lastChar == '!' || lastChar == '%' || lastChar == '²' ->
            "${base}×0."
          else -> "${base}0."
        }
      current.copy(expression = updated, errorMessage = null, justEvaluated = false)
    }
  }

  fun onOperator(op: String) {
    _state.update { current ->
      if (current.errorMessage != null) return@update current
      val base = current.expression
      if (base.isEmpty()) {
        // Allow unary minus at start
        return@update if (op == "−") {
          current.copy(expression = "−", justEvaluated = false)
        } else {
          current
        }
      }

      val last = base.last()
      val binaryOps = setOf('+', '−', '×', '÷', '^')
      val updated =
        when {
          last in binaryOps -> {
            // Allow negative exponent or negative factor after ×, ÷, ^
            if (op == "−" && (last == '×' || last == '÷' || last == '^')) {
              base + op
            } else if (base.length == 1 && last == '−') {
              base
            } else {
              base.dropLast(1) + op
            }
          }
          last == '(' -> {
            if (op == "−") base + op else base
          }
          else -> base + op
        }
      current.copy(expression = updated, errorMessage = null, justEvaluated = false)
    }
  }

  fun onPercent() {
    _state.update { current ->
      if (current.errorMessage != null || current.expression.isEmpty()) return@update current
      val last = current.expression.last()
      if (last.isDigit() || last == ')' || last == 'π' || last == 'e' || last == '!' || last == '²') {
        current.copy(expression = current.expression + "%", justEvaluated = false)
      } else {
        current
      }
    }
  }

  fun onSmartParenthesis() {
    _state.update { current ->
      val base = if (current.justEvaluated || current.errorMessage != null) "" else current.expression
      val openCount = base.count { it == '(' }
      val closeCount = base.count { it == ')' }
      val last = base.lastOrNull()
      val canClose =
        openCount > closeCount &&
          last != null &&
          (last.isDigit() || last == ')' || last == 'π' || last == 'e' || last == '!' || last == '%' || last == '²')

      val next = if (canClose) "$base)" else "$base("
      current.copy(expression = next, errorMessage = null, justEvaluated = false)
    }
  }

  fun onToggleSign() {
    _state.update { current ->
      val base = current.expression
      if (current.errorMessage != null || base.isEmpty()) {
        return@update current.copy(expression = "−", errorMessage = null, justEvaluated = false)
      }
      // If entire expression is a simple number, toggle leading minus
      if (base.startsWith("−") && base.drop(1).all { it.isDigit() || it == '.' }) {
        return@update current.copy(expression = base.drop(1), justEvaluated = false)
      }
      if (base.all { it.isDigit() || it == '.' }) {
        return@update current.copy(expression = "−$base", justEvaluated = false)
      }
      // Otherwise append (− or toggle trailing (−
      val updated =
        if (base.endsWith("(−")) {
          base.dropLast(2)
        } else {
          "$base(−"
        }
      current.copy(expression = updated, justEvaluated = false)
    }
  }

  fun onFunction(funcToken: String) {
    _state.update { current ->
      val base = if (current.justEvaluated || current.errorMessage != null) "" else current.expression
      current.copy(expression = base + funcToken, errorMessage = null, justEvaluated = false)
    }
  }

  fun onConstant(constant: String) {
    _state.update { current ->
      val base = if (current.justEvaluated || current.errorMessage != null) "" else current.expression
      current.copy(expression = base + constant, errorMessage = null, justEvaluated = false)
    }
  }

  fun onPostfix(symbol: String) {
    _state.update { current ->
      if (current.errorMessage != null || current.expression.isEmpty()) return@update current
      val last = current.expression.last()
      if (last.isDigit() || last == ')' || last == 'π' || last == 'e') {
        current.copy(expression = current.expression + symbol, justEvaluated = false)
      } else {
        current
      }
    }
  }

  fun onReciprocal() {
    _state.update { current ->
      if (current.errorMessage != null) return@update current
      val base = current.expression
      val updated =
        if (base.isEmpty()) {
          "1÷("
        } else if (base.all { it.isDigit() || it == '.' }) {
          "1÷$base"
        } else {
          "1÷($base)"
        }
      current.copy(expression = updated, justEvaluated = false)
    }
  }

  fun onClear() {
    _state.update { current ->
      current.copy(expression = "", errorMessage = null, justEvaluated = false)
    }
  }

  fun onBackspace() {
    _state.update { current ->
      if (current.errorMessage != null) {
        return@update current.copy(errorMessage = null, justEvaluated = false)
      }
      val expr = current.expression
      if (expr.isEmpty()) return@update current

      val multiTokens =
        listOf(
          "sin⁻¹(",
          "cos⁻¹(",
          "tan⁻¹(",
          "asin(",
          "acos(",
          "atan(",
          "sin(",
          "cos(",
          "tan(",
          "log(",
          "abs(",
          "ln(",
          "10^(",
          "e^(",
          "√(",
        )
      val matchedToken = multiTokens.firstOrNull { expr.endsWith(it) }
      val updated =
        if (matchedToken != null) {
          expr.dropLast(matchedToken.length)
        } else {
          expr.dropLast(1)
        }
      current.copy(expression = updated, errorMessage = null, justEvaluated = false)
    }
  }

  fun onEquals() {
    val current = uiState.value
    val expr = current.expression.trim()
    if (expr.isEmpty()) return

    when (val result = CalculatorEngine.evaluate(expr, current.isDegreeMode)) {
      is EvaluationResult.Success -> {
        val cleanExpr = CalculatorEngine.autoCloseParentheses(expr)
        _state.update {
          it.copy(
            expression = result.formatted,
            errorMessage = null,
            justEvaluated = true,
          )
        }
        // Only save meaningful calculations (not e.g. pressing = on "5")
        if (cleanExpr != result.formatted) {
          viewModelScope.launch {
            historyRepository.saveCalculation(
              expression = cleanExpr,
              result = result.formatted,
            )
          }
        }
      }
      is EvaluationResult.Error -> {
        _state.update { it.copy(errorMessage = result.message, justEvaluated = false) }
      }
    }
  }

  fun onToggleScientificMode() {
    _state.update { it.copy(isScientificMode = !it.isScientificMode) }
  }

  fun onToggleInverseMode() {
    _state.update { it.copy(isInverseMode = !it.isInverseMode) }
  }

  fun onToggleDegreeMode() {
    val next = !uiState.value.isDegreeMode
    viewModelScope.launch { preferencesRepository.setDegreeMode(next) }
  }

  fun onToggleTheme(currentlyDark: Boolean) {
    viewModelScope.launch { preferencesRepository.setDarkMode(!currentlyDark) }
  }

  fun onReuseHistoryItem(valueToInsert: String) {
    _state.update { current ->
      current.copy(
        expression = valueToInsert,
        errorMessage = null,
        justEvaluated = false,
      )
    }
  }

  fun onDeleteHistoryItem(item: CalculationHistoryEntity) {
    viewModelScope.launch { historyRepository.deleteItem(item.id) }
  }

  fun onClearAllHistory() {
    viewModelScope.launch { historyRepository.clearAll() }
  }

  private fun takeTrailingNumber(expr: String): String {
    var i = expr.length - 1
    while (i >= 0 && (expr[i].isDigit() || expr[i] == '.')) {
      i--
    }
    return expr.substring(i + 1)
  }

  class Factory(
    private val historyRepository: HistoryRepository,
    private val preferencesRepository: PreferencesRepository,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return CalculatorViewModel(historyRepository, preferencesRepository) as T
    }
  }
}
