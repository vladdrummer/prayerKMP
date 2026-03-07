package com.vladdrummer.prayerkmp.feature.prayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.TextDecrease
import androidx.compose.material.icons.outlined.TextIncrease
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vladdrummer.prayerkmp.PlatformBackHandler
import com.vladdrummer.prayerkmp.feature.prayer.view_model.PrayerViewState
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.ic_reading_nav_add_to_morning_rule
import kotlinproject.composeapp.generated.resources.ic_reading_nav_add_to_night_rule
import kotlinproject.composeapp.generated.resources.ic_reading_nav_font_aa
import kotlinproject.composeapp.generated.resources.ic_reading_nav_remove_from_morning_rule
import kotlinproject.composeapp.generated.resources.ic_reading_nav_remove_from_night_rule
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.painterResource

@Composable
fun PrayerScreen(
    viewState: PrayerViewState,
    modifier: Modifier = Modifier,
    onIncreaseFont: () -> Unit = {},
    onDecreaseFont: () -> Unit = {},
    onSwitchFont: () -> Unit = {},
    onResetFontDefaults: () -> Unit = {},
    onToggleMorning: () -> Unit = {},
    onToggleEvening: () -> Unit = {},
    onNavigateBackWithoutSave: () -> Unit = {},
    bottomActionText: String? = null,
    onBottomActionClick: (() -> Unit)? = null,
) {
    if (viewState.isLoading) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val scrollState = rememberScrollState()
    var controlsExpanded by remember { mutableStateOf(false) }
    var fontsPanelVisible by remember { mutableStateOf(false) }
    var addPanelVisible by remember { mutableStateOf(false) }
    var autoScrollPanelVisible by remember { mutableStateOf(false) }
    var autoScrollEnabled by remember { mutableStateOf(false) }
    var autoScrollSpeed by remember { mutableStateOf(0.35f) }
    val fabRotation = animateFloatAsState(
        targetValue = if (controlsExpanded) 45f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "fab_rotation"
    )
    val controlIconsTint = MaterialTheme.colorScheme.onPrimaryContainer
    var scrollRestored by remember(viewState.resId) { mutableStateOf(false) }
    PlatformBackHandler(onBack = onNavigateBackWithoutSave)
    val stopAutoScrollOnUserScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (autoScrollEnabled && source == NestedScrollSource.UserInput && available.y != 0f) {
                    autoScrollEnabled = false
                }
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(controlsExpanded, viewState.addable) {
        if (!controlsExpanded) {
            fontsPanelVisible = false
            addPanelVisible = false
            autoScrollPanelVisible = false
        } else {
            fontsPanelVisible = true
            addPanelVisible = false
            autoScrollPanelVisible = false
            delay(220)
            if (viewState.addable) addPanelVisible = true
            delay(180)
            autoScrollPanelVisible = true
        }
    }
    LaunchedEffect(autoScrollEnabled, autoScrollSpeed, viewState.text) {
        if (!autoScrollEnabled) return@LaunchedEffect
        while (isActive && autoScrollEnabled) {
            if (scrollState.value >= scrollState.maxValue) {
                autoScrollEnabled = false
                break
            }
            val stepPx = (0.8f + (autoScrollSpeed * 6f)).toInt().coerceAtLeast(1)
            val next = (scrollState.value + stepPx).coerceAtMost(scrollState.maxValue)
            scrollState.scrollTo(next)
            delay(16)
        }
    }
    LaunchedEffect(viewState.resId) {
        PrayerScrollSession.currentResId = viewState.resId
    }
    LaunchedEffect(scrollState.value, viewState.resId) {
        if (PrayerScrollSession.currentResId == viewState.resId) {
            PrayerScrollSession.currentScroll = scrollState.value
        }
    }
    LaunchedEffect(viewState.initialScrollPosition, viewState.resId, scrollState.maxValue) {
        if (!scrollRestored && viewState.initialScrollPosition > 0) {
            scrollState.scrollTo(viewState.initialScrollPosition.coerceAtMost(scrollState.maxValue))
            scrollRestored = true
        }
        if (!scrollRestored && viewState.initialScrollPosition == 0) {
            scrollRestored = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(stopAutoScrollOnUserScroll)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrayerTextComposable(
                html = viewState.text,
                fontSizeSp = viewState.fontSizeSp,
                fontIndex = viewState.fontIndex,
                modifier = Modifier.fillMaxWidth()
            )
            if (!bottomActionText.isNullOrBlank() && onBottomActionClick != null) {
                Button(
                    onClick = onBottomActionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    Text(bottomActionText)
                }
                Spacer(modifier = Modifier.height(120.dp))
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.padding(end = 8.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(
                    visible = autoScrollPanelVisible,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                            slideInVertically(
                                initialOffsetY = { it / 2 },
                                animationSpec = spring(stiffness = Spring.StiffnessLow)
                            ) +
                            expandVertically(
                                expandFrom = Alignment.Bottom,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            ),
                    exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                            slideOutVertically(
                                targetOffsetY = { it / 2 },
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            ) +
                            shrinkVertically(
                                shrinkTowards = Alignment.Bottom,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            )
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 2.dp,
                        shadowElevation = 4.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.align(Alignment.End),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Автоскролл",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Switch(
                                    checked = autoScrollEnabled,
                                    onCheckedChange = { autoScrollEnabled = it },
                                )
                            }
                            Slider(
                                value = autoScrollSpeed,
                                onValueChange = { autoScrollSpeed = it },
                                valueRange = 0f..1f,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .width(170.dp)
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = addPanelVisible && viewState.addable,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                            slideInVertically(
                                initialOffsetY = { it / 2 },
                                animationSpec = spring(stiffness = Spring.StiffnessLow)
                            ) +
                            expandVertically(
                                expandFrom = Alignment.Bottom,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            ),
                    exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                            slideOutVertically(
                                targetOffsetY = { it / 2 },
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            ) +
                            shrinkVertically(
                                shrinkTowards = Alignment.Bottom,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            )
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 2.dp,
                        shadowElevation = 4.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .wrapContentSize()
                                .defaultMinSize(minHeight = 44.dp)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Добавить эту\nмолитву в правило",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Row(
                                modifier = Modifier.wrapContentSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    modifier = Modifier.widthIn(100.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    IconButton(
                                        onClick = onToggleMorning,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (viewState.isAddedToMorning) {
                                                    Res.drawable.ic_reading_nav_remove_from_morning_rule
                                                } else {
                                                    Res.drawable.ic_reading_nav_add_to_morning_rule
                                                }
                                            ),
                                            contentDescription = "Добавить в утреннее",
                                            tint = controlIconsTint,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = onToggleEvening,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (viewState.isAddedToEvening) {
                                                    Res.drawable.ic_reading_nav_remove_from_night_rule
                                                } else {
                                                    Res.drawable.ic_reading_nav_add_to_night_rule
                                                }
                                            ),
                                            contentDescription = "Добавить в вечернее",
                                            tint = controlIconsTint,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = fontsPanelVisible,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                            slideInHorizontally(
                                initialOffsetX = { it / 3 },
                                animationSpec = spring(stiffness = Spring.StiffnessLow)
                            ) +
                            expandHorizontally(
                                expandFrom = Alignment.End,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            ),
                    exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                            slideOutHorizontally(
                                targetOffsetX = { it / 3 },
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            ) +
                            shrinkHorizontally(
                                shrinkTowards = Alignment.End,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            )
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 2.dp,
                        shadowElevation = 4.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 44.dp)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Шрифт / язык",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Row(
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onIncreaseFont,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.TextIncrease,
                                        contentDescription = "Шрифт больше",
                                        tint = controlIconsTint,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onDecreaseFont,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.TextDecrease,
                                        contentDescription = "Шрифт меньше",
                                        tint = controlIconsTint,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onSwitchFont,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_reading_nav_font_aa),
                                        contentDescription = "Сменить шрифт",
                                        tint = controlIconsTint,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onResetFontDefaults,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.RestartAlt,
                                        contentDescription = "Вернуть к настройкам по умолчанию",
                                        tint = controlIconsTint,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            FloatingActionButton(
                onClick = { controlsExpanded = !controlsExpanded },
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Параметры текста",
                    modifier = Modifier
                        .size(30.dp)
                        .rotate(fabRotation.value)
                )
            }
        }
    }
}
