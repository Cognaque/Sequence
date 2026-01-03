@file:OptIn(ExperimentalMaterial3Api::class)

package com.cognaque.sequence

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.abs

// ==========================================
// 1. CONSTANTS & RESOURCES
// ==========================================

object AppStrings {
    const val PRIORITY_TITLE = "PRIORITY"
    const val FOCUS_STACK = "FOCUS STACK"
    const val DUMP_PLACEHOLDER = "Dump task..."
    const val DELETE_ALL_TITLE = "Delete Everything?"
    const val DELETE_ALL_BODY = "This will wipe all tasks and memory. Cannot be undone."
    const val DELETE_CONFIRM = "DELETE ALL"
    const val EXPORT_READY = "Data Export Ready"
    const val EXPORT_BODY = "Your data has been converted to JSON. Copy it to your clipboard."
    const val IMPORT_TITLE = "Import Data"
    const val IMPORT_BODY = "Paste JSON data to restore your brain."
    const val DAILY_CHORE_PROTOCOL = "DAILY CHORE"
    const val DATA_SOVEREIGNTY = "MANAGE DATA"
    const val NO_THREATS = "I am a brain Watson, the rest of me is a mere appendix"
    const val SLEEP_MODE = "🌙 Sleep Mode Active"
}

object AppColors {
    val Primary = Color(0xFF81C784)
    val Secondary = Color(0xFF64B5F6)
    val Tertiary = Color(0xFFF48FB1)
    val Background = Color(0xFF121212)
    val Surface = Color(0xFF1E1E1E)
    val Error = Color(0xFFEF5350)
    val AddAction = Color(0xFFFFD54F)
    val SurfaceVariant = Color(0xFF2C2F33)
    val TextPrimary = Color(0xFFEEEEEE)
    val TextSecondary = Color(0xFFB0B0B0)
}

object AppConstants {
    const val MAX_INPUT_LENGTH = 500
    const val DB_VERSION = 13
    const val FLOAT_TOLERANCE = 0.0001f
    const val MAX_KEYWORD_STORAGE = 2000
    const val INTENT_MAX_STR_LEN = 1000
    const val PRIORITY_LIST_LIMIT = 7
}

// ==========================================
// 2. DATA LAYER (Room & Entities)
// ==========================================

enum class EisenhowerQuadrant { PRIORITY, SCHEDULE, DELEGATE, LATER }

@Entity(tableName = "keyword_weights")
data class KeywordWeight(
    @PrimaryKey val keyword: String,
    val count: Int,
    val avgImm: Float, val avgLt: Float, val avgProx: Float, val avgAcc: Float, val avgEff: Float,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_chores")
data class DailyChore(@PrimaryKey val id: String = UUID.randomUUID().toString(), val text: String)

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
    val isDailyChore: Boolean = false,
    val entryCount: Int = 1,
    val creationTimestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val parentId: String? = null,
    val orderIndex: Long = System.currentTimeMillis()
)

