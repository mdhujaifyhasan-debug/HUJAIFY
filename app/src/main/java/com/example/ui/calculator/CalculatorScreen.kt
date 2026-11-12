package com.example.ui.calculator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCalculatorColors
import kotlinx.coroutines.launch

private enum class KeyRole {
  NUMBER,
  OPERATOR,
  SCIENTIFIC,
  CLEAR,
  EQUALS,
}

private data class CalcKeySpec(
  val label: String,
  val role: KeyRole,
  val testTag: String,
  val onClick: () -> Unit,
)

@Composable
fun CalculatorScreen(
  uiState: CalculatorUiState,
  isDarkTheme: Boolean,
  viewModel: CalculatorViewModel,
) {
  var showHistorySheet by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }
  val clipboardManager = LocalClipboardManager.current
  val coroutineScope = rememberCoroutineScope()

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .statusBarsPadding()
          .navigationBarsPadding(),
      contentAlignment = Alignment.TopCenter,
    ) {
      BoxWithConstraints(
        modifier =
          Modifier.fillMaxSize()
            .widthIn(max = 600.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        val isWideLayout = maxWidth >= 560.dp && maxHeight < 520.dp
        val compactHeight = maxHeight < 660.dp

        Column(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          // 1. Minimalist Top Bar with Simple Logo, History, and Light/Dark Toggle
          TopHeaderBar(
            isDarkTheme = isDarkTheme,
            historyCount = uiState.historyItems.size,
            onToggleTheme = { viewModel.onToggleTheme(isDarkTheme) },
            onOpenHistory = { showHistorySheet = true },
          )

          Spacer(modifier = Modifier.height(if (compactHeight || isWideLayout) 6.dp else 10.dp))

          if (isWideLayout) {
            // Adaptive Landscape Layout: Display & Controls on left, Keypad(s) on right
            Row(
              modifier = Modifier.fillMaxWidth().weight(1f, fill = true),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Column(
                modifier = Modifier.weight(0.42f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                CalculatorDisplayCard(
                  expression = uiState.expression,
                  livePreview = uiState.livePreview,
                  errorMessage = uiState.errorMessage,
                  isScientificMode = uiState.isScientificMode,
                  isDegreeMode = uiState.isDegreeMode,
                  onCopyValue = { textToCopy ->
                    clipboardManager.setText(AnnotatedString(textToCopy))
                    coroutineScope.launch {
                      snackbarHostState.currentSnackbarData?.dismiss()
                      snackbarHostState.showSnackbar("Copied: $textToCopy")
                    }
                  },
                  modifier = Modifier.fillMaxWidth().weight(1f),
                )

                ModeControlBar(
                  isScientificMode = uiState.isScientificMode,
                  isDegreeMode = uiState.isDegreeMode,
                  isInverseMode = uiState.isInverseMode,
                  onToggleScientific = viewModel::onToggleScientificMode,
                  onToggleDegree = viewModel::onToggleDegreeMode,
                  onToggleInverse = viewModel::onToggleInverseMode,
                  onBackspace = viewModel::onBackspace,
                )
              }

              Row(
                modifier = Modifier.weight(0.58f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                AnimatedVisibility(
                  visible = uiState.isScientificMode,
                  modifier = Modifier.weight(0.46f).fillMaxHeight(),
                ) {
                  ScientificKeypadGrid(
                    isInverseMode = uiState.isInverseMode,
                    spacing = 6.dp,
                    viewModel = viewModel,
                  )
                }

                StandardKeypadGrid(
                  spacing = 6.dp,
                  viewModel = viewModel,
                  modifier = Modifier.weight(if (uiState.isScientificMode) 0.54f else 1f).fillMaxHeight(),
                )
              }
            }
          } else {
            // Portrait / Tall Adaptive Layout
            CalculatorDisplayCard(
              expression = uiState.expression,
              livePreview = uiState.livePreview,
              errorMessage = uiState.errorMessage,
              isScientificMode = uiState.isScientificMode,
              isDegreeMode = uiState.isDegreeMode,
              onCopyValue = { textToCopy ->
                clipboardManager.setText(AnnotatedString(textToCopy))
                coroutineScope.launch {
                  snackbarHostState.currentSnackbarData?.dismiss()
                  snackbarHostState.showSnackbar("Copied: $textToCopy")
                }
              },
              modifier =
                Modifier.fillMaxWidth()
                  .weight(if (uiState.isScientificMode) 0.23f else 0.30f, fill = true),
            )

            Spacer(modifier = Modifier.height(if (compactHeight) 8.dp else 12.dp))

            ModeControlBar(
              isScientificMode = uiState.isScientificMode,
              isDegreeMode = uiState.isDegreeMode,
              isInverseMode = uiState.isInverseMode,
              onToggleScientific = viewModel::onToggleScientificMode,
              onToggleDegree = viewModel::onToggleDegreeMode,
              onToggleInverse = viewModel::onToggleInverseMode,
              onBackspace = viewModel::onBackspace,
            )

            Spacer(modifier = Modifier.height(if (compactHeight) 6.dp else 10.dp))

            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .weight(if (uiState.isScientificMode) 0.68f else 0.60f, fill = true),
              verticalArrangement = Arrangement.spacedBy(if (compactHeight) 6.dp else 9.dp),
            ) {
              AnimatedVisibility(
                visible = uiState.isScientificMode,
                enter = expandVertically(animationSpec = spring(stiffness = 500f)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = 500f)) + fadeOut(),
                modifier = Modifier.weight(0.36f, fill = true),
              ) {
                ScientificKeypadGrid(
                  isInverseMode = uiState.isInverseMode,
                  spacing = if (compactHeight) 6.dp else 8.dp,
                  viewModel = viewModel,
                )
              }

              StandardKeypadGrid(
                spacing = if (compactHeight) 6.dp else 9.dp,
                viewModel = viewModel,
                modifier = Modifier.weight(if (uiState.isScientificMode) 0.64f else 1f, fill = true),
              )
            }
          }

          // 5. Bottom Attribution Footer
          Text(
            text = stringResource(R.string.created_by_footer),
            style = MaterialTheme.typography.labelSmall,
            color = LocalCalculatorColors.current.subtleText.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier =
              Modifier.fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp)
                .testTag("created_by_footer"),
          )
        }
      }
    }
  }

  if (showHistorySheet) {
    HistorySheet(
      historyItems = uiState.historyItems,
      onDismiss = { showHistorySheet = false },
      onSelectResult = { res -> viewModel.onReuseHistoryItem(res) },
      onSelectExpression = { expr -> viewModel.onReuseHistoryItem(expr) },
      onDeleteItem = viewModel::onDeleteHistoryItem,
      onClearAll = viewModel::onClearAllHistory,
    )
  }
}

