package com.example.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

sealed interface EvaluationResult {
  data class Success(val formatted: String, val value: Double) : EvaluationResult

  data class Error(val message: String) : EvaluationResult
}

object CalculatorEngine {

  private sealed interface Token {
    data class Number(val value: Double) : Token

    data class Constant(val name: String, val value: Double) : Token

    data class Function(val name: String) : Token

    data class Operator(val symbol: Char) : Token

    data object LeftParen : Token

    data object RightParen : Token

    data object Factorial : Token

    data object Percent : Token
  }

  fun evaluate(expression: String, isDegreeMode: Boolean): EvaluationResult {
    val trimmed = expression.trim()
    if (trimmed.isEmpty()) return EvaluationResult.Error("Empty")

    return try {
      val balanced = autoCloseParentheses(trimmed)
      val tokens = tokenize(balanced)
      if (tokens.isEmpty()) return EvaluationResult.Error("Invalid expression")
      val parser = Parser(tokens, isDegreeMode)
      val result = parser.parseExpression()
      if (!parser.isAtEnd()) {
        return EvaluationResult.Error("Syntax error")
      }
      if (result.isNaN() || result.isInfinite()) {
        return EvaluationResult.Error("Math error")
      }
      EvaluationResult.Success(formatResult(result), result)
    } catch (e: CalculatorException) {
      EvaluationResult.Error(e.message ?: "Error")
    } catch (_: Exception) {
      EvaluationResult.Error("Syntax error")
    }
  }

  fun evaluatePreview(expression: String, isDegreeMode: Boolean): String? {
    val trimmed = expression.trim()
    if (trimmed.isEmpty()) return null
    // Don't evaluate if expression is just a single number or constant with no operator/function
    val hasOperation =
      trimmed.any { it in charArrayOf('+', '−', '-', '×', '*', '÷', '/', '^', '!', '%', '√', '(') } ||
        trimmed.any { it.isLetter() && it != 'E' && it != 'e' && it != 'π' } ||
        trimmed.contains("π") ||
        trimmed == "e"
    if (!hasOperation) return null
    // Avoid showing error if user is mid-expression ending with an operator or open paren
    val last = trimmed.last()
    if (last in charArrayOf('+', '−', '-', '×', '*', '÷', '/', '^', '(')) return null

    return when (val res = evaluate(trimmed, isDegreeMode)) {
      is EvaluationResult.Success -> res.formatted
      is EvaluationResult.Error -> null
    }
  }

  fun autoCloseParentheses(expr: String): String {
    var open = 0
    for (ch in expr) {
      if (ch == '(') open++ else if (ch == ')' && open > 0) open--
    }
    return if (open > 0) expr + ")".repeat(open) else expr
  }

  fun formatResult(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "Error"
    // Clean near-zero floating point noise (e.g. sin(180°) = 1.22E-16 -> 0)
    val cleaned = if (abs(value) < 1e-12) 0.0 else value
    if (cleaned == 0.0) return "0"

    val absVal = abs(cleaned)
    return if (absVal >= 1e12 || (absVal > 0 && absVal < 1e-7)) {
      val bd = BigDecimal.valueOf(cleaned).round(MathContext(10, RoundingMode.HALF_UP)).stripTrailingZeros()
      bd.toString().replace("E+", "E")
    } else {
      val bd = BigDecimal.valueOf(cleaned).round(MathContext(12, RoundingMode.HALF_UP)).stripTrailingZeros()
      bd.toPlainString()
    }
  }

