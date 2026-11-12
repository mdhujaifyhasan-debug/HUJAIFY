package com.example

import com.example.domain.CalculatorEngine
import com.example.domain.EvaluationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {

  private fun eval(expr: String, deg: Boolean = true): String {
    return when (val res = CalculatorEngine.evaluate(expr, deg)) {
      is EvaluationResult.Success -> res.formatted
      is EvaluationResult.Error -> error("Expected success for '$expr', got error: ${res.message}")
    }
  }

  @Test
  fun basicArithmeticAndPrecedence() {
    assertEquals("14", eval("2+3×4"))
    assertEquals("20", eval("(2+3)×4"))
    assertEquals("0.3", eval("0.1+0.2"))
    assertEquals("2.5", eval("5÷2"))
    assertEquals("30", eval("200×15%"))
    assertEquals("0.5", eval("50%"))
  }

  @Test
  fun scientificFunctionsInDegreeAndRadian() {
    assertEquals("0.5", eval("sin(30)", deg = true))
    assertEquals("0.5", eval("cos(60)", deg = true))
    assertEquals("1", eval("tan(45)", deg = true))
    assertEquals("0", eval("sin(180)", deg = true))
    assertEquals("0", eval("cos(90)", deg = true))
    assertEquals("30", eval("sin⁻¹(0.5)", deg = true))
    assertEquals("1", eval("sin(π÷2)", deg = false))
    assertEquals("2", eval("log(100)"))
    assertEquals("1", eval("ln(e)"))
    assertEquals("5", eval("√(25)"))
    assertEquals("120", eval("5!"))
    assertEquals("8", eval("2^3"))
    assertEquals("25", eval("5²"))
    assertEquals("7", eval("abs(−7)"))
  }

  @Test
  fun implicitMultiplicationAndAutoCloseParens() {
    assertEquals("14", eval("2(3+4)"))
    assertEquals("14", eval("2(3+4"))
    assertEquals("1", eval("sin(90"))
  }

  @Test
  fun errorCases() {
    assertTrue(CalculatorEngine.evaluate("5÷0", true) is EvaluationResult.Error)
    assertTrue(CalculatorEngine.evaluate("√(−4)", true) is EvaluationResult.Error)
    assertTrue(CalculatorEngine.evaluate("tan(90)", true) is EvaluationResult.Error)
    assertTrue(CalculatorEngine.evaluate("log(0)", true) is EvaluationResult.Error)
    assertTrue(CalculatorEngine.evaluate("(−2)!", true) is EvaluationResult.Error)
  }

  @Test
  fun livePreviewBehavior() {
    assertNull(CalculatorEngine.evaluatePreview("42", true))
    assertNull(CalculatorEngine.evaluatePreview("42+", true))
    assertEquals("12", CalculatorEngine.evaluatePreview("4+8", true))
    assertEquals("0.5", CalculatorEngine.evaluatePreview("sin(30", true))
  }
}