fun Task.getAgeInDays(): Int {
    return try {
        ChronoUnit.DAYS.between(
            Instant.ofEpochMilli(creationTimestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
            LocalDate.now(ZoneId.systemDefault())
        ).toInt().coerceAtLeast(0)
    } catch (e: Exception) { 0 }
}

fun Task.calculateImpactScore(): Float {
    val ageFactor = (getAgeInDays() * 0.05f).coerceAtMost(0.5f)
    val dynamicProximity = (proximity + ageFactor).coerceAtMost(1.0f)
    return ((immediate * 0.4f) + (longTerm * 0.3f) + (dynamicProximity * 0.2f) + (accumulation * 0.1f))
}

fun Task.calculateMomentumScore(): Float {
    return calculateImpactScore() * (1.5f - (effort * 0.5f))
}

fun Task.calculateQuadrant(): EisenhowerQuadrant {
    if (isDailyChore || isManuallyPromoted) return EisenhowerQuadrant.PRIORITY
    val impact = calculateImpactScore()
    if (impact >= (0.5f - AppConstants.FLOAT_TOLERANCE)) return EisenhowerQuadrant.PRIORITY

    val avgUrgency = (immediate + proximity) / 2f
    val avgImportance = (longTerm + accumulation) / 2f

    return when {
        avgImportance >= 0.5f -> EisenhowerQuadrant.SCHEDULE
        avgUrgency >= 0.5f -> EisenhowerQuadrant.DELEGATE
        else -> EisenhowerQuadrant.LATER
    }
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isDone = 0")
    fun getActiveTasksFlow(): Flow<List<Task>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksOneShot(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Update
    suspend fun updateTasks(tasks: List<Task>)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): Task?

    @Query("SELECT * FROM tasks WHERE normalizedSignature = :sig AND isDone = 0 LIMIT 1")
    suspend fun findActiveTaskBySignature(sig: String): Task?

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("SELECT * FROM daily_chores")
    fun getDailyChoreTemplates(): Flow<List<DailyChore>>

    @Query("SELECT * FROM daily_chores")
    suspend fun getDailyChoreTemplatesOneShot(): List<DailyChore>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyChoreTemplate(chore: DailyChore)

    @Delete
    suspend fun deleteDailyChoreTemplate(chore: DailyChore)

    @Query("DELETE FROM daily_chores")
    suspend fun deleteAllDailyChores()

    @Query("SELECT COUNT(*) FROM tasks WHERE isDailyChore = 1 AND rawText = :text AND (isDone = 0 OR creationTimestamp >= :startOfDay)")
    suspend fun hasDailyChoreForToday(text: String, startOfDay: Long): Int
}

@Dao
interface LearningDao {
    @Query("SELECT * FROM keyword_weights WHERE keyword = :keyword")
    suspend fun getWeight(keyword: String): KeywordWeight?

    @Query("SELECT * FROM keyword_weights WHERE keyword IN (:keywords)")
    suspend fun getWeightsForKeywords(keywords: List<String>): List<KeywordWeight>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWeight(weight: KeywordWeight)

    @Query("DELETE FROM keyword_weights")
    suspend fun deleteAllWeights()

    @Query("DELETE FROM keyword_weights WHERE keyword NOT IN (SELECT keyword FROM keyword_weights ORDER BY count DESC, lastUpdated DESC LIMIT :limit)")
    suspend fun pruneExcessKeywords(limit: Int)
}

@Database(entities = [Task::class, KeywordWeight::class, DailyChore::class], version = AppConstants.DB_VERSION, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun learningDao(): LearningDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "adhd_brain_db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

// ==========================================
// 3. LOGIC (AI Engine)
// ==========================================

class LearningEngine(private val learningDao: LearningDao) {
    private fun tokenize(text: String): List<String> =
        text.lowercase().split(Regex("[^a-z]+")).filter { it.length > 2 } +
                text.lowercase().split(Regex("[^a-z]+")).zipWithNext { a, b -> "$a $b" }

    suspend fun learnFromTask(text: String, imm: Float, lt: Float, prox: Float, acc: Float, eff: Float) {
        val now = System.currentTimeMillis()
        val tokens = tokenize(text)
        val existingWeights = learningDao.getWeightsForKeywords(tokens).associateBy { it.keyword }

        tokens.forEach { t ->
            val w = existingWeights[t]
            val newWeight = if (w != null) {
                KeywordWeight(t, w.count + 1,
                    (w.avgImm * w.count + imm) / (w.count + 1),
                    (w.avgLt * w.count + lt) / (w.count + 1),
                    (w.avgProx * w.count + prox) / (w.count + 1),
                    (w.avgAcc * w.count + acc) / (w.count + 1),
                    (w.avgEff * w.count + eff) / (w.count + 1), now)
            } else {
                KeywordWeight(t, 1, imm, lt, prox, acc, eff, now)
            }
            learningDao.saveWeight(newWeight)
        }
    }

    suspend fun predict(text: String): PredictionResult {
        val tokens = tokenize(text)
        if (tokens.isEmpty()) return PredictionResult(0f, 0f, 0f, 0f, 0f, false)

        val weights = learningDao.getWeightsForKeywords(tokens)
        if (weights.isEmpty()) return PredictionResult(0f, 0f, 0f, 0f, 0f, false)

        var i = 0f; var l = 0f; var p = 0f; var a = 0f; var e = 0f; var totalWeight = 0

        weights.forEach { w ->
            val relevance = w.count.coerceAtMost(10)
            i += w.avgImm * relevance
            l += w.avgLt * relevance
            p += w.avgProx * relevance
            a += w.avgAcc * relevance
            e += w.avgEff * relevance
            totalWeight += relevance
        }

        return if (totalWeight < 3) PredictionResult(0f, 0f, 0f, 0f, 0f, false)
        else PredictionResult(i / totalWeight, l / totalWeight, p / totalWeight, a / totalWeight, e / totalWeight, true)
    }

    fun generateNormalizedSignature(text: String) = tokenize(text).sorted().joinToString(" ")
    data class PredictionResult(val imm: Float, val lt: Float, val prox: Float, val acc: Float, val eff: Float, val confidence: Boolean)
}

// ==========================================
// 4. VIEWMODEL
// ==========================================

data class TaskDisplayItem(val task: Task, val indentLevel: Int)

class TaskViewModel(context: Context) : ViewModel() {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.taskDao()
    private val aiEngine = LearningEngine(db.learningDao())
    private val prefs = context.getSharedPreferences("adhd_prefs", Context.MODE_PRIVATE)

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _windDownHour = MutableStateFlow(prefs.getInt("wind_down_hour", 22))
    val windDownHour = _windDownHour.asStateFlow()

    private val _displayTasks = MutableStateFlow<List<TaskDisplayItem>>(emptyList())
    val displayTasks = _displayTasks.asStateFlow()

    private var isDragging = false

    init {
        checkAndGenerateDailyChores()
        viewModelScope.launch(Dispatchers.IO) { db.learningDao().pruneExcessKeywords(AppConstants.MAX_KEYWORD_STORAGE) }

        viewModelScope.launch {
            dao.getActiveTasksFlow().combine(_windDownHour) { list, windDown ->
                val nowHour = LocalTime.now().hour
                val sleep = if (windDown > 4) (nowHour >= windDown || nowHour < 4) else (nowHour >= windDown && nowHour < 4)

                val relevantTasks = list.filter {
                    val quad = it.calculateQuadrant()
                    val isPriority = it.isDailyChore || (quad == EisenhowerQuadrant.PRIORITY && !it.needsClarification)
                    val isSleepHidden = sleep && it.effort > 0.4f && !it.isDailyChore
                    isPriority && !isSleepHidden
                }

                val grouped = relevantTasks.groupBy { it.parentId }
                val rootTasks = grouped[null]?.sortedWith(
                    compareByDescending<Task> { it.isDailyChore }.thenByDescending { it.calculateMomentumScore() }
                ) ?: emptyList()

                val sortedRoots = rootTasks.take(AppConstants.PRIORITY_LIST_LIMIT)
                val flattened = mutableListOf<TaskDisplayItem>()

                fun recurse(parentId: String, depth: Int) {
                    if (depth > 5) return
                    val children = grouped[parentId]?.sortedBy { it.orderIndex } ?: emptyList()
                    children.forEach { child ->
                        flattened.add(TaskDisplayItem(child, depth))
                        recurse(child.id, depth + 1)
                    }
                }

                sortedRoots.forEach { root ->
                    flattened.add(TaskDisplayItem(root, 0))
                    recurse(root.id, 1)
                }
                flattened.toList()
            }
                .flowOn(Dispatchers.Default)
                .collect {
                    if (!isDragging) _displayTasks.value = it
                }
        }
    }

    fun onDragStart() { isDragging = true }

    fun onDragEnd() {
        isDragging = false
        val currentItems = _displayTasks.value
        viewModelScope.launch(Dispatchers.IO) {
            val updates = currentItems.mapIndexed { index, item ->
                item.task.copy(orderIndex = index.toLong())
            }
            dao.updateTasks(updates)
        }
    }

    fun onDragCancel() {
        onDragEnd()
    }

    fun swapItems(fromIndex: Int, toIndex: Int) {
        val currentList = _displayTasks.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val itemFrom = currentList[fromIndex]
            val itemTo = currentList[toIndex]

            if (itemFrom.indentLevel == itemTo.indentLevel && itemFrom.task.parentId == itemTo.task.parentId) {
                currentList[fromIndex] = itemTo
                currentList[toIndex] = itemFrom
                _displayTasks.value = currentList
            }
        }
    }

    val secondaryTasks = dao.getActiveTasksFlow().map { list ->
        list.filter {
            it.calculateQuadrant() != EisenhowerQuadrant.PRIORITY &&
                    !it.needsClarification &&
                    !it.isDailyChore &&
                    it.parentId == null
        }.groupBy { it.calculateQuadrant() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val activeClarification = dao.getActiveTasksFlow().map { it.find { t -> t.needsClarification } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dailyChoreTemplates = dao.getDailyChoreTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun checkAndGenerateDailyChores() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val templates = dao.getDailyChoreTemplatesOneShot()
                    templates.forEach {
                        if (dao.hasDailyChoreForToday(it.text, startOfDay) == 0) {
                            dao.insertTask(
                                Task(
                                    rawText = it.text,
                                    isDailyChore = true,
                                    immediate = 0.8f, longTerm = 0.8f, proximity = 1.0f, accumulation = 0.5f, effort = 0.3f,
                                    normalizedSignature = "DAILY_${it.id}_${startOfDay}"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                emitError("Sync failed", e)
            }
        }
    }

    fun onAddTask(text: String) {
        if (text.isBlank()) return
        if (text.length > 500) { emitError("Too long"); return }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val sig = aiEngine.generateNormalizedSignature(text)
                val existing = dao.findActiveTaskBySignature(sig)

                if (existing != null) {
                    dao.updateTask(existing.copy(entryCount = existing.entryCount + 1, needsReEvaluation = existing.entryCount >= 3))
                    emitMessage("Merged with existing task")
                } else {
                    val p = aiEngine.predict(text)
                    val newTask = if (p.confidence) {
                        Task(rawText = text, normalizedSignature = sig, immediate = p.imm, longTerm = p.lt, proximity = p.prox, accumulation = p.acc, effort = p.eff, isAiGenerated = true)
                    } else {
                        Task(rawText = text, normalizedSignature = sig, needsClarification = true)
                    }
                    dao.insertTask(newTask)
                }
            } catch (e: Exception) {
                emitError("Add failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addSubtask(parentId: String, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val parent = dao.getTaskById(parentId) ?: return@launch
            dao.insertTask(
                Task(
                    rawText = text, parentId = parentId,
                    immediate = parent.immediate, longTerm = parent.longTerm, proximity = parent.proximity, accumulation = parent.accumulation, effort = parent.effort,
                    isManuallyPromoted = true,
                    isDailyChore = parent.isDailyChore
                )
            )
        }
    }

    fun submitClarification(taskId: String, imm: Float, lt: Float, acc: Float, prox: Float, eff: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.getTaskById(taskId)?.let {
                    aiEngine.learnFromTask(it.rawText, imm, lt, prox, acc, eff)
                    dao.updateTask(it.copy(immediate = imm, longTerm = lt, accumulation = acc, proximity = prox, effort = eff, isAiGenerated = false, needsClarification = false))
                    //emitMessage("Brain updated")
                }
            } catch (e: Exception) {
                emitError("Save failed", e)
            }
        }
    }

    fun saveNotes(task: Task, notes: String) {
        viewModelScope.launch(Dispatchers.IO) { try { dao.updateTask(task.copy(notes = notes)) } catch (e: Exception) { emitError("Note failed", e) } }
    }

    fun deleteTask(task: Task) = viewModelScope.launch(Dispatchers.IO) { try { dao.deleteTask(task) } catch (e: Exception) { emitError("Delete failed", e) } }

    fun promoteTask(task: Task) = viewModelScope.launch(Dispatchers.IO) { try { dao.updateTask(task.copy(isManuallyPromoted = true)) } catch (e: Exception) { emitError("Update failed", e) } }

    fun markDone(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        try {
            dao.updateTask(task.copy(isDone = true))
        } catch (e: Exception) { emitError("Done failed", e) }
    }

    fun addDailyChore(t: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            dao.insertDailyChoreTemplate(DailyChore(text = t.trim()))
            checkAndGenerateDailyChores()
            //emitMessage("Chore added")
        } catch (e: Exception) { emitError("Add failed", e) }
    }

    fun deleteDailyChore(c: DailyChore) = viewModelScope.launch(Dispatchers.IO) { try { dao.deleteDailyChoreTemplate(c) } catch (e: Exception) { emitError("Delete failed", e) } }

    fun setWindDownHour(h: Int) {
        val v = h.coerceIn(0, 23)
        _windDownHour.value = v
        prefs.edit().putInt("wind_down_hour", v).apply()
    }

    fun generateExport() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val tasks = dao.getAllTasksOneShot()
                val json = JSONObject().apply {
                    put("tasks", JSONArray().apply {
                        tasks.forEach {
                            put(JSONObject().apply {
                                put("text", it.rawText)
                                put("created", it.creationTimestamp)
                                put("isDone", it.isDone)
                                put("notes", it.notes)
                            })
                        }
                    })
                    put("dailies", JSONArray().apply {
                        dao.getDailyChoreTemplatesOneShot().forEach { put(it.text) }
                    })
                }
                _uiEvent.emit(UiEvent.ShowExport(json.toString(2)))
            } catch (e: Exception) {
                emitError("Export failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importData(json: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                if (json.isBlank()) throw Exception("Empty")
                val root = JSONObject(json)
                db.withTransaction {
                    val t = root.optJSONArray("tasks")
                    if (t != null) {
                        for (i in 0 until t.length()) {
                            val o = t.getJSONObject(i)
                            dao.insertTask(
                                Task(
                                    rawText = o.getString("text"),
                                    creationTimestamp = o.optLong("created", System.currentTimeMillis()),
                                    isDone = o.optBoolean("isDone"),
                                    notes = o.optString("notes")
                                )
                            )
                        }
                    }
                    val d = root.optJSONArray("dailies")
                    if (d != null) for (i in 0 until d.length()) dao.insertDailyChoreTemplate(DailyChore(text = d.getString(i)))
                }
                emitMessage("Imported")
            } catch (e: Exception) {
                emitError("Import failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun nukeAllData() = viewModelScope.launch(Dispatchers.IO) {
        try {
            dao.deleteAllTasks()
            dao.deleteAllDailyChores()
            db.learningDao().deleteAllWeights()
            checkAndGenerateDailyChores()
            emitMessage("Reset complete")
        } catch (e: Exception) {
            emitError("Reset failed", e)
        }
    }

    private fun emitError(m: String, e: Throwable? = null) {
        Log.e("SequenceApp", m, e)
        viewModelScope.launch { _uiEvent.emit(UiEvent.ShowSnackbar("⚠️ $m")) }
    }

    private fun emitMessage(m: String) {
        viewModelScope.launch { _uiEvent.emit(UiEvent.ShowSnackbar(m)) }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        data class ShowExport(val json: String) : UiEvent()
    }
}

class TaskViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = TaskViewModel(context) as T
}

// ==========================================
// 5. UI COMPONENTS
// ==========================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(android.graphics.Color.BLACK)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val context = LocalContext.current
            val viewModel: TaskViewModel = viewModel(factory = TaskViewModelFactory(context.applicationContext))
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(Unit) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) viewModel.checkAndGenerateDailyChores()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            AppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = AppColors.Background) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AppColors.Primary,
            secondary = AppColors.Secondary,
            surface = AppColors.Surface,
            background = AppColors.Background,
            tertiary = AppColors.Tertiary,
            error = AppColors.Error
        ),
        content = content
    )
}

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
        containerColor = AppColors.Background
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
                            }
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
        task.isDailyChore -> listOf(AppColors.Tertiary, AppColors.Tertiary)
        task.isManuallyPromoted -> listOf(Color(0xFF424242), Color(0xFF616161))
        task.calculateMomentumScore() >= 1.2f -> listOf(Color(0xFF7F0000), Color(0xFFB71C1C))
        impact >= 0.85f -> listOf(Color(0xFFF57C00), Color(0xFFE65100))
        impact >= 0.6f -> listOf(Color(0xFFFF9800), Color(0xFFFFB74D))
        else -> listOf(Color(0xFF81C784), Color(0xFF66BB6A))
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
                        color = if (task.isDailyChore) Color.Black else Color.White
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
    val isSleep = remember(windDown) {
        val h = LocalTime.now().hour
        if (windDown > 4) (h >= windDown || h < 4) else (h >= windDown && h < 4)
    }
    var showVault by remember { mutableStateOf(false) }

    Column(Modifier.statusBarsPadding().fillMaxWidth()) {
        if (isSleep) Box(Modifier.fillMaxWidth().background(AppColors.SurfaceVariant.copy(0.5f)).padding(top = 8.dp), contentAlignment = Alignment.Center) { Text(AppStrings.SLEEP_MODE, fontSize = 12.sp, color = AppColors.Secondary) }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), horizontalArrangement = Arrangement.Center) {
            Text(AppStrings.PRIORITY_TITLE, style = TextStyle(fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 2.sp, color = AppColors.Primary), modifier = Modifier.pointerInput(Unit) { detectTapGestures(onLongPress = { showVault = true }) })
        }
    }
    if (showVault) VaultSheet({ showVault = false }, viewModel, onTriggerReeval)
}

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