  private fun tokenize(input: String): List<Token> {
    val raw = mutableListOf<Token>()
    var i = 0
    val n = input.length

    while (i < n) {
      val ch = input[i]
      when {
        ch.isWhitespace() -> i++
        ch.isDigit() || ch == '.' -> {
          val start = i
          var dotSeen = ch == '.'
          i++
          while (i < n && (input[i].isDigit() || (input[i] == '.' && !dotSeen))) {
            if (input[i] == '.') dotSeen = true
            i++
          }
          // Support scientific notation like 1.5E-4 or 1.5E+10
          if (i < n && input[i] == 'E') {
            val expStart = i
            i++
            if (i < n && (input[i] == '+' || input[i] == '-')) i++
            if (i < n && input[i].isDigit()) {
              while (i < n && input[i].isDigit()) i++
            } else {
              i = expStart
            }
          }
          val numStr = input.substring(start, i)
          val num = numStr.toDoubleOrNull() ?: throw CalculatorException("Invalid number")
          raw.add(Token.Number(num))
        }
        ch == 'π' -> {
          raw.add(Token.Constant("π", PI))
          i++
        }
        ch == 'e' -> {
          raw.add(Token.Constant("e", Math.E))
          i++
        }
        ch == '√' -> {
          raw.add(Token.Function("sqrt"))
          i++
        }
        ch.isLetter() -> {
          val start = i
          while (i < n && (input[i].isLetter() || input[i].isDigit() || input[i] == '⁻' || input[i] == '¹')) {
            i++
          }
          val name = input.substring(start, i)
          when (name) {
            "sin", "cos", "tan", "asin", "acos", "atan",
            "sin⁻¹", "cos⁻¹", "tan⁻¹", "log", "ln", "sqrt", "abs" -> {
              raw.add(Token.Function(name))
            }
            else -> throw CalculatorException("Unknown function: $name")
          }
        }
        ch == '+' -> {
          raw.add(Token.Operator('+'))
          i++
        }
        ch == '-' || ch == '−' -> {
          raw.add(Token.Operator('-'))
          i++
        }
        ch == '*' || ch == '×' -> {
          raw.add(Token.Operator('*'))
          i++
        }
        ch == '/' || ch == '÷' -> {
          raw.add(Token.Operator('/'))
          i++
        }
        ch == '^' -> {
          raw.add(Token.Operator('^'))
          i++
        }
        ch == '(' -> {
          raw.add(Token.LeftParen)
          i++
        }
        ch == ')' -> {
          raw.add(Token.RightParen)
          i++
        }
        ch == '!' -> {
          raw.add(Token.Factorial)
          i++
        }
        ch == '%' -> {
          raw.add(Token.Percent)
          i++
        }
        ch == '²' -> {
          raw.add(Token.Operator('^'))
          raw.add(Token.Number(2.0))
          i++
        }
        else -> throw CalculatorException("Unexpected character: $ch")
      }
    }

    // Insert implicit multiplication where mathematically appropriate:
    // e.g. 2π, 2(3), (2)(3), π(2), 2sin(30), 5!2, 50%(2)
    val result = mutableListOf<Token>()
    for (idx in raw.indices) {
      val curr = raw[idx]
      if (result.isNotEmpty()) {
        val prev = result.last()
        val prevProducesValue =
          prev is Token.Number ||
            prev is Token.Constant ||
            prev is Token.RightParen ||
            prev is Token.Factorial ||
            prev is Token.Percent
        val currStartsValue =
          curr is Token.Number ||
            curr is Token.Constant ||
            curr is Token.Function ||
            curr is Token.LeftParen

        if (prevProducesValue && currStartsValue) {
          result.add(Token.Operator('*'))
        }
      }
      result.add(curr)
    }

    return result
  }

