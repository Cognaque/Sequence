@file:OptIn(ExperimentalMaterial3Api::class)

package com.cognaque.sequence.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cognaque.sequence.data.AppColors
import com.cognaque.sequence.data.AppConstants
import com.cognaque.sequence.data.AppStrings
import com.cognaque.sequence.data.Task
import com.cognaque.sequence.data.calculateImpactScore
import com.cognaque.sequence.data.calculateMomentumScore
import com.cognaque.sequence.data.getAgeInDays
import com.cognaque.sequence.ui.TaskViewModel
import java.time.LocalTime
import kotlin.math.abs

@Composable
fun MainScreen(viewModel: TaskViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val displayTasks by viewModel.displayTasks.collectAsState()
    val activeClarification by viewModel.activeClarification.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    var showExportDialog by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showTaskDetails by remember { mutableStateOf<Task?>(null) }
    var showManualReeval by remember { mutableStateOf<Task?>(null) }
    var showSubtaskInputFor by remember { mutableStateOf<Task?>(null) }
    var subtaskText by remember { mutableStateOf("") }
    var draggingItem by remember { mutableStateOf<Task?>(null) }

    LaunchedEffect(true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is TaskViewModel.UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message, withDismissAction = true)
                is TaskViewModel.UiEvent.ShowExport -> showExportDialog = event.json
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { AppHeader(viewModel, onTriggerReeval = { showManualReeval = it }) },
        bottomBar = { if (activeClarification == null) TaskInput(viewModel::onAddTask, isLoading) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                Modifier.padding(horizontal = 24.dp).fillMaxSize().testTag("TaskList")
            ) {
                item { HeaderTitle(AppStrings.FOCUS_STACK, false) }
                if (displayTasks.isEmpty()) {
                    item { EmptyState() }
                } else {
                    itemsIndexed(displayTasks, key = { _, item -> item.task.id }) { index, item ->
                        val task = item.task
                        val indent = (item.indentLevel * 16).dp
                        val currentIndex by rememberUpdatedState(index)

                        val isDragging = draggingItem?.id == task.id
                        val alpha by animateFloatAsState(if (isDragging) 0.5f else 1f, label = "alpha")

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                when (it) {
                                    SwipeToDismissBoxValue.StartToEnd -> { viewModel.markDone(task); true }
                                    SwipeToDismissBoxValue.EndToStart -> { showSubtaskInputFor = task; false }
                                    else -> false
                                }
                            },
                            positionalThreshold = { it * 0.75f }
                        )

                        var dragOffset by remember { mutableFloatStateOf(0f) }

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = { PrioritySwipeBackground(dismissState) },
                            enableDismissFromEndToStart = true,
                            enableDismissFromStartToEnd = true,
                            modifier = Modifier.graphicsLayer { this.alpha = alpha }
                        ) {
                            val dragModifier = if (item.indentLevel > 0) {
                                Modifier.pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            draggingItem = task
                                            viewModel.onDragStart()
                                        },
                                        onDragEnd = {
                                            draggingItem = null
                                            dragOffset = 0f
                                            viewModel.onDragEnd()
                                        },
                                        onDragCancel = {
                                            draggingItem = null
                                            dragOffset = 0f
                                            viewModel.onDragCancel()
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount.y
                                            if (abs(dragOffset) > 100) {
                                                val direction = if (dragOffset > 0) 1 else -1
                                                val targetIndex = currentIndex + direction
                                                if (targetIndex in displayTasks.indices) {
                                                    viewModel.swapItems(currentIndex, targetIndex)
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    dragOffset = 0f
                                                }
                                            }
                                        }
                                    )
                                }
                            } else Modifier

                            TaskRow(
                                task = task,
                                indent = indent,
                                onClick = { showTaskDetails = task },
                                dragModifier = dragModifier
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                item { Spacer(Modifier.height(100.dp)) }
            }

            activeClarification?.let { ClarificationOverlay(it) { i, l, a, p, e -> viewModel.submitClarification(it.id, i, l, a, p, e) } }
            showManualReeval?.let { ClarificationOverlay(it) { i, l, a, p, e -> viewModel.submitClarification(it.id, i, l, a, p, e); showManualReeval = null } }

            showTaskDetails?.let { task ->
                TaskDetailsDialog(task, onDismiss = { showTaskDetails = null }, onSaveNotes = { notes -> viewModel.saveNotes(task, notes) }, onReevaluate = { showTaskDetails = null; showManualReeval = task })
            }

            if (showSubtaskInputFor != null) {
                AlertDialog(
                    onDismissRequest = { showSubtaskInputFor = null; subtaskText = "" },
                    title = { Text("Add Subtask") },
                    text = { OutlinedTextField(value = subtaskText, onValueChange = { subtaskText = it }, placeholder = { Text("Enter subtask...") }, singleLine = true) },
                    confirmButton = { Button(onClick = { if (subtaskText.isNotBlank()) { viewModel.addSubtask(showSubtaskInputFor!!.id, subtaskText); showSubtaskInputFor = null; subtaskText = "" } }) { Text("Add") } }
                )
            }
            if (showExportDialog != null) ExportDialog(showExportDialog!!, { showExportDialog = null }, { clipboardManager.setText(AnnotatedString(it)); showExportDialog = null })
            if (showImportDialog) ImportDialog({ showImportDialog = false }, { viewModel.importData(it); showImportDialog = false }, clipboardManager)
        }
    }
}

