package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CalculatorUiTest {

  @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun basicCalculationScientificToggleAndFooterAreFunctional() {
    // Verify attribution footer is visible
    composeRule.onNodeWithTag("created_by_footer").assertIsDisplayed()

    // Perform 7 + 8 = 15
    composeRule.onNodeWithTag("key_7").performClick()
    composeRule.onNodeWithTag("key_add").performClick()
    composeRule.onNodeWithTag("key_8").performClick()
    composeRule.onNodeWithTag("key_eq").performClick()
    composeRule.onNodeWithTag("calculator_display").assertTextEquals("15")

    // Toggle Scientific mode with one tap and compute sin(30) = 0.5
    composeRule.onNodeWithTag("scientific_toggle_button").performClick()
    composeRule.onNodeWithTag("key_sin").assertIsDisplayed()
    composeRule.onNodeWithTag("key_ac").performClick()
    composeRule.onNodeWithTag("key_sin").performClick()
    composeRule.onNodeWithTag("key_3").performClick()
    composeRule.onNodeWithTag("key_0").performClick()
    composeRule.onNodeWithTag("key_eq").performClick()
    composeRule.onNodeWithTag("calculator_display").assertTextEquals("0.5")

    // Open History sheet and verify it displays cleanly
    composeRule.onNodeWithTag("history_button").performClick()
    composeRule.onNodeWithTag("close_history_button").assertIsDisplayed()
  }
}
