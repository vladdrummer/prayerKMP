package com.vladdrummer.prayerkmp.feature.personaldata

import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import com.vladdrummer.prayerkmp.feature.auth.GoogleEmailAuthBottomSheet
import com.vladdrummer.prayerkmp.feature.personaldata.view_model.DEFAULT_DUHOVNIK
import com.vladdrummer.prayerkmp.feature.personaldata.view_model.DEFAULT_NAME_IMENIT
import com.vladdrummer.prayerkmp.feature.personaldata.view_model.DEFAULT_PERSON_NAME
import com.vladdrummer.prayerkmp.feature.personaldata.view_model.PersonalDataViewState
import com.vladdrummer.prayerkmp.feature.personaldata.view_model.PersonalPerson
import com.vladdrummer.prayerkmp.feature.personaldata.view_model.PersonalSectionType
import com.vladdrummer.prayerkmp.feature.personaldata.view_model.statusList

@Composable
fun PersonalDataScreen(
    viewState: PersonalDataViewState,
    onNameImenitChanged: (String) -> Unit,
    onDuhovnikChanged: (String) -> Unit,
    onGenderChanged: (Boolean) -> Unit,
    onPersonNameChanged: (PersonalSectionType, Int, String) -> Unit,
    onPersonGenderChanged: (PersonalSectionType, Int, Int) -> Unit,
    onPersonStatusChanged: (PersonalSectionType, Int, Int) -> Unit,
    onPersonAdded: (PersonalSectionType) -> Unit,
    onPersonRemoved: (PersonalSectionType, Int) -> Unit,
    onGoogleAccountSelected: (String) -> Unit,
    onGoogleAccountCleared: () -> Unit,
) {
    var isGoogleSheetVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "about_me_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "О себе",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = if (viewState.nameImenit == DEFAULT_NAME_IMENIT) "" else viewState.nameImenit,
                        onValueChange = onNameImenitChanged,
                        singleLine = true,
                        label = { Text("Имя") },
                        placeholder = { Text(DEFAULT_NAME_IMENIT) },
                    )
                    GenderToggle(
                        isMale = viewState.isMale,
                        onSelectMale = { onGenderChanged(true) },
                        onSelectFemale = { onGenderChanged(false) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item(key = "about_mentor_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "О духовнике",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = if (viewState.duhovnik == DEFAULT_DUHOVNIK) "" else viewState.duhovnik,
                        onValueChange = onDuhovnikChanged,
                        singleLine = true,
                        label = { Text("Имя духовника") },
                        placeholder = { Text(DEFAULT_DUHOVNIK) },
                    )
                }
            }
        }

        item(key = "google_account_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Облако и авторизация",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (viewState.googleAccountEmail.isBlank()) {
                            "Google-аккаунт не подключен"
                        } else {
                            "Подключен: ${viewState.googleAccountEmail}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactActionButton(
                            text = if (viewState.googleAccountEmail.isBlank()) "Подключить" else "Сменить",
                            onClick = {
                                isGoogleSheetVisible = true
                            }
                        )
                        if (viewState.googleAccountEmail.isNotBlank()) {
                            CompactActionButton(text = "Отключить", onClick = onGoogleAccountCleared)
                        }
                    }
                }
            }
        }

        val orderedSections = listOf(
            PersonalSectionType.Parents,
            PersonalSectionType.Relatives,
            PersonalSectionType.Children,
            PersonalSectionType.GodChildren,
            PersonalSectionType.Benefactors,
            PersonalSectionType.Dead,
        ).mapNotNull { type -> viewState.sections.firstOrNull { it.type == type } }

        orderedSections.forEach { section ->
            item(key = "section_${section.type.name}") {
                SectionCard(
                    type = section.type,
                    people = section.people,
                    onPersonNameChanged = onPersonNameChanged,
                    onPersonGenderChanged = onPersonGenderChanged,
                    onPersonStatusChanged = onPersonStatusChanged,
                    onPersonAdded = onPersonAdded,
                    onPersonRemoved = onPersonRemoved,
                )
            }
        }
    }

    if (isGoogleSheetVisible) {
        GoogleEmailAuthBottomSheet(
            onDismiss = { isGoogleSheetVisible = false },
            onAuthorized = { email ->
                onGoogleAccountSelected(email)
                isGoogleSheetVisible = false
            }
        )
    }
}

@Composable
private fun SectionCard(
    type: PersonalSectionType,
    people: List<PersonalPerson>,
    onPersonNameChanged: (PersonalSectionType, Int, String) -> Unit,
    onPersonGenderChanged: (PersonalSectionType, Int, Int) -> Unit,
    onPersonStatusChanged: (PersonalSectionType, Int, Int) -> Unit,
    onPersonAdded: (PersonalSectionType) -> Unit,
    onPersonRemoved: (PersonalSectionType, Int) -> Unit,
) {
    val statuses = statusList(type.isDead)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = type.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            people.forEachIndexed { index, person ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = if (person.name == DEFAULT_PERSON_NAME) "" else person.name,
                            onValueChange = { onPersonNameChanged(type, index, it) },
                            singleLine = true,
                            label = { Text("Имя") },
                            placeholder = { Text(DEFAULT_PERSON_NAME) },
                        )
                        GenderToggle(
                            isMale = person.gender == 1,
                            onSelectMale = { onPersonGenderChanged(type, index, 1) },
                            onSelectFemale = { onPersonGenderChanged(type, index, 0) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val next = if (person.status <= 0) statuses.lastIndex else person.status - 1
                                        onPersonStatusChanged(type, index, next)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) { Text("<", style = MaterialTheme.typography.labelLarge) }
                                Text(
                                    text = "Статус: ${statuses.getOrElse(person.status) { "" }}",
                                    modifier = Modifier,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                OutlinedButton(
                                    onClick = {
                                        val next = if (person.status >= statuses.lastIndex) 0 else person.status + 1
                                        onPersonStatusChanged(type, index, next)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) { Text(">", style = MaterialTheme.typography.labelLarge) }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (people.size > 1) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 1.dp
                                    ) {
                                        IconButton(
                                            onClick = { onPersonRemoved(type, index) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Удалить запись",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                val hasName = person.name.trim().isNotBlank() && person.name != DEFAULT_PERSON_NAME
                                if (index == people.lastIndex && hasName) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 1.dp
                                    ) {
                                        IconButton(
                                            onClick = { onPersonAdded(type) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Добавить запись",
                                                modifier = Modifier.size(16.dp)
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
    }
}

@Composable
private fun CompactActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun GenderToggle(
    isMale: Boolean,
    onSelectMale: () -> Unit,
    onSelectFemale: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerShape = RoundedCornerShape(10.dp)
    val thumbShape = RoundedCornerShape(8.dp)
    Surface(
        modifier = modifier.height(36.dp),
        shape = containerShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp)
        ) {
            val segmentWidth = maxWidth / 2
            val thumbOffset = animateDpAsState(
                targetValue = if (isMale) 0.dp else segmentWidth,
                animationSpec = spring(stiffness = 500f),
                label = "gender_thumb_offset"
            )
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset.value)
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .shadow(elevation = 1.dp, shape = thumbShape)
                    .clip(thumbShape)
                    .background(MaterialTheme.colorScheme.surface)
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxHeight()
                        .clickable { onSelectMale() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Мужской",
                        color = if (isMale) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxHeight()
                        .clickable { onSelectFemale() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Женский",
                        color = if (!isMale) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}