  private class Parser(
    private val tokens: List<Token>,
    private val isDegreeMode: Boolean,
  ) {
    private var pos = 0

    fun isAtEnd(): Boolean = pos >= tokens.size

    private fun peek(): Token? = tokens.getOrNull(pos)

    private fun advance(): Token = tokens[pos++]

    fun parseExpression(): Double {
      var left = parseTerm()
      while (true) {
        val tok = peek()
        if (tok is Token.Operator && (tok.symbol == '+' || tok.symbol == '-')) {
          advance()
          val right = parseTerm()
          left = if (tok.symbol == '+') left + right else left - right
        } else {
          break
        }
      }
      return left
    }

    private fun parseTerm(): Double {
      var left = parseUnary()
      while (true) {
        val tok = peek()
        if (tok is Token.Operator && (tok.symbol == '*' || tok.symbol == '/')) {
          advance()
          val right = parseUnary()
          if (tok.symbol == '/') {
            if (abs(right) < 1e-15) throw CalculatorException("Cannot divide by 0")
            left /= right
          } else {
            left *= right
          }
        } else {
          break
        }
      }
      return left
    }

    private fun parseUnary(): Double {
      val tok = peek()
      if (tok is Token.Operator && (tok.symbol == '+' || tok.symbol == '-')) {
        advance()
        val value = parseUnary()
        return if (tok.symbol == '-') -value else value
      }
      return parsePower()
    }

    private fun parsePower(): Double {
      val base = parsePostfix()
      val tok = peek()
      if (tok is Token.Operator && tok.symbol == '^') {
        advance()
        // Right-associative exponentiation, unary allowed in exponent (e.g. 2^-3)
        val exponent = parseUnary()
        val res = base.pow(exponent)
        if (res.isNaN() || res.isInfinite()) throw CalculatorException("Domain error")
        return res
      }
      return base
    }

    private fun parsePostfix(): Double {
      var value = parsePrimary()
      while (true) {
        when (peek()) {
          is Token.Factorial -> {
            advance()
            value = factorial(value)
          }
          is Token.Percent -> {
            advance()
            value /= 100.0
          }
          else -> break
        }
      }
      return value
    }

    private fun parsePrimary(): Double {
      val tok = peek() ?: throw CalculatorException("Syntax error")
      return when (tok) {
        is Token.Number -> {
          advance()
          tok.value
        }
        is Token.Constant -> {
          advance()
          tok.value
        }
        is Token.LeftParen -> {
          advance()
          val value = parseExpression()
          val closing = peek()
          if (closing !is Token.RightParen) throw CalculatorException("Missing ')'")
          advance()
          value
        }
        is Token.Function -> {
          advance()
          val arg =
            if (peek() is Token.LeftParen) {
              advance()
              val inner = parseExpression()
              if (peek() !is Token.RightParen) throw CalculatorException("Missing ')'")
              advance()
              inner
            } else {
              // Support e.g. √9 without parens
              parseUnary()
            }
          applyFunction(tok.name, arg)
        }
        else -> throw CalculatorException("Syntax error")
      }
    }

    private fun applyFunction(name: String, arg: Double): Double {
      return when (name) {
        "sin" -> {
          val rad = if (isDegreeMode) Math.toRadians(arg) else arg
          sin(rad)
        }
        "cos" -> {
          val rad = if (isDegreeMode) Math.toRadians(arg) else arg
          cos(rad)
        }
        "tan" -> {
          val rad = if (isDegreeMode) Math.toRadians(arg) else arg
          if (abs(cos(rad)) < 1e-12) throw CalculatorException("Undefined")
          tan(rad)
        }
        "asin", "sin⁻¹" -> {
          if (arg < -1.0 || arg > 1.0) throw CalculatorException("Domain error")
          val rad = asin(arg)
          if (isDegreeMode) Math.toDegrees(rad) else rad
        }
        "acos", "cos⁻¹" -> {
          if (arg < -1.0 || arg > 1.0) throw CalculatorException("Domain error")
          val rad = acos(arg)
          if (isDegreeMode) Math.toDegrees(rad) else rad
        }
        "atan", "tan⁻¹" -> {
          val rad = atan(arg)
          if (isDegreeMode) Math.toDegrees(rad) else rad
        }
        "log" -> {
          if (arg <= 0.0) throw CalculatorException("Domain error")
          log10(arg)
        }
        "ln" -> {
          if (arg <= 0.0) throw CalculatorException("Domain error")
          ln(arg)
        }
        "sqrt" -> {
          if (arg < 0.0) throw CalculatorException("Domain error")
          sqrt(arg)
        }
        "abs" -> abs(arg)
        "exp" -> exp(arg)
        else -> throw CalculatorException("Unknown function")
      }
    }

    private fun factorial(x: Double): Double {
      if (x < 0.0 || x != kotlin.math.floor(x) || x > 170.0) {
        throw CalculatorException("Domain error")
      }
      var result = 1.0
      for (k in 2..x.toInt()) {
        result *= k
      }
      return result
    }
  }

  private class CalculatorException(message: String) : RuntimeException(message)
}