@Composable
private fun TopHeaderBar(
  isDarkTheme: Boolean,
  historyCount: Int,
  onToggleTheme: () -> Unit,
  onOpenHistory: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.size(36.dp),
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_smart_calc_logo),
          contentDescription = stringResource(R.string.cd_app_logo),
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
        )
      }
      Spacer(modifier = Modifier.size(10.dp))
      Text(
        text = stringResource(R.string.header_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(
        onClick = onOpenHistory,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("history_button"),
      ) {
        Box(contentAlignment = Alignment.TopEnd) {
          Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = stringResource(R.string.cd_history_button),
            tint = MaterialTheme.colorScheme.onBackground,
          )
          if (historyCount > 0) {
            Surface(
              color = MaterialTheme.colorScheme.primary,
              shape = RoundedCornerShape(50),
              modifier = Modifier.size(7.dp),
            ) {}
          }
        }
      }

      IconButton(
        onClick = onToggleTheme,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("theme_toggle_button"),
      ) {
        AnimatedContent(targetState = isDarkTheme, label = "theme_icon") { dark ->
          Icon(
            imageVector = if (dark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
            contentDescription = stringResource(R.string.cd_theme_toggle),
            tint =
              if (dark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
          )
        }
      }
    }
  }
}

@Composable
private fun CalculatorDisplayCard(
  expression: String,
  livePreview: String?,
  errorMessage: String?,
  isScientificMode: Boolean,
  isDegreeMode: Boolean,
  onCopyValue: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val calcColors = LocalCalculatorColors.current
  val scrollState = rememberScrollState()

  LaunchedEffect(expression) {
    scrollState.animateScrollTo(scrollState.maxValue)
  }

  val displayText = expression.ifEmpty { "0" }
  val dynamicFontSize =
    when {
      displayText.length <= 10 -> 44.sp
      displayText.length <= 16 -> 34.sp
      displayText.length <= 24 -> 27.sp
      else -> 22.sp
    }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(24.dp),
    color = calcColors.displayBackground,
    border = BorderStroke(1.dp, calcColors.displayBorder.copy(alpha = 0.7f)),
    tonalElevation = 1.dp,
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        val statusText =
          if (isScientificMode) {
            val angle =
              if (isDegreeMode) stringResource(R.string.angle_deg)
              else stringResource(R.string.angle_rad)
            "${stringResource(R.string.mode_scientific)} • $angle"
          } else {
            stringResource(R.string.mode_basic)
          }

        Text(
          text = statusText,
          style = MaterialTheme.typography.labelSmall,
          color = calcColors.subtleText,
        )

        if (expression.isNotEmpty()) {
          IconButton(
            onClick = { onCopyValue(livePreview ?: expression) },
            modifier = Modifier.size(32.dp).testTag("copy_result_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.ContentCopy,
              contentDescription = "Copy",
              tint = calcColors.subtleText,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }

      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
          horizontalArrangement = Arrangement.End,
        ) {
          Text(
            text = displayText,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Medium,
            fontSize = dynamicFontSize,
            lineHeight = dynamicFontSize * 1.15f,
            color =
              if (expression.isEmpty()) calcColors.subtleText.copy(alpha = 0.55f)
              else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.testTag("calculator_display"),
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.CenterEnd) {
          when {
            errorMessage != null -> {
              Text(
                text = errorMessage,
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("calculator_error"),
              )
            }
            livePreview != null -> {
              Text(
                text = "= $livePreview",
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("calculator_live_preview"),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ModeControlBar(
  isScientificMode: Boolean,
  isDegreeMode: Boolean,
  isInverseMode: Boolean,
  onToggleScientific: () -> Unit,
  onToggleDegree: () -> Unit,
  onToggleInverse: () -> Unit,
  onBackspace: () -> Unit,
) {
  val calcColors = LocalCalculatorColors.current

  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Surface(
        onClick = onToggleScientific,
        shape = RoundedCornerShape(14.dp),
        color =
          if (isScientificMode) MaterialTheme.colorScheme.primary
          else calcColors.operatorKey,
        contentColor =
          if (isScientificMode) Color.White else calcColors.operatorKeyContent,
        modifier =
          Modifier.height(48.dp)
            .minimumInteractiveComponentSize()
            .testTag("scientific_toggle_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector =
              if (isScientificMode) Icons.Rounded.Calculate else Icons.Rounded.Functions,
            contentDescription = stringResource(R.string.cd_scientific_toggle),
            modifier = Modifier.size(18.dp),
          )
          Text(
            text =
              if (isScientificMode) stringResource(R.string.mode_basic)
              else stringResource(R.string.mode_scientific),
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }

      AnimatedVisibility(visible = isScientificMode) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Surface(
            onClick = onToggleDegree,
            shape = RoundedCornerShape(12.dp),
            color = calcColors.scientificKey,
            contentColor = calcColors.scientificKeyContent,
            modifier = Modifier.height(48.dp).testTag("angle_mode_button"),
          ) {
            Box(
              modifier = Modifier.padding(horizontal = 12.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text =
                  if (isDegreeMode) stringResource(R.string.angle_deg)
                  else stringResource(R.string.angle_rad),
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }

          Surface(
            onClick = onToggleInverse,
            shape = RoundedCornerShape(12.dp),
            color =
              if (isInverseMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
              else calcColors.scientificKey,
            contentColor =
              if (isInverseMode) MaterialTheme.colorScheme.primary
              else calcColors.scientificKeyContent,
            border =
              if (isInverseMode) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier.height(48.dp).testTag("inverse_mode_button"),
          ) {
            Box(
              modifier = Modifier.padding(horizontal = 12.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(text = "INV", style = MaterialTheme.typography.labelLarge)
            }
          }
        }
      }
    }

    Surface(
      onClick = onBackspace,
      shape = RoundedCornerShape(14.dp),
      color = calcColors.operatorKey.copy(alpha = 0.7f),
      contentColor = calcColors.operatorKeyContent,
      modifier = Modifier.size(width = 56.dp, height = 48.dp).testTag("backspace_button"),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.Backspace,
          contentDescription = stringResource(R.string.cd_backspace),
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}

@Composable
private fun ScientificKeypadGrid(
  isInverseMode: Boolean,
  spacing: Dp,
  viewModel: CalculatorViewModel,
  modifier: Modifier = Modifier,
) {
  val rows =
    remember(isInverseMode) {
      listOf(
        listOf(
          if (isInverseMode)
            CalcKeySpec("sin⁻¹", KeyRole.SCIENTIFIC, "key_asin") {
              viewModel.onFunction("sin⁻¹(")
            }
          else
            CalcKeySpec("sin", KeyRole.SCIENTIFIC, "key_sin") { viewModel.onFunction("sin(") },
          if (isInverseMode)
            CalcKeySpec("cos⁻¹", KeyRole.SCIENTIFIC, "key_acos") {
              viewModel.onFunction("cos⁻¹(")
            }
          else
            CalcKeySpec("cos", KeyRole.SCIENTIFIC, "key_cos") { viewModel.onFunction("cos(") },
          if (isInverseMode)
            CalcKeySpec("tan⁻¹", KeyRole.SCIENTIFIC, "key_atan") {
              viewModel.onFunction("tan⁻¹(")
            }
          else
            CalcKeySpec("tan", KeyRole.SCIENTIFIC, "key_tan") { viewModel.onFunction("tan(") },
          if (isInverseMode)
            CalcKeySpec("10ˣ", KeyRole.SCIENTIFIC, "key_10pow") { viewModel.onFunction("10^(") }
          else CalcKeySpec("log", KeyRole.SCIENTIFIC, "key_log") { viewModel.onFunction("log(") },
          if (isInverseMode)
            CalcKeySpec("eˣ", KeyRole.SCIENTIFIC, "key_epow") { viewModel.onFunction("e^(") }
          else CalcKeySpec("ln", KeyRole.SCIENTIFIC, "key_ln") { viewModel.onFunction("ln(") },
        ),
        listOf(
          CalcKeySpec("√", KeyRole.SCIENTIFIC, "key_sqrt") { viewModel.onFunction("√(") },
          CalcKeySpec("x²", KeyRole.SCIENTIFIC, "key_sq") { viewModel.onPostfix("²") },
          CalcKeySpec("xʸ", KeyRole.SCIENTIFIC, "key_pow") { viewModel.onOperator("^") },
          CalcKeySpec("π", KeyRole.SCIENTIFIC, "key_pi") { viewModel.onConstant("π") },
          CalcKeySpec("e", KeyRole.SCIENTIFIC, "key_e") { viewModel.onConstant("e") },
        ),
        listOf(
          CalcKeySpec("!", KeyRole.SCIENTIFIC, "key_fact") { viewModel.onPostfix("!") },
          CalcKeySpec("1/x", KeyRole.SCIENTIFIC, "key_recip") { viewModel.onReciprocal() },
          CalcKeySpec("|x|", KeyRole.SCIENTIFIC, "key_abs") { viewModel.onFunction("abs(") },
          CalcKeySpec("(", KeyRole.SCIENTIFIC, "key_lparen") { viewModel.onConstant("(") },
          CalcKeySpec(")", KeyRole.SCIENTIFIC, "key_rparen") { viewModel.onConstant(")") },
        ),
      )
    }

  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(spacing),
  ) {
    rows.forEach { row ->
      Row(
        modifier = Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(spacing),
      ) {
        row.forEach { spec ->
          CalculatorKeyButton(
            spec = spec,
            isCompact = true,
            modifier = Modifier.weight(1f).fillMaxHeight(),
          )
        }
      }
    }
  }
}

@Composable
private fun StandardKeypadGrid(
  spacing: Dp,
  viewModel: CalculatorViewModel,
  modifier: Modifier = Modifier,
) {
  val rows = remember {
    listOf(
      listOf(
        CalcKeySpec("AC", KeyRole.CLEAR, "key_ac") { viewModel.onClear() },
        CalcKeySpec("( )", KeyRole.OPERATOR, "key_paren") { viewModel.onSmartParenthesis() },
        CalcKeySpec("%", KeyRole.OPERATOR, "key_percent") { viewModel.onPercent() },
        CalcKeySpec("÷", KeyRole.OPERATOR, "key_div") { viewModel.onOperator("÷") },
      ),
      listOf(
        CalcKeySpec("7", KeyRole.NUMBER, "key_7") { viewModel.onDigit("7") },
        CalcKeySpec("8", KeyRole.NUMBER, "key_8") { viewModel.onDigit("8") },
        CalcKeySpec("9", KeyRole.NUMBER, "key_9") { viewModel.onDigit("9") },
        CalcKeySpec("×", KeyRole.OPERATOR, "key_mul") { viewModel.onOperator("×") },
      ),
      listOf(
        CalcKeySpec("4", KeyRole.NUMBER, "key_4") { viewModel.onDigit("4") },
        CalcKeySpec("5", KeyRole.NUMBER, "key_5") { viewModel.onDigit("5") },
        CalcKeySpec("6", KeyRole.NUMBER, "key_6") { viewModel.onDigit("6") },
        CalcKeySpec("−", KeyRole.OPERATOR, "key_sub") { viewModel.onOperator("−") },
      ),
      listOf(
        CalcKeySpec("1", KeyRole.NUMBER, "key_1") { viewModel.onDigit("1") },
        CalcKeySpec("2", KeyRole.NUMBER, "key_2") { viewModel.onDigit("2") },
        CalcKeySpec("3", KeyRole.NUMBER, "key_3") { viewModel.onDigit("3") },
        CalcKeySpec("+", KeyRole.OPERATOR, "key_add") { viewModel.onOperator("+") },
      ),
      listOf(
        CalcKeySpec("+/−", KeyRole.NUMBER, "key_sign") { viewModel.onToggleSign() },
        CalcKeySpec("0", KeyRole.NUMBER, "key_0") { viewModel.onDigit("0") },
        CalcKeySpec(".", KeyRole.NUMBER, "key_dot") { viewModel.onDecimal() },
        CalcKeySpec("=", KeyRole.EQUALS, "key_eq") { viewModel.onEquals() },
      ),
    )
  }

  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(spacing),
  ) {
    rows.forEach { row ->
      Row(
        modifier = Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(spacing),
      ) {
        row.forEach { spec ->
          CalculatorKeyButton(
            spec = spec,
            isCompact = false,
            modifier = Modifier.weight(1f).fillMaxHeight(),
          )
        }
      }
    }
  }
}

@Composable
private fun CalculatorKeyButton(
  spec: CalcKeySpec,
  isCompact: Boolean,
  modifier: Modifier = Modifier,
) {
  val calcColors = LocalCalculatorColors.current
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.94f else 1f,
    animationSpec = spring(stiffness = 700f),
    label = "key_scale",
  )

  val (containerColor, contentColor) =
    when (spec.role) {
      KeyRole.NUMBER -> calcColors.numberKey to calcColors.numberKeyContent
      KeyRole.OPERATOR -> calcColors.operatorKey to calcColors.operatorKeyContent
      KeyRole.SCIENTIFIC -> calcColors.scientificKey to calcColors.scientificKeyContent
      KeyRole.CLEAR -> calcColors.clearKey to calcColors.clearKeyContent
      KeyRole.EQUALS -> calcColors.equalsKey to calcColors.equalsKeyContent
    }

  val fontSize =
    when {
      isCompact -> 15.sp
      spec.role == KeyRole.OPERATOR || spec.role == KeyRole.EQUALS -> 24.sp
      spec.label.length > 2 -> 18.sp
      else -> 22.sp
    }

  Button(
    onClick = {
      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      spec.onClick()
    },
    interactionSource = interactionSource,
    shape = RoundedCornerShape(if (isCompact) 14.dp else 18.dp),
    colors =
      ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor,
      ),
    elevation =
      ButtonDefaults.buttonElevation(
        defaultElevation = if (spec.role == KeyRole.NUMBER || spec.role == KeyRole.EQUALS) 1.dp else 0.dp,
        pressedElevation = 0.dp,
      ),
    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .testTag(spec.testTag),
  ) {
    Text(
      text = spec.label,
      fontSize = fontSize,
      fontWeight =
        if (spec.role == KeyRole.EQUALS || spec.role == KeyRole.CLEAR) FontWeight.Bold
        else FontWeight.SemiBold,
      maxLines = 1,
      softWrap = false,
    )
  }
}