@Composable
fun TaskInput(onAdd: (String) -> Unit, isLoading: Boolean) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    Surface(color = AppColors.SurfaceVariant, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Row(Modifier.padding(20.dp).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).background(Color.White.copy(0.05f), RoundedCornerShape(24.dp)).clickable { focusRequester.requestFocus() }.padding(16.dp, 12.dp)) {
                BasicTextField(
                    value = text,
                    onValueChange = { if (it.length <= AppConstants.MAX_INPUT_LENGTH) text = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onAdd(text); text = "" }),
                    modifier = Modifier.focusRequester(focusRequester).fillMaxWidth()
                )
                if (text.isEmpty()) Text(AppStrings.DUMP_PLACEHOLDER, color = Color.Gray)
            }
            Spacer(Modifier.width(12.dp))
            if (isLoading) CircularProgressIndicator(Modifier.size(24.dp))
            else IconButton(onClick = { onAdd(text); text = "" }) { Icon(Icons.Default.Send, null, tint = AppColors.Primary) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrioritySwipeBackground(dismissState: SwipeToDismissBoxState) {
    val direction = dismissState.dismissDirection
    val color = if (direction == SwipeToDismissBoxValue.StartToEnd) AppColors.Primary else AppColors.AddAction
    val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
    val icon = if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Check else Icons.Default.Add
    Box(Modifier.fillMaxSize().background(color, RoundedCornerShape(16.dp)).padding(horizontal = 24.dp), contentAlignment = alignment) {
        Icon(icon, null, tint = if (direction == SwipeToDismissBoxValue.EndToStart) Color.Black else Color.White)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSwipeBackground(dismissState: SwipeToDismissBoxState) {
    val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) AppColors.Primary else AppColors.Error
    val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
    val icon = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Icons.Default.ArrowUpward else Icons.Default.Delete
    Box(Modifier.fillMaxSize().background(color).padding(horizontal = 24.dp), contentAlignment = alignment) { Icon(icon, null, tint = Color.White) }
}

@Composable
fun TaskDetailsDialog(task: Task, onDismiss: () -> Unit, onSaveNotes: (String) -> Unit, onReevaluate: () -> Unit) {
    val ctx = LocalContext.current
    var notes by remember(task.id) { mutableStateOf(task.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Task Details") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(task.rawText, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.TextPrimary)
                Text("Created ${task.getAgeInDays()} days ago", fontSize = 12.sp, color = Color.Gray)
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
                TextButton(onClick = onReevaluate) { Text("Re-evaluate", color = AppColors.Tertiary) }
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
                .background(AppColors.Surface, RoundedCornerShape(24.dp))
                .border(1.dp, AppColors.Primary.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Text("TEACHING THE AI...", color = AppColors.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(task.rawText, color = AppColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text(questions[step], color = AppColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
                        containerColor = AppColors.SurfaceVariant,
                        contentColor = AppColors.Primary
                    )
                ) {
                    Text(label)
                }
            }
        }
    }
}

@Composable fun HeaderTitle(text: String, isHidden: Boolean) { Text(text, color = if (isHidden) AppColors.Secondary else AppColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🧠", fontSize = 54.sp) // Semicolon is optional in Kotlin, usually removed

        Text(
            text = AppStrings.NO_THREATS,
            color = Color.Gray,
            fontSize = 9.5.sp
        )
    }
}@Composable fun ExportDialog(json: String, onDismiss: () -> Unit, onCopy: (String) -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text(AppStrings.EXPORT_READY) }, text = { Text(AppStrings.EXPORT_BODY) }, confirmButton = { Button(onClick = { onCopy(json) }) { Text("Copy") } }) }
@Composable fun ImportDialog(onDismiss: () -> Unit, onPaste: (String) -> Unit, cm: ClipboardManager) { AlertDialog(onDismissRequest = onDismiss, title = { Text(AppStrings.IMPORT_TITLE) }, text = { Text(AppStrings.IMPORT_BODY) }, confirmButton = { Button(onClick = { cm.getText()?.text?.let(onPaste) }) { Text("Paste") } }) }
