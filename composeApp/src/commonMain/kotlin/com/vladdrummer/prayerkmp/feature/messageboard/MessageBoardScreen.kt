package com.vladdrummer.prayerkmp.feature.messageboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import com.vladdrummer.prayerkmp.feature.messageboard.view_model.MessageBoardMessageUi
import com.vladdrummer.prayerkmp.feature.messageboard.view_model.MessageBoardViewState
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.cs_icon
import kotlinproject.composeapp.generated.resources.legacy_prayer_app
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.Image

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MessageBoardScreen(
    viewState: MessageBoardViewState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onVote: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onPost: (text: String, name: String) -> Unit,
    onEdit: (id: Int, text: String, name: String) -> Unit,
) {
    var messageDialogState by remember { mutableStateOf<MessageDialogState?>(null) }
    var deleteCandidate by remember { mutableStateOf<MessageBoardMessageUi?>(null) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = viewState.isLoading,
        onRefresh = onRefresh
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!viewState.errorMessage.isNullOrBlank()) {
                    Text(
                        text = viewState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (viewState.isLoading && viewState.messages.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
                    ) {
                        items(viewState.messages, key = { it.id }) { item ->
                            val canManage = item.gmail == viewState.currentUserEmail
                            MessageBoardCard(
                                item = item,
                                canManage = canManage,
                                isSubmitting = viewState.isSubmitting,
                                onVote = onVote,
                                onEdit = {
                                    messageDialogState = MessageDialogState.Edit(
                                        id = item.id,
                                        name = item.name,
                                        message = item.message
                                    )
                                },
                                onDelete = { deleteCandidate = item }
                            )
                        }
                    }
                }
            }
        FloatingActionButton(
            onClick = {
                messageDialogState = MessageDialogState.Create(
                    name = viewState.suggestedName,
                    message = ""
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 12.dp, end = 6.dp)
        ) {
            Icon(Icons.Filled.Edit, contentDescription = "Добавить просьбу")
        }
        PullRefreshIndicator(
            refreshing = viewState.isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    }

    val currentDialog = messageDialogState
    if (currentDialog != null) {
        MessageDialog(
            title = if (currentDialog is MessageDialogState.Edit) "Редактирование просьбы" else "Новая просьба",
            initialName = currentDialog.name,
            initialMessage = currentDialog.message,
            onDismiss = { messageDialogState = null },
            onConfirm = { name, message ->
                when (currentDialog) {
                    is MessageDialogState.Create -> onPost(message, name)
                    is MessageDialogState.Edit -> onEdit(currentDialog.id, message, name)
                }
                messageDialogState = null
            }
        )
    }

    val deleting = deleteCandidate
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Удаление просьбы") },
            text = { Text("Вы действительно хотите удалить свою просьбу?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove(deleting.id)
                        deleteCandidate = null
                    }
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun MessageBoardCard(
    item: MessageBoardMessageUi,
    canManage: Boolean,
    isSubmitting: Boolean,
    onVote: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (canManage) {
                    IconButton(onClick = onEdit, enabled = !isSubmitting) {
                        Icon(Icons.Filled.Edit, contentDescription = "Редактировать")
                    }
                    IconButton(onClick = onDelete, enabled = !isSubmitting) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Удалить")
                    }
                }
            }

            ExpandableMessageText(
                text = item.message,
                collapsedLines = 5
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                val sourceIcon = if (item.senderType == 1) {
                    painterResource(Res.drawable.legacy_prayer_app)
                } else {
                    painterResource(Res.drawable.cs_icon)
                }
                Image(
                    painter = sourceIcon,
                    contentDescription = "Источник",
                    modifier = Modifier.size(15.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onVote(item.id) },
                    enabled = !isSubmitting,
                ) {
                    Text("Я за вас помолился")
                }
                Text(
                    text = item.votes.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpandableMessageText(
    text: String,
    collapsedLines: Int,
) {
    var expanded by remember { mutableStateOf(false) }
    var hasOverflow by remember(text) { mutableStateOf(false) }

    Column(modifier = Modifier.animateContentSize()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (!expanded) hasOverflow = result.hasVisualOverflow || result.lineCount > collapsedLines
            }
        )
        if (hasOverflow || expanded) {
            Surface(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .align(Alignment.CenterHorizontally)
                    .clickable { expanded = !expanded },
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (expanded) "Свернуть" else "Развернуть",
                        style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageDialog(
    title: String,
    initialName: String,
    initialMessage: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, message: String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var message by remember(initialMessage) { mutableStateOf(initialMessage) }
    var submitAttempted by remember { mutableStateOf(false) }
    LaunchedEffect(initialName) {
        if (name.isBlank() && initialName.isNotBlank()) name = initialName
    }
    val isNameValid = name.trim().length >= 2
    val isMessageValid = message.trim().length >= 6
    val isFormValid = isNameValid && isMessageValid

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Закрыть",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Имя") },
                    isError = submitAttempted && !isNameValid,
                    supportingText = {
                        if (submitAttempted && !isNameValid) {
                            Text("Слишком короткое")
                        }
                    }
                )
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth(),
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Текст просьбы") },
                    isError = submitAttempted && !isMessageValid,
                    supportingText = {
                        if (submitAttempted && !isMessageValid) {
                            Text("Слишком короткое")
                        }
                    },
                    minLines = 4,
                    maxLines = 7
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    OutlinedButton(
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(),
                        onClick = {
                            submitAttempted = true
                            if (isFormValid) {
                                onConfirm(name.trim(), message.trim())
                            }
                        },
                    ) { Text("Отправить") }
                }
            }
        }
    }
}

private sealed interface MessageDialogState {
    val name: String
    val message: String

    data class Create(
        override val name: String,
        override val message: String,
    ) : MessageDialogState

    data class Edit(
        val id: Int,
        override val name: String,
        override val message: String,
    ) : MessageDialogState
}
