package com.example.ui.calculator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CalculationHistoryEntity
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCalculatorColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySheet(
  historyItems: List<CalculationHistoryEntity>,
  onDismiss: () -> Unit,
  onSelectResult: (String) -> Unit,
  onSelectExpression: (String) -> Unit,
  onDeleteItem: (CalculationHistoryEntity) -> Unit,
  onClearAll: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var showClearConfirm by remember { mutableStateOf(false) }

  val todayLabel = stringResource(R.string.group_today)
  val yesterdayLabel = stringResource(R.string.group_yesterday)
  val earlierLabel = stringResource(R.string.group_earlier)

  val groupedItems =
    remember(historyItems, todayLabel, yesterdayLabel, earlierLabel) {
      val now = Calendar.getInstance()
      val startOfToday =
        (now.clone() as Calendar).apply {
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }.timeInMillis
      val startOfYesterday = startOfToday - 24L * 60L * 60L * 1000L

      historyItems.groupBy { item ->
        when {
          item.timestamp >= startOfToday -> todayLabel
          item.timestamp >= startOfYesterday -> yesterdayLabel
          else -> earlierLabel
        }
      }
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
          )
          Spacer(modifier = Modifier.size(10.dp))
          Column {
            Text(
              text = stringResource(R.string.history_title),
              style = MaterialTheme.typography.headlineSmall,
            )
            if (historyItems.isNotEmpty()) {
              Text(
                text = stringResource(R.string.history_tap_hint),
                style = MaterialTheme.typography.labelSmall,
                color = LocalCalculatorColors.current.subtleText,
              )
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (historyItems.isNotEmpty()) {
            TextButton(
              onClick = { showClearConfirm = true },
              modifier = Modifier.testTag("clear_all_history_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.DeleteSweep,
                contentDescription = stringResource(R.string.cd_clear_history),
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.size(6.dp))
              Text(text = stringResource(R.string.clear_all))
            }
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("close_history_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = stringResource(R.string.cd_close_history),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

      if (historyItems.isEmpty()) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(64.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.History,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(16.dp),
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = stringResource(R.string.history_empty_title),
            style = MaterialTheme.typography.titleMedium,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = stringResource(R.string.history_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalCalculatorColors.current.subtleText,
            textAlign = TextAlign.Center,
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().height(380.dp),
          contentPadding = PaddingValues(vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          groupedItems.forEach { (groupTitle, itemsInGroup) ->
            item(key = "header_$groupTitle") {
              Text(
                text = groupTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
              )
            }
            items(itemsInGroup, key = { it.id }) { item ->
              HistoryItemCard(
                item = item,
                onClickResult = {
                  onSelectResult(item.result)
                  onDismiss()
                },
                onClickExpression = {
                  onSelectExpression(item.expression)
                  onDismiss()
                },
                onDelete = { onDeleteItem(item) },
              )
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }

  if (showClearConfirm) {
    AlertDialog(
      onDismissRequest = { showClearConfirm = false },
      title = { Text(stringResource(R.string.clear_history_confirm_title)) },
      text = { Text(stringResource(R.string.clear_history_confirm_message)) },
      confirmButton = {
        TextButton(
          onClick = {
            showClearConfirm = false
            onClearAll()
          },
          modifier = Modifier.testTag("confirm_clear_history_button"),
        ) {
          Text(
            text = stringResource(R.string.clear_all),
            color = MaterialTheme.colorScheme.error,
          )
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirm = false }) {
          Text(stringResource(R.string.cancel))
        }
      },
    )
  }
}

@Composable
private fun HistoryItemCard(
  item: CalculationHistoryEntity,
  onClickResult: () -> Unit,
  onClickExpression: () -> Unit,
  onDelete: () -> Unit,
) {
  val timeText =
    remember(item.timestamp) {
      SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
    }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .clickable(onClick = onClickResult)
        .testTag("history_item_${item.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.expression,
          fontFamily = JetBrainsMonoFamily,
          style = MaterialTheme.typography.bodyMedium,
          color = LocalCalculatorColors.current.subtleText,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .clickable(onClick = onClickExpression)
              .padding(vertical = 2.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "= ${item.result}",
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          Text(
            text = timeText,
            style = MaterialTheme.typography.labelSmall,
            color = LocalCalculatorColors.current.subtleText,
          )
        }
      }

      IconButton(
        onClick = onDelete,
        modifier =
          Modifier.minimumInteractiveComponentSize().testTag("delete_history_${item.id}"),
      ) {
        Icon(
          imageVector = Icons.Outlined.DeleteOutline,
          contentDescription = stringResource(R.string.cd_delete_history_item),
          tint = LocalCalculatorColors.current.subtleText,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}
