package com.cognaque.sequence

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

/**
 * ADHD Consequence Engine - LEARNING AI VERSION + DAILY CHORES
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.setBackgroundColor(android.graphics.Color.BLACK)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val context = LocalContext.current
            val viewModel: TaskViewModel = viewModel(
                factory = TaskViewModelFactory(context.applicationContext)
            )
            val colorScheme = darkColorScheme(
                primary = Color(0xFF81C784),
                secondary = Color(0xFF64B5F6),
                surface = Color(0xFF1E1E1E),
                background = Color(0xFF121212),
                tertiary = Color(0xFFF48FB1) // Pink for Daily Chores
            )
            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AdhdTaskSystemApp(viewModel)
                }
            }
        }
    }
}


@Composable
fun BrainAppIcon(size: Dp = 48.dp, animate: Boolean = true) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon_anim")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    val scale = if (animate) pulse else 1f

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.toPx() * scale
            val height = size.toPx() * scale
            val center = Offset(size.toPx() / 2, size.toPx() / 2)

            val brainPath = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(
                    center.x - width * 0.45f, center.y - height * 0.35f,
                    center.x + width * 0.05f, center.y + height * 0.35f
                ))
                addOval(androidx.compose.ui.geometry.Rect(
                    center.x - width * 0.05f, center.y - height * 0.35f,
                    center.x + width * 0.45f, center.y + height * 0.35f
                ))
            }

            drawPath(
                path = brainPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF81C784), Color(0xFF43A047)),
                    center = center
                )
            )

            val boltPath = Path().apply {
                moveTo(center.x + width * 0.1f, center.y - height * 0.25f)
                lineTo(center.x - width * 0.15f, center.y + height * 0.05f)
                lineTo(center.x + width * 0.05f, center.y + height * 0.05f)
                lineTo(center.x - width * 0.1f, center.y + height * 0.3f)
            }

            drawPath(
                path = boltPath,
                color = Color.White,
                style = Stroke(width = 4f)
            )
        }
    }
}

// ==========================================
// 🧠 DATABASE LAYER (ROOM)
// ==========================================

enum class EisenhowerQuadrant {
    PRIORITY, SCHEDULE, DELEGATE, LATER
}

@Entity(tableName = "keyword_weights")
data class KeywordWeight(
    @PrimaryKey val keyword: String,
    val count: Int,
    val avgImm: Float,
    val avgLt: Float,
    val avgProx: Float,
    val avgAcc: Float,
    val avgEff: Float
)

// Daily Chore Templates
@Entity(tableName = "daily_chores")
data class DailyChore(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val text: String
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val rawText: String,
    val normalizedSignature: String = "",
    val immediate: Float = 0f,
    val longTerm: Float = 0f,
    val proximity: Float = 0f,
    val accumulation: Float = 0f,
    val effort: Float = 0f,
    val isDone: Boolean = false,
    val isAiGenerated: Boolean = false,
    val needsClarification: Boolean = false,
    val needsReEvaluation: Boolean = false,
    val isManuallyPromoted: Boolean = false,
    val isDailyChore: Boolean = false, // NEW FIELD
    val entryCount: Int = 1,
    val creationTimestamp: Long = System.currentTimeMillis()
) {
    @Ignore
    val ageInDays: Int = ((System.currentTimeMillis() - creationTimestamp) / (1000 * 60 * 60 * 24)).toInt()

    @Ignore
    val impactScore: Float = run {
        val ageEscalation = (ageInDays * 0.05f).coerceAtMost(0.5f)
        val effectiveProximity = (proximity + ageEscalation).coerceAtMost(1.0f)
        (immediate * 0.4f) + (longTerm * 0.3f) + (effectiveProximity * 0.2f) + (accumulation * 0.1f)
    }

    @Ignore
    val momentumScore: Float = impactScore * (1.5f - (effort * 0.5f))

    @Ignore val urgency: Float = (immediate + proximity) / 2f
    @Ignore val importance: Float = (longTerm + accumulation) / 2f

    @Ignore val quadrant: EisenhowerQuadrant = when {
        isDailyChore -> EisenhowerQuadrant.PRIORITY // Daily chores always prioritized visually
        isManuallyPromoted -> EisenhowerQuadrant.PRIORITY
        impactScore >= 0.5f -> EisenhowerQuadrant.PRIORITY
        urgency < 0.5f && importance >= 0.5f -> EisenhowerQuadrant.SCHEDULE
        urgency >= 0.5f && importance < 0.5f -> EisenhowerQuadrant.DELEGATE
        else -> EisenhowerQuadrant.LATER
    }
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isDone = 0")
    fun getActiveTasksFlow(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): Task?

    @Query("SELECT * FROM tasks WHERE normalizedSignature = :sig AND isDone = 0 LIMIT 1")
    suspend fun findActiveTaskBySignature(sig: String): Task?

    // --- Daily Chore Methods ---

    @Query("SELECT * FROM daily_chores")
    fun getDailyChoreTemplates(): Flow<List<DailyChore>>

    @Query("SELECT * FROM daily_chores")
    suspend fun getDailyChoreTemplatesOneShot(): List<DailyChore>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyChoreTemplate(chore: DailyChore)

    @Delete
    suspend fun deleteDailyChoreTemplate(chore: DailyChore)

    // Check if a task was created TODAY for this text (Active or Done)
    @Query("SELECT COUNT(*) FROM tasks WHERE isDailyChore = 1 AND rawText = :text AND creationTimestamp >= :startOfDay")
    suspend fun hasDailyChoreForToday(text: String, startOfDay: Long): Int
}

@Dao
interface LearningDao {
    @Query("SELECT * FROM keyword_weights WHERE keyword = :keyword")
    suspend fun getWeight(keyword: String): KeywordWeight?

    @Query("SELECT * FROM keyword_weights ORDER BY count DESC")
    fun getAllWeights(): Flow<List<KeywordWeight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWeight(weight: KeywordWeight)
}

// Updated Version to 7
@Database(entities = [Task::class, KeywordWeight::class, DailyChore::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun learningDao(): LearningDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "adhd_task_db_v7"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==========================================
// 🤖 LEARNING ENGINE
// ==========================================

data class PredictionResult(
    val imm: Float,
    val lt: Float,
    val prox: Float,
    val acc: Float,
    val eff: Float,
    val confidence: Boolean
)

class LearningEngine(private val learningDao: LearningDao) {
    private val noiseWords = setOf("my", "the", "on", "a", "an", "at", "for", "with", "me", "to", "of", "in", "and", "is", "it","be", "that", "have", "I", "not", "he", "as", "you", "do", "this", "but", "his", "by", "from", "they", "we", "say", "her", "she", "or", "will", "one", "all", "would", "there", "their", "are", "was", "were", "been", "being", "him", "us", "them", "if")

    private fun tokenize(text: String): List<String> {
        val words = text.lowercase()
            .split(Regex("[^a-z]+"))
            .filter { it.length > 2 && it !in noiseWords }
        val bigrams = words.zipWithNext { a, b -> "$a $b" }
        return words + bigrams
    }

    suspend fun learnFromTask(text: String, imm: Float, lt: Float, prox: Float, acc: Float, eff: Float) {
        val tokens = tokenize(text)
        tokens.forEach { token ->
            val existing = learningDao.getWeight(token)
            val newWeight = if (existing == null) {
                KeywordWeight(token, 1, imm, lt, prox, acc, eff)
            } else {
                val n = existing.count + 1
                KeywordWeight(
                    token, n,
                    ((existing.avgImm * existing.count) + imm) / n,
                    ((existing.avgLt * existing.count) + lt) / n,
                    ((existing.avgProx * existing.count) + prox) / n,
                    ((existing.avgAcc * existing.count) + acc) / n,
                    ((existing.avgEff * existing.count) + eff) / n
                )
            }
            learningDao.saveWeight(newWeight)
        }
    }

    suspend fun predict(text: String): PredictionResult {
        val tokens = tokenize(text)
        if (tokens.isEmpty()) return PredictionResult(0f,0f,0f,0f,0f, false)

        var sumImm = 0f; var sumLt = 0f; var sumProx = 0f; var sumAcc = 0f; var sumEff = 0f
        var totalWeight = 0
        var maxConfidence = 0

        tokens.forEach { token ->
            val w = learningDao.getWeight(token)
            if (w != null) {
                val weight = w.count.coerceAtMost(10)
                sumImm += w.avgImm * weight
                sumLt += w.avgLt * weight
                sumProx += w.avgProx * weight
                sumAcc += w.avgAcc * weight
                sumEff += w.avgEff * weight
                totalWeight += weight
                maxConfidence = maxOf(maxConfidence, w.count)
            }
        }

        if (totalWeight < 3) return PredictionResult(0f,0f,0f,0f,0f, false)

        return PredictionResult(
            sumImm / totalWeight,
            sumLt / totalWeight,
            sumProx / totalWeight,
            sumAcc / totalWeight,
            sumEff / totalWeight,
            true
        )
    }

    fun generateNormalizedSignature(text: String): String {
        return tokenize(text).sorted().joinToString(" ")
    }
}

// ==========================================
// 📱 VIEWMODEL
// ==========================================

class TaskViewModel(context: Context) : ViewModel() {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.taskDao()
    private val aiEngine = LearningEngine(db.learningDao())
    private val prefs = context.getSharedPreferences("adhd_prefs", Context.MODE_PRIVATE)

    private val _windDownHour = MutableStateFlow(prefs.getInt("wind_down_hour", 22))
    val windDownHour = _windDownHour.asStateFlow()

    val allActiveTasks = dao.getActiveTasksFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val keywordWeights = db.learningDao().getAllWeights().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val dailyChoreTemplates = dao.getDailyChoreTemplates().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val priorityTasks = combine(allActiveTasks, _windDownHour) { list, windDown ->
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val isSleepMode = currentHour >= windDown || currentHour < 4

        list.filter { task ->
            // Daily Chores are always priority
            if (task.isDailyChore) return@filter true

            val isPriority = task.quadrant == EisenhowerQuadrant.PRIORITY && !task.needsClarification
            if (isSleepMode && isPriority) {
                task.effort <= 0.4f
            } else {
                isPriority
            }
        }
            .sortedWith(compareByDescending<Task> { it.isDailyChore }.thenByDescending { it.momentumScore })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val secondaryTasks = allActiveTasks.map { list ->
        list.filter { it.quadrant != EisenhowerQuadrant.PRIORITY && !it.needsClarification && !it.isDailyChore }
            .groupBy { it.quadrant }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val activeClarification = allActiveTasks.map { it.find { t -> t.needsClarification } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        checkAndGenerateDailyChores()
    }

    // --- Daily Chore Logic ---
    private fun checkAndGenerateDailyChores() {
        viewModelScope.launch {
            val startOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val templates = dao.getDailyChoreTemplatesOneShot()
            templates.forEach { template ->
                // Check if we already created this chore today (completed or active)
                val count = dao.hasDailyChoreForToday(template.text, startOfDay)
                if (count == 0) {
                    dao.insertTask(Task(
                        rawText = template.text,
                        isDailyChore = true,
                        immediate = 0.8f, // High urgency
                        longTerm = 0.8f,  // High importance
                        proximity = 1.0f,
                        accumulation = 0.5f,
                        effort = 0.3f, // Default moderate effort
                        normalizedSignature = "DAILY_${template.id}"
                    ))
                }
            }
        }
    }

    fun addDailyChoreTemplate(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            dao.insertDailyChoreTemplate(DailyChore(text = text.trim()))
            // Trigger generation immediately so it appears now
            checkAndGenerateDailyChores()
        }
    }

    fun deleteDailyChoreTemplate(chore: DailyChore) {
        viewModelScope.launch {
            dao.deleteDailyChoreTemplate(chore)
        }
    }

    // --- Standard Task Logic ---
    fun setWindDownHour(hour: Int) {
        val clamped = hour.coerceIn(0, 23)
        _windDownHour.value = clamped
        prefs.edit().putInt("wind_down_hour", clamped).apply()
    }

    fun onAddTask(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val signature = aiEngine.generateNormalizedSignature(trimmed)
            val existing = dao.findActiveTaskBySignature(signature)

            if (existing != null) {
                dao.updateTask(existing.copy(
                    entryCount = existing.entryCount + 1,
                    needsReEvaluation = existing.entryCount >= 3,
                    accumulation = (existing.accumulation + 0.15f).coerceAtMost(1.0f)
                ))
            } else {
                val p = aiEngine.predict(trimmed)
                if (p.confidence) {
                    dao.insertTask(Task(
                        rawText = trimmed,
                        normalizedSignature = signature,
                        immediate = p.imm, longTerm = p.lt, proximity = p.prox, accumulation = p.acc, effort = p.eff,
                        isAiGenerated = true
                    ))
                } else {
                    dao.insertTask(Task(rawText = trimmed, normalizedSignature = signature, needsClarification = true))
                }
            }
        }
    }

    fun submitClarification(taskId: String, immediate: Float, longTerm: Float, accumulation: Float, proximity: Float, effort: Float) {
        viewModelScope.launch {
            val task = dao.getTaskById(taskId) ?: return@launch
            aiEngine.learnFromTask(task.rawText, immediate, longTerm, proximity, accumulation, effort)
            dao.updateTask(task.copy(
                immediate = immediate, longTerm = longTerm, accumulation = accumulation, proximity = proximity, effort = effort,
                isAiGenerated = false, needsClarification = false, needsReEvaluation = false
            ))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { dao.deleteTask(task) }
    }

    fun promoteTask(task: Task) {
        viewModelScope.launch { dao.updateTask(task.copy(isManuallyPromoted = true)) }
    }

    fun markDone(task: Task) {
        viewModelScope.launch { dao.updateTask(task.copy(isDone = true)) }
    }
}

class TaskViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaskViewModel(context) as T
    }
}

// ==========================================
// 🎨 UI COMPONENTS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhdTaskSystemApp(viewModel: TaskViewModel) {
    val priorityTasks by viewModel.priorityTasks.collectAsState()
    val secondaryTasks by viewModel.secondaryTasks.collectAsState()
    val activeClarification by viewModel.activeClarification.collectAsState()
    val keywordWeights by viewModel.keywordWeights.collectAsState()
    val dailyTemplates by viewModel.dailyChoreTemplates.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showVault by remember { mutableStateOf(false) }
    var showInfoDialogFor by remember { mutableStateOf<Task?>(null) }
    var showCorrectionOverlayFor by remember { mutableStateOf<Task?>(null) }
    var showAiMemory by remember { mutableStateOf(false) }

    // Vault Input
    var newDailyChoreText by remember { mutableStateOf("") }

    val context = LocalContext.current
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = textColor.copy(alpha = 0.6f)

    var isPressingBrand by remember { mutableStateOf(false) }
    LaunchedEffect(isPressingBrand) {
        if (isPressingBrand) {
            delay(3000) // 3 seconds hold to open vault
            showVault = true
            isPressingBrand = false
        }
    }

    if (showAiMemory) {
        AlertDialog(
            onDismissRequest = { showAiMemory = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text("AI Brain (Learned Weights)")
                }
            },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp).fillMaxWidth()) {
                    if (keywordWeights.isEmpty()) {
                        item { Text("No learning data yet.", color = secondaryTextColor) }
                    }
                    items(keywordWeights) { w ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(w.keyword, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("Cnt: ${w.count}", fontSize = 10.sp, color = secondaryTextColor)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Eff: ${String.format("%.1f", w.avgEff)}", fontSize = 10.sp, color = secondaryTextColor)
                                Text("Imm: ${String.format("%.1f", w.avgImm)}", fontSize = 10.sp, color = secondaryTextColor)
                                Text("Prox: ${String.format("%.1f", w.avgProx)}", fontSize = 10.sp, color = secondaryTextColor)
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAiMemory = false }) { Text("Close") } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    showInfoDialogFor?.let { task ->
        AlertDialog(
            onDismissRequest = { showInfoDialogFor = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Impact Analysis")
                    if (task.isAiGenerated) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.AutoAwesome, "AI Learned", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    }
                    if (task.isDailyChore) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Repeat, "Daily", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                    }
                }
            },
            text = {
                Column {
                    Text(task.rawText, fontWeight = FontWeight.Bold)
                    if (task.isDailyChore) {
                        Text("This task regenerates daily.", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    FactorRow("Immediate (40%)", task.immediate)
                    FactorRow("Long-term (30%)", task.longTerm)
                    FactorRow("Proximity (20%)", task.proximity)
                    FactorRow("Accumulation (10%)", task.accumulation)
                }
            },
            dismissButton = {
                Row {
                    if (!task.isDailyChore) {
                        TextButton(onClick = {
                            showCorrectionOverlayFor = task
                            showInfoDialogFor = null
                        }) {
                            Text("Train AI")
                        }
                    }
                    if (task.quadrant == EisenhowerQuadrant.SCHEDULE || task.quadrant == EisenhowerQuadrant.DELEGATE) {
                        TextButton(onClick = {
                            val intent = Intent(Intent.ACTION_INSERT).apply {
                                data = CalendarContract.Events.CONTENT_URI
                                putTitle(task.rawText)
                                putDescription("Momentum: ${task.momentumScore}")
                            }
                            context.startActivity(intent)
                        }) {
                            Text("Calendar")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showInfoDialogFor = null }) { Text("OK") } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        topBar = {
            Column(Modifier.statusBarsPadding().fillMaxWidth()) {
                val windDown by viewModel.windDownHour.collectAsState()
                val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val isSleepMode = currentHour >= windDown || currentHour < 4

                if (isSleepMode) {
                    Box(Modifier.fillMaxWidth().background(Color(0xFF1E2124).copy(0.5f)).padding(top = 8.dp), contentAlignment = Alignment.Center) {
                        Text("🌙 Sleep Mode Active", fontSize = 12.sp, color = Color(0xFF90CAF9))
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isPressingBrand = true
                                    try { awaitRelease() } finally { isPressingBrand = false }
                                }
                            )
                        }
                    ) {
                        Text(
                            "PRIORITY",
                            style = TextStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                letterSpacing = 2.sp,
                                color = if (isPressingBrand) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (activeClarification == null) {
                BrainDumpInput(
                    value = inputText,
                    onValueChange = { inputText = it },
                    onSend = { viewModel.onAddTask(inputText); inputText = "" }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(Modifier.padding(horizontal = 24.dp).fillMaxSize()) {
                item {
                    val subtitle = if (showVault) "HIDDEN VAULT" else "FOCUS STACK"
                    Text(
                        subtitle,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (showVault) MaterialTheme.colorScheme.secondary else secondaryTextColor,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                }

                if (priorityTasks.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            BrainAppIcon(size = 64.dp, animate = false)
                            Spacer(Modifier.height(16.dp))
                            Text("I am a brain Watson, the rest of me is a mere appendix.", color = secondaryTextColor, fontSize = 12.sp)
                        }
                    }
                }

                items(priorityTasks, key = { it.id }) { task ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.StartToEnd) {
                                viewModel.markDone(task)
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = { SwipeBackground(dismissState) },
                        enableDismissFromEndToStart = false
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TaskRow(task, onRowClick = { showInfoDialogFor = task })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                item { Spacer(Modifier.height(100.dp)) }
            }

            activeClarification?.let { task ->
                ClarificationOverlay(task) { imm, lt, acc, prox, eff ->
                    viewModel.submitClarification(task.id, imm, lt, acc, prox, eff)
                }
            }

            showCorrectionOverlayFor?.let { task ->
                ClarificationOverlay(task) { imm, lt, acc, prox, eff ->
                    viewModel.submitClarification(task.id, imm, lt, acc, prox, eff)
                    showCorrectionOverlayFor = null
                }
            }
        }

        if (showVault) {
            ModalBottomSheet(
                onDismissRequest = { showVault = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                LazyColumn(Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
                    // --- DAILY CHORE MANAGER ---
                    item {
                        Text("DAILY CHORE PROTOCOL", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(vertical = 16.dp))
                        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newDailyChoreText,
                                onValueChange = { newDailyChoreText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Enter daily task...", fontSize = 14.sp) },
                                singleLine = true
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    viewModel.addDailyChoreTemplate(newDailyChoreText)
                                    newDailyChoreText = ""
                                },
                                modifier = Modifier.background(MaterialTheme.colorScheme.tertiary, CircleShape)
                            ) {
                                Icon(Icons.Default.Add, "Add", tint = Color.Black)
                            }
                        }

                        if (dailyTemplates.isNotEmpty()) {
                            Text("Active Daily Routines:", fontSize = 12.sp, color = secondaryTextColor, modifier = Modifier.padding(bottom = 8.dp))
                            dailyTemplates.forEach { template ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp).background(Color.White.copy(0.05f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("↻ ${template.text}", color = Color.White)
                                    IconButton(onClick = { viewModel.deleteDailyChoreTemplate(template) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(0.7f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }

                    // --- SETTINGS ---
                    item {
                        Text("SETTINGS", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                        val windDown by viewModel.windDownHour.collectAsState()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                        ) {
                            Column {
                                Text("Wind Down Time", fontWeight = FontWeight.Bold, color = textColor)
                                Text("Hide hard tasks after ${windDown}:00", fontSize = 12.sp, color = secondaryTextColor)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.setWindDownHour(windDown - 1) }) {
                                    Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Text("$windDown:00", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp), color = textColor)
                                IconButton(onClick = { viewModel.setWindDownHour(windDown + 1) }) {
                                    Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("VAULTED LATENCY", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                            IconButton(onClick = { showAiMemory = true }) {
                                Icon(Icons.Default.Psychology, contentDescription = "View Brain", tint = secondaryTextColor)
                            }
                        }
                    }

                    EisenhowerQuadrant.values().filter { it != EisenhowerQuadrant.PRIORITY }.forEach { quad ->
                        item {
                            Text(quad.name, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = secondaryTextColor)
                            Spacer(Modifier.height(8.dp))
                        }
                        val tasks = secondaryTasks[quad] ?: emptyList()
                        if (tasks.isEmpty()) {
                            item { Text("Clear", color = secondaryTextColor, fontSize = 12.sp); Spacer(Modifier.height(16.dp)) }
                        } else {
                            items(tasks, key = { it.id }) { task ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = {
                                        when (it) {
                                            SwipeToDismissBoxValue.EndToStart -> { viewModel.deleteTask(task); true }
                                            SwipeToDismissBoxValue.StartToEnd -> { viewModel.promoteTask(task); true }
                                            else -> false
                                        }
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = { VaultSwipeBackground(dismissState) },
                                    enableDismissFromStartToEnd = true,
                                    enableDismissFromEndToStart = true
                                ) {
                                    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            Modifier.fillMaxWidth().clickable { showInfoDialogFor = task }.padding(vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if(task.isAiGenerated) {
                                                Icon(Icons.Default.AutoAwesome, null, tint = secondaryTextColor, modifier = Modifier.size(12.dp))
                                                Spacer(Modifier.width(8.dp))
                                            } else {
                                                Box(Modifier.size(3.dp).background(secondaryTextColor, CircleShape))
                                                Spacer(Modifier.width(12.dp))
                                            }
                                            Text(task.rawText, color = textColor, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                            Text("${task.ageInDays}d", color = secondaryTextColor, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

// ==========================================
// 🎨 HELPER COMPOSABLES
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeBackground(dismissState: SwipeToDismissBoxState) {
    val color = when (dismissState.dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> Color(0xFF81C784).copy(alpha = 0.9f)
        else -> Color.Transparent
    }
    Box(
        modifier = Modifier.fillMaxSize().background(color, RoundedCornerShape(16.dp)).padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Icon(Icons.Default.Send, contentDescription = "Complete", tint = Color.White)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSwipeBackground(dismissState: SwipeToDismissBoxState) {
    val color = when (dismissState.dismissDirection) {
        SwipeToDismissBoxValue.EndToStart -> Color(0xFFEF5350).copy(alpha = 0.9f)
        SwipeToDismissBoxValue.StartToEnd -> Color(0xFF64B5F6).copy(alpha = 0.9f)
        else -> Color.Transparent
    }
    val alignment = if(dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
    val icon = if(dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Star else Icons.Default.Delete
    Box(
        modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 24.dp),
        contentAlignment = alignment
    ) {
        Icon(icon, contentDescription = "Action", tint = Color.White)
    }
}

@Composable
fun TaskRow(task: Task, onRowClick: () -> Unit) {
    // Styling Logic
    val baseColor = when {
        task.isDailyChore -> MaterialTheme.colorScheme.tertiary // Pink for Daily Chores
        task.momentumScore >= 1.2f -> Color(0xFF81C784)
        task.impactScore >= 0.85f -> Color(0xFFB00020)
        task.impactScore >= 0.70f -> Color(0xFFE65100)
        else -> Color(0xFF455A64)
    }
    val animatedColor by animateColorAsState(baseColor, animationSpec = tween(800))

    Surface(
        color = animatedColor.copy(alpha = 0.9f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onRowClick() }.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.rawText, color = if(task.isDailyChore) Color.Black else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (task.isAiGenerated) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color.White.copy(0.6f), modifier = Modifier.size(14.dp))
                    }
                    if (task.isDailyChore) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Repeat, contentDescription = "Daily", tint = Color.Black.copy(0.6f), modifier = Modifier.size(14.dp))
                    }
                }

                // Subtitle Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.isDailyChore) {
                        Text("Daily chore added by you", color = Color.Black.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        if (task.ageInDays > 0) {
                            Text("• ${task.ageInDays}d Old", color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        if (task.isManuallyPromoted) {
                            if (task.ageInDays > 0) Spacer(Modifier.width(8.dp))
                            Text("• Modified by you", color = Color(0xFF64B5F6), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClarificationOverlay(task: Task, onComplete: (Float, Float, Float, Float, Float) -> Unit) {
    var step by remember { mutableStateOf(0) }
    var immediate by remember { mutableStateOf(task.immediate) }
    var longTerm by remember { mutableStateOf(task.longTerm) }
    var proximity by remember { mutableStateOf(task.proximity) }
    var accumulation by remember { mutableStateOf(task.accumulation) }
    var effort by remember { mutableStateOf(task.effort) }

    val questions = listOf(
        "Immediate: Consequences if ignored today?",
        "Long-term: Impact on your 6-month goals?",
        "Proximity: How close is the 'Point of No Return'?",
        "Accumulation: Does this impact compound with time?",
        "Effort: Activation energy required?"
    )
    val effortOptions = listOf("Trivial (2m)" to 0.1f, "Simple (15m)" to 0.3f, "Focus (1h)" to 0.6f, "Draining (4h+)" to 0.9f)
    val impactOptions = listOf("Minimal" to 0.1f, "Low" to 0.3f, "Significant" to 0.6f, "Severe" to 0.9f)
    val currentOptions = if (step == 4) effortOptions else impactOptions

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp)).padding(24.dp)) {
            Text("TEACHING THE AI...", color = Color(0xFF64B5F6), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(task.rawText, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text(questions[step], color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(24.dp))

            currentOptions.forEach { (label, value) ->
                Button(
                    onClick = {
                        when(step) {
                            0 -> immediate = value
                            1 -> longTerm = value
                            2 -> proximity = value
                            3 -> accumulation = value
                            4 -> { effort = value; onComplete(immediate, longTerm, accumulation, proximity, effort) }
                        }
                        if (step < 4) step++
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(label)
                }
            }
        }
    }
}

@Composable
fun BrainDumpInput(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Surface(
        color = Color(0xFF1E2124).copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 0.dp,
        tonalElevation = 8.dp,
    ) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))) {
            Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp)).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() }),
                        decorationBox = { inner ->
                            if (value.isEmpty()) Text("Dump task (AI is learning)...", color = Color.White.copy(alpha = 0.3f), fontSize = 16.sp)
                            inner()
                        }
                    )
                }
                Spacer(Modifier.width(12.dp))
                val buttonColor = if (value.isNotBlank()) Color(0xFF81C784) else Color.White.copy(alpha = 0.1f)
                IconButton(onClick = onSend, enabled = value.isNotBlank(), modifier = Modifier.background(buttonColor.copy(alpha = 0.15f), CircleShape).size(48.dp)) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = buttonColor, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun FactorRow(label: String, value: Float) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        val level = when {
            value >= 0.8f -> "Critical"
            value >= 0.5f -> "Moderate"
            value >= 0.2f -> "Low"
            else -> "Negligible"
        }
        Text(level, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
    }
}
fun Intent.putTitle(title: String) { putExtra(CalendarContract.Events.TITLE, title) }
fun Intent.putDescription(desc: String) { putExtra(CalendarContract.Events.DESCRIPTION, desc) }