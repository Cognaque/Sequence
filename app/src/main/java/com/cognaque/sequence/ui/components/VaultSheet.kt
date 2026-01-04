package com.cognaque.sequence.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cognaque.sequence.data.AppStrings
import com.cognaque.sequence.data.EisenhowerQuadrant
import com.cognaque.sequence.data.Task
import com.cognaque.sequence.data.calculateImpactScore
import com.cognaque.sequence.data.calculateMomentumScore
import com.cognaque.sequence.data.getAgeInDays
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

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        LazyColumn(Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
            item {
                Text(AppStrings.DAILY_CHORE_PROTOCOL, fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    OutlinedTextField(value = newChore, onValueChange = { newChore = it }, modifier = Modifier.weight(1f), singleLine = true)
                    IconButton(onClick = { viewModel.addDailyChore(newChore); newChore = "" }) { Icon(Icons.Default.Add, null) }
                }
            }
            items(dailyTemplates) { chore ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(chore.text, color = MaterialTheme.colorScheme.onSurface)
                    IconButton(onClick = { viewModel.deleteDailyChore(chore) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                }
            }
            listOf(EisenhowerQuadrant.SCHEDULE, EisenhowerQuadrant.DELEGATE, EisenhowerQuadrant.LATER).forEach { quad ->
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(quad.name, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                }
                val tasks = secondaryTasks[quad] ?: emptyList()
                if (tasks.isEmpty()) item { Text("Empty", color = Color.Gray, fontSize = 12.sp) }
                else {
                    items(tasks, key = { it.id }) { task ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                when (it) {
                                    SwipeToDismissBoxValue.StartToEnd -> { viewModel.promoteTask(task); true }
                                    SwipeToDismissBoxValue.EndToStart -> { viewModel.deleteTask(task); true }
                                    else -> false
                                }
                            },
                            positionalThreshold = { it * 0.75f }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = { VaultSwipeBackground(dismissState) },
                            enableDismissFromEndToStart = true,
                            enableDismissFromStartToEnd = true
                        ) {
                            VaultTaskRow(
                                task = task,
                                onClick = { vaultTaskDetail = task }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            item {
                Spacer(Modifier.height(24.dp))
                Text("SETTINGS", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Bed Time", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Hide hard tasks after ${windDown}:00", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.setWindDownHour(windDown - 1) }) { Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                        Text("$windDown:00", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = { viewModel.setWindDownHour(windDown + 1) }) { Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
            item {
                Spacer(Modifier.height(24.dp))
                Text(AppStrings.DATA_SOVEREIGNTY, fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.generateExport() }, Modifier.weight(1f)) { Text("Export") }
                    OutlinedButton(onClick = { clipboard.getText()?.text?.let { viewModel.importData(it) } }, Modifier.weight(1f)) { Text("Import") }
                }
                Button(onClick = { showNuke = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth()) { Text("DELETE DATA") }
            }
            item { Spacer(Modifier.height(48.dp)) }
        }
        if (vaultTaskDetail != null) {
            TaskDetailsDialog(task = vaultTaskDetail!!, onDismiss = { vaultTaskDetail = null }, onSaveNotes = { notes -> viewModel.saveNotes(vaultTaskDetail!!, notes) }, onReevaluate = { onTriggerReeval(vaultTaskDetail!!); vaultTaskDetail = null; onDismiss() })
        }
    }
    if (showNuke) AlertDialog(onDismissRequest = { showNuke = false }, title = { Text(AppStrings.DELETE_ALL_TITLE) }, text = { Text(AppStrings.DELETE_ALL_BODY) }, confirmButton = { Button(onClick = { viewModel.nukeAllData(); showNuke = false }) { Text(AppStrings.DELETE_CONFIRM) } })
}

@Composable
fun VaultTaskRow(task: Task, onClick: () -> Unit) {
    val impact = task.calculateImpactScore()
    val gradient = when {
        task.isDailyChore -> listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.tertiary)
        task.isManuallyPromoted -> listOf(Color(0xFF424242), Color(0xFF616161))
        task.calculateMomentumScore() >= 1.2f -> listOf(Color(0xFF7F0000), Color(0xFFB71C1C))
        impact >= 0.85f -> listOf(Color(0xFFF57C00), Color(0xFFE65100))
        impact >= 0.6f -> listOf(Color(0xFFFF9800), Color(0xFFFFB74D))
        else -> listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = Color.Transparent, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
            Row(
                Modifier
                    .background(Brush.horizontalGradient(gradient))
                    .clickable { onClick() }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.rawText,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (task.isDailyChore) {
                            Text("Daily chore added by you", color = Color.Black.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            val age = task.getAgeInDays()
                            if (age > 0) {
                                Text("• ${age}d Old", color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            if (task.isManuallyPromoted) {
                                if (age > 0) Spacer(Modifier.width(8.dp))
                                Text("Added by you", color = Color.Black.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSwipeBackground(dismissState: SwipeToDismissBoxState) {
    val direction = dismissState.dismissDirection
    val color = if (direction == SwipeToDismissBoxValue.StartToEnd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
    val icon = if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.ArrowUpward else Icons.Default.Delete
    Box(Modifier.fillMaxSize().background(color, RoundedCornerShape(16.dp)).padding(horizontal = 24.dp), contentAlignment = alignment) {
        Icon(icon, null, tint = Color.White)
    }
}
