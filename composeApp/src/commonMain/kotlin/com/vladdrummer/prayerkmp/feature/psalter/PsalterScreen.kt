package com.vladdrummer.prayerkmp.feature.psalter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vladdrummer.prayerkmp.feature.psalter.view_model.PsalterSelectableName
import com.vladdrummer.prayerkmp.feature.psalter.view_model.PsalterViewState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsalterScreen(
    viewState: PsalterViewState,
    modifier: Modifier = Modifier,
    onModeSelected: (PsalterMode) -> Unit = {},
    onToggleKathisma: (Int) -> Unit = {},
    onContinueReadClick: (PsalterMode, Int, Int) -> Unit = { _, _, _ -> },
    onPsalmClick: (Int, Int) -> Unit = { _, _ -> },
    onNameToggle: (String) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ModeToggle(
                selectedMode = viewState.selectedMode,
                onModeSelected = onModeSelected
            )
        }
        val lastMode = viewState.lastReadMode
        val lastKathisma = viewState.lastReadKathisma
        val lastPage = viewState.lastReadPage
        if (lastMode != null && lastKathisma != null && lastPage != null) {
            item {
                Card(
                    onClick = { onContinueReadClick(lastMode, lastKathisma, lastPage) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Продолжить чтение", style = MaterialTheme.typography.titleMedium)
                        Text("Кафизма $lastKathisma, страница ${lastPage + 1}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        when (viewState.selectedMode) {
            PsalterMode.OverDead -> {
                if (viewState.deadNames.isNotEmpty()) {
                    item {
                        NamesSelector(
                            title = "За кого читаем",
                            names = viewState.deadNames,
                            onNameToggle = onNameToggle
                        )
                    }
                }
            }
            PsalterMode.OverHealth -> {
                if (viewState.healthNames.isNotEmpty()) {
                    item {
                        NamesSelector(
                            title = "О ком читаем",
                            names = viewState.healthNames,
                            onNameToggle = onNameToggle
                        )
                    }
                }
            }
            PsalterMode.Usual -> Unit
        }
        items(viewState.kathismas) { kathisma ->
            val expanded = viewState.expandedKathismas.contains(kathisma)
            val psalms = viewState.kathismaPsalms[kathisma].orEmpty()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleKathisma(kathisma) },
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = "Кафизма $kathisma. Псалмы:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expanded) "Свернуть" else "Развернуть",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    AnimatedVisibility(
                        visible = expanded,
                        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                            expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                        exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                            shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            psalms.forEach { psalm ->
                                Surface(
                                    modifier = Modifier.clickable { onPsalmClick(kathisma, psalm) },
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = psalm.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NamesSelector(
    title: String,
    names: List<PsalterSelectableName>,
    onNameToggle: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                names.forEach { entry ->
                    val id = entry.id
                    val name = entry.title
                    val selected = entry.selected
                    Surface(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onNameToggle(id) },
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = if (selected) "$name ✓" else name,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeToggle(
    selectedMode: PsalterMode,
    onModeSelected: (PsalterMode) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToggleItem(
                title = "Обычный",
                selected = selectedMode == PsalterMode.Usual,
                modifier = Modifier.weight(1f)
            ) { onModeSelected(PsalterMode.Usual) }
            ToggleItem(
                title = "За здравие",
                selected = selectedMode == PsalterMode.OverHealth,
                modifier = Modifier.weight(1f)
            ) { onModeSelected(PsalterMode.OverHealth) }
            ToggleItem(
                title = "За усопших",
                selected = selectedMode == PsalterMode.OverDead,
                modifier = Modifier.weight(1f)
            ) { onModeSelected(PsalterMode.OverDead) }
        }
    }
}

@Composable
private fun ToggleItem(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(bg)
            .clickable { onClick() },
        color = bg
    ) {
        Text(
            text = title,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp)
        )
    }
}
