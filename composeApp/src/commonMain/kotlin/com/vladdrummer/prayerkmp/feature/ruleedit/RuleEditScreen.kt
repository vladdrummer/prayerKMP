package com.vladdrummer.prayerkmp.feature.ruleedit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vladdrummer.prayerkmp.feature.prayer.PrayerTextComposable
import com.vladdrummer.prayerkmp.feature.ruleedit.view_model.RuleEditViewState
import com.vladdrummer.prayerkmp.feature.ruleedit.view_model.RuleType

@Composable
fun RuleEditScreen(
    viewState: RuleEditViewState,
    modifier: Modifier = Modifier,
    editingEnabled: Boolean = true,
    onSelectRule: (RuleType) -> Unit,
    onPartCheckedChange: (Int, Boolean) -> Unit,
    onRemoveAdditionalPrayer: (String) -> Unit,
    onOpenPartPreview: (Int) -> Unit,
    onOpenAdditionalPrayerPreview: (String) -> Unit,
    onClosePreview: () -> Unit,
) {
    val additionalInsertIndex = if (viewState.selectedRule == RuleType.Morning) 20 else 12
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Настройка молитвенного правила",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Отмеченные пункты войдут в правило. Нажмите на пункт, чтобы посмотреть полный текст.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                selected = viewState.selectedRule == RuleType.Morning,
                enabled = editingEnabled,
                onClick = { onSelectRule(RuleType.Morning) },
                modifier = Modifier.weight(1f),
                label = { Text("Утреннее") }
            )
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                selected = viewState.selectedRule == RuleType.Evening,
                enabled = editingEnabled,
                onClick = { onSelectRule(RuleType.Evening) },
                modifier = Modifier.weight(1f),
                label = { Text("Вечернее") }
            )
        }

        if (viewState.isLoading) {
            Text(text = "Загрузка...", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(viewState.parts, key = { _, item -> item.index }) { index, part ->
                RulePartRow(
                    title = part.title,
                    checked = part.enabled,
                    editingEnabled = editingEnabled,
                    onCheckedChange = { onPartCheckedChange(part.index, it) },
                    onOpenPreview = { onOpenPartPreview(part.index) }
                )

                if (index == additionalInsertIndex) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Дополнительные молитвы",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (viewState.additionalPrayers.isEmpty()) {
                                Text(
                                    text = "Не добавлены",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                viewState.additionalPrayers.forEach { prayer ->
                                    AdditionalPrayerRow(
                                        title = prayer.title,
                                        onOpenPreview = { onOpenAdditionalPrayerPreview(prayer.resId) },
                                        onRemove = { onRemoveAdditionalPrayer(prayer.resId) },
                                        editingEnabled = editingEnabled,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (viewState.previewTitle != null) {
        AlertDialog(
            onDismissRequest = onClosePreview,
            confirmButton = {
                TextButton(onClick = onClosePreview) {
                    Text("Закрыть")
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 540.dp)
                ) {
                    if (viewState.isPreviewLoading) {
                        Text("Загрузка...")
                    } else {
                        PrayerTextComposable(
                            html = viewState.previewHtml.orEmpty(),
                            fontSizeSp = 18,
                            fontIndex = 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun RulePartRow(
    title: String,
    checked: Boolean,
    editingEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpenPreview: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clickable { onOpenPreview() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, enabled = editingEnabled, onCheckedChange = onCheckedChange)
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpenPreview) {
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = "Просмотр"
                )
            }
        }
    }
}

@Composable
private fun AdditionalPrayerRow(
    title: String,
    onOpenPreview: () -> Unit,
    onRemove: () -> Unit,
    editingEnabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenPreview() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onOpenPreview) {
            Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = "Просмотр"
            )
        }
        IconButton(onClick = onRemove, enabled = editingEnabled) {
            Icon(
                imageVector = Icons.Filled.Cancel,
                contentDescription = "Удалить"
            )
        }
    }
}