@Composable
fun TaskRow(task: Task, indent: Dp, onClick: () -> Unit, dragModifier: Modifier = Modifier) {
    val impact = task.calculateImpactScore()
    val gradient = when {
        task.isDailyChore -> listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.tertiary)
        task.isManuallyPromoted -> listOf(Color(0xFF424242), Color(0xFF616161))
        task.calculateMomentumScore() >= 1.2f -> listOf(Color(0xFF7F0000), Color(0xFFB71C1C))
        impact >= 0.85f -> listOf(Color(0xFFF57C00), Color(0xFFE65100))
        impact >= 0.6f -> listOf(Color(0xFFFF9800), Color(0xFFFFB74D))
        else -> listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
    }

    Row(Modifier.fillMaxWidth().padding(start = indent).then(dragModifier), verticalAlignment = Alignment.CenterVertically) {
        if (indent > 0.dp) { Icon(Icons.Default.SubdirectoryArrowRight, null, tint = Color.Gray, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)) }
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
                        color = Color.White // Keep white on gradient backgrounds
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

@Composable
fun AppHeader(viewModel: TaskViewModel, onTriggerReeval: (Task) -> Unit) {
    val windDown by viewModel.windDownHour.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val isSleep = remember(windDown) {
        val h = LocalTime.now().hour
        if (windDown > 4) (h >= windDown || h < 4) else (h >= windDown && h < 4)
    }
    var showVault by remember { mutableStateOf(false) }

    Column(Modifier.statusBarsPadding().fillMaxWidth()) {
        if (isSleep) Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)).padding(top = 8.dp), contentAlignment = Alignment.Center) { Text(AppStrings.SLEEP_MODE, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary) }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            // Spacer to balance the layout if needed, or just alignment
            Box(Modifier.size(48.dp)) // Placeholder for balance
            Text(AppStrings.PRIORITY_TITLE, style = TextStyle(fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary), modifier = Modifier.pointerInput(Unit) { detectTapGestures(onLongPress = { showVault = true }) })
            IconButton(onClick = { viewModel.toggleTheme() }) {
                Icon(
                    imageVector = if (isDark) Icons.Default.WbSunny else Icons.Default.DarkMode,
                    contentDescription = "Toggle Theme",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
    if (showVault) VaultSheet({ showVault = false }, viewModel, onTriggerReeval)
}

@Composable
fun TaskInput(onAdd: (String) -> Unit, isLoading: Boolean) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Row(Modifier.padding(20.dp).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).background(MaterialTheme.colorScheme.onSurface.copy(0.05f), RoundedCornerShape(24.dp)).clickable { focusRequester.requestFocus() }.padding(16.dp, 12.dp)) {
                BasicTextField(
                    value = text,
                    onValueChange = { if (it.length <= AppConstants.MAX_INPUT_LENGTH) text = it },
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onAdd(text); text = "" }),
                    modifier = Modifier.focusRequester(focusRequester).fillMaxWidth()
                )
                if (text.isEmpty()) Text(AppStrings.DUMP_PLACEHOLDER, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Spacer(Modifier.width(12.dp))
            if (isLoading) CircularProgressIndicator(Modifier.size(24.dp))
            else IconButton(onClick = { onAdd(text); text = "" }) { Icon(Icons.Default.Send, null, tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrioritySwipeBackground(dismissState: SwipeToDismissBoxState) {
    val direction = dismissState.dismissDirection
    val color = if (direction == SwipeToDismissBoxValue.StartToEnd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiaryContainer
    val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
    val icon = if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Check else Icons.Default.Add
    Box(Modifier.fillMaxSize().background(color, RoundedCornerShape(16.dp)).padding(horizontal = 24.dp), contentAlignment = alignment) {
        Icon(icon, null, tint = if (direction == SwipeToDismissBoxValue.EndToStart) Color.Black else Color.White)
    }
}

@Composable fun HeaderTitle(text: String, isHidden: Boolean) { Text(text, color = if (isHidden) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🧠", fontSize = 54.sp)

        Text(
            text = AppStrings.NO_THREATS,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.5.sp
        )
    }
}
