package com.cognaque.sequence.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.cognaque.sequence.data.*
import com.cognaque.sequence.logic.LearningEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

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
