package com.cognaque.sequence.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

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
    // Adjusted for Time Blindness: Prioritize LongTerm (0.4) and Proximity (0.3) over Immediate (0.2)
    return ((longTerm * 0.4f) + (dynamicProximity * 0.3f) + (immediate * 0.2f) + (accumulation * 0.1f))
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
