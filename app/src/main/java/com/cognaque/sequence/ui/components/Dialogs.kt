package com.cognaque.sequence.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipboardManager
import com.cognaque.sequence.data.AppStrings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import com.cognaque.sequence.data.AppConstants
import com.cognaque.sequence.data.Task
import com.cognaque.sequence.data.getAgeInDays

@Composable
fun TaskDetailsDialog(task: Task, onDismiss: () -> Unit, onSaveNotes: (String) -> Unit, onReevaluate: () -> Unit) {
    val ctx = LocalContext.current
    var notes by remember(task.id) { mutableStateOf(task.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Task Details") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(task.rawText, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Created ${task.getAgeInDays()} days ago", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it; onSaveNotes(it) },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onReevaluate) { Text("Re-evaluate", color = MaterialTheme.colorScheme.tertiary) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            data = CalendarContract.Events.CONTENT_URI
                            putExtra(CalendarContract.Events.TITLE, task.rawText.take(AppConstants.INTENT_MAX_STR_LEN))
                        }
                        ctx.startActivity(intent)
                    } catch (e: Exception) {
                    }
                }) { Text("Add to Calendar") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun ClarificationOverlay(task: Task, onFinish: (Float, Float, Float, Float, Float) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val vals = remember { mutableStateListOf(task.immediate, task.longTerm, task.proximity, task.accumulation, task.effort) }
    val questions = listOf("Immediate: Consequences if ignored today?", "Long-term: Impact on future goals?", "Proximity: How close is the deadline?", "Accumulation: Does this get worse with time?", "Effort: Activation energy required?")
    val options = if (step == 4) listOf("Trivial (2m)" to 0.1f, "Simple (15m)" to 0.3f, "Focus (1h)" to 0.6f, "Draining (4h+)" to 0.9f) else listOf("Minimal" to 0.1f, "Low" to 0.3f, "Significant" to 0.6f, "Severe" to 0.9f)

    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.8f)).padding(32.dp).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Text("TEACHING THE AI...", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(task.rawText, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text(questions[step], color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            options.forEach { (label, value) ->
                Button(
                    onClick = {
                        vals[step] = value
                        if (step < 4) step++ else onFinish(vals[0], vals[1], vals[3], vals[2], vals[4])
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(label)
                }
            }
        }
    }
}

@Composable fun ExportDialog(json: String, onDismiss: () -> Unit, onCopy: (String) -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text(AppStrings.EXPORT_READY) }, text = { Text(AppStrings.EXPORT_BODY) }, confirmButton = { Button(onClick = { onCopy(json) }) { Text("Copy") } }) }
@Composable fun ImportDialog(onDismiss: () -> Unit, onPaste: (String) -> Unit, cm: ClipboardManager) { AlertDialog(onDismissRequest = onDismiss, title = { Text(AppStrings.IMPORT_TITLE) }, text = { Text(AppStrings.IMPORT_BODY) }, confirmButton = { Button(onClick = { cm.getText()?.text?.let(onPaste) }) { Text("Paste") } }) }
