package com.cognaque.sequence.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cognaque.sequence.data.AppColors
import com.cognaque.sequence.data.AppStrings
import com.cognaque.sequence.data.EisenhowerQuadrant
import com.cognaque.sequence.data.Task
import com.cognaque.sequence.ui.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSheet(onDismiss: () -> Unit, viewModel: TaskViewModel, onTriggerReeval: (Task) -> Unit) {
    val clipboard = LocalClipboardManager.current
    val dailyTemplates by viewModel.dailyChoreTemplates.collectAsState()
    val secondaryTasks by viewModel.secondaryTasks.collectAsState()
    val windDown by viewModel.windDownHour.collectAsState()
    var newChore by remember { mutableStateOf("") }
    var showNuke by remember { mutableStateOf(false) }
    var vaultTaskDetail by remember { mutableStateOf<Task?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppColors.Surface) {
        LazyColumn(Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
            item {
                Text(AppStrings.DAILY_CHORE_PROTOCOL, fontWeight = FontWeight.Black, fontSize = 12.sp, color = AppColors.Tertiary)
                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    OutlinedTextField(value = newChore, onValueChange = { newChore = it }, modifier = Modifier.weight(1f), singleLine = true)
                    IconButton(onClick = { viewModel.addDailyChore(newChore); newChore = "" }) { Icon(Icons.Default.Add, null) }
                }
            }
            items(dailyTemplates) { chore ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(chore.text, color = Color.White)
                    IconButton(onClick = { viewModel.deleteDailyChore(chore) }) { Icon(Icons.Default.Delete, null, tint = AppColors.Error) }
                }
            }
            listOf(EisenhowerQuadrant.SCHEDULE, EisenhowerQuadrant.DELEGATE, EisenhowerQuadrant.LATER).forEach { quad ->
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(quad.name, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AppColors.TextSecondary)
                    Spacer(Modifier.height(8.dp))
                }
                val tasks = secondaryTasks[quad] ?: emptyList()
                if (tasks.isEmpty()) item { Text("Empty", color = Color.Gray, fontSize = 12.sp) }
                else {
                    items(tasks, key = { it.id }) { task ->
                        val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = {
                            when (it) {
                                SwipeToDismissBoxValue.StartToEnd -> { viewModel.promoteTask(task); true }
                                SwipeToDismissBoxValue.EndToStart -> { viewModel.deleteTask(task); true }
                                else -> false
                            }
                        })
                        SwipeToDismissBox(state = dismissState, backgroundContent = { VaultSwipeBackground(dismissState) }) {
                            Surface(color = AppColors.Surface, modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth().clickable { vaultTaskDetail = task }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(6.dp).background(AppColors.Secondary, CircleShape))
                                    Spacer(Modifier.width(12.dp))
                                    Text(task.rawText, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(24.dp))
                Text("SETTINGS", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Gray)
                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Bed Time", fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        Text("Hide hard tasks after ${windDown}:00", fontSize = 12.sp, color = AppColors.TextSecondary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.setWindDownHour(windDown - 1) }) { Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColors.Primary) }
                        Text("$windDown:00", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp), color = AppColors.TextPrimary)
                        IconButton(onClick = { viewModel.setWindDownHour(windDown + 1) }) { Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColors.Primary) }
                    }
                }
            }
            item {
                Spacer(Modifier.height(24.dp))
                Text(AppStrings.DATA_SOVEREIGNTY, fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Gray)
                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.generateExport() }, Modifier.weight(1f)) { Text("Export") }
                    OutlinedButton(onClick = { clipboard.getText()?.text?.let { viewModel.importData(it) } }, Modifier.weight(1f)) { Text("Import") }
                }
                Button(onClick = { showNuke = true }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error), modifier = Modifier.fillMaxWidth()) { Text("DELETE DATA") }
            }
            item { Spacer(Modifier.height(48.dp)) }
        }
        if (vaultTaskDetail != null) {
            TaskDetailsDialog(task = vaultTaskDetail!!, onDismiss = { vaultTaskDetail = null }, onSaveNotes = { notes -> viewModel.saveNotes(vaultTaskDetail!!, notes) }, onReevaluate = { onTriggerReeval(vaultTaskDetail!!); vaultTaskDetail = null; onDismiss() })
        }
    }
    if (showNuke) AlertDialog(onDismissRequest = { showNuke = false }, title = { Text(AppStrings.DELETE_ALL_TITLE) }, text = { Text(AppStrings.DELETE_ALL_BODY) }, confirmButton = { Button(onClick = { viewModel.nukeAllData(); showNuke = false }) { Text(AppStrings.DELETE_CONFIRM) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSwipeBackground(dismissState: SwipeToDismissBoxState) {
    val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) AppColors.Primary else AppColors.Error
    val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
    val icon = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Icons.Default.ArrowUpward else Icons.Default.Delete
    Box(Modifier.fillMaxSize().background(color).padding(horizontal = 24.dp), contentAlignment = alignment) { Icon(icon, null, tint = Color.White) }
}
