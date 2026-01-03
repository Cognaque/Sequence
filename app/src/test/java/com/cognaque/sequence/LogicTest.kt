package com.cognaque.sequence

import org.junit.Test
import org.junit.Assert.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class LogicTest {

    @Test
    fun testCalculateImpactScore() {
        val task = Task(
            rawText = "Test",
            immediate = 1.0f,
            longTerm = 0.5f,
            proximity = 0.5f,
            accumulation = 0.2f,
            effort = 0.1f,
            creationTimestamp = System.currentTimeMillis() // 0 days old
        )

        // Age factor = 0
        // Dynamic proximity = 0.5
        // Impact = 1.0 * 0.4 + 0.5 * 0.3 + 0.5 * 0.2 + 0.2 * 0.1
        //        = 0.4 + 0.15 + 0.1 + 0.02
        //        = 0.67
        assertEquals(0.67f, task.calculateImpactScore(), 0.001f)
    }

    @Test
    fun testCalculateImpactScoreWithAge() {
        // Mock a task created 10 days ago
        val tenDaysAgo = LocalDate.now().minusDays(10).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val task = Task(
            rawText = "Old Task",
            immediate = 0.5f,
            longTerm = 0.5f,
            proximity = 0.5f,
            accumulation = 0.5f,
            effort = 0.5f,
            creationTimestamp = tenDaysAgo
        )

        // Age factor = 10 * 0.05 = 0.5 (max is 0.5)
        // Dynamic proximity = min(0.5 + 0.5, 1.0) = 1.0
        // Impact = 0.5 * 0.4 + 0.5 * 0.3 + 1.0 * 0.2 + 0.5 * 0.1
        //        = 0.2 + 0.15 + 0.2 + 0.05
        //        = 0.6
        assertEquals(0.6f, task.calculateImpactScore(), 0.001f)
    }

    @Test
    fun testCalculateQuadrant() {
        // High impact -> Priority
        val priorityTask = Task(
            rawText = "Priority",
            immediate = 1.0f,
            longTerm = 1.0f,
            proximity = 1.0f,
            accumulation = 1.0f
        )
        assertEquals(EisenhowerQuadrant.PRIORITY, priorityTask.calculateQuadrant())

        // Manual promotion -> Priority
        val manualTask = Task(
            rawText = "Manual",
            isManuallyPromoted = true
        )
        assertEquals(EisenhowerQuadrant.PRIORITY, manualTask.calculateQuadrant())

        // Daily chore -> Priority
        val chore = Task(
            rawText = "Chore",
            isDailyChore = true
        )
        assertEquals(EisenhowerQuadrant.PRIORITY, chore.calculateQuadrant())

        // Low urgency, High importance -> Schedule
        val scheduleTask = Task(
            rawText = "Schedule",
            immediate = 0.1f,
            proximity = 0.1f, // Avg urgency = 0.1
            longTerm = 0.9f,
            accumulation = 0.9f, // Avg importance = 0.9
            effort = 0.1f
        )
        // Impact check:
        // 0.1*0.4 + 0.9*0.3 + 0.1*0.2 + 0.9*0.1 = 0.04 + 0.27 + 0.02 + 0.09 = 0.42 < 0.5 -> Not Priority
        assertEquals(EisenhowerQuadrant.SCHEDULE, scheduleTask.calculateQuadrant())

        // High urgency, Low importance -> Delegate
        val delegateTask = Task(
            rawText = "Delegate",
            immediate = 0.9f,
            proximity = 0.9f, // Avg urgency = 0.9
            longTerm = 0.1f,
            accumulation = 0.1f, // Avg importance = 0.1
            effort = 0.1f
        )
        // Impact check:
        // 0.9*0.4 + 0.1*0.3 + 0.9*0.2 + 0.1*0.1 = 0.36 + 0.03 + 0.18 + 0.01 = 0.58 >= 0.5 -> Priority ???
        // Wait, let's re-calculate.
        // 0.36 + 0.03 + 0.18 + 0.01 = 0.58.
        // 0.58 > 0.5, so it should be PRIORITY according to the code.

        // Let's try to construct a DELEGATE case that doesn't trigger Priority.
        // Needs Impact < 0.5.
        // Impact = 0.4*Imm + 0.3*Lt + 0.2*Prox + 0.1*Acc
        // AvgUrg >= 0.5 (Imm + Prox >= 1.0)
        // AvgImp < 0.5 (Lt + Acc < 1.0)

        // Try: Imm=0.6, Prox=0.4 (AvgUrg=0.5), Lt=0.1, Acc=0.1 (AvgImp=0.1)
        // Impact = 0.6*0.4 + 0.1*0.3 + 0.4*0.2 + 0.1*0.1
        //        = 0.24 + 0.03 + 0.08 + 0.01 = 0.36 < 0.5. Good.

        val realDelegateTask = Task(
            rawText = "Real Delegate",
            immediate = 0.6f,
            proximity = 0.4f,
            longTerm = 0.1f,
            accumulation = 0.1f
        )
        assertEquals(EisenhowerQuadrant.DELEGATE, realDelegateTask.calculateQuadrant())

        // Low urgency, Low importance -> Later
        val laterTask = Task(
            rawText = "Later",
            immediate = 0.1f,
            proximity = 0.1f,
            longTerm = 0.1f,
            accumulation = 0.1f
        )
        assertEquals(EisenhowerQuadrant.LATER, laterTask.calculateQuadrant())
    }

    @Test
    fun testCalculateQuadrant_HighUrgencyHighImportanceLowImpact() {
        // Case: Urgent (Proximity=1.0) and Important (Accumulation=1.0)
        // But Impact is low (Imm=0, Lt=0) -> Impact = 0.2 + 0.1 = 0.3 < 0.5.
        // AvgUrg = (0 + 1) / 2 = 0.5 (High)
        // AvgImp = (0 + 1) / 2 = 0.5 (High)
        // Current logic:
        // AvgUrg < 0.5? False.
        // AvgUrg >= 0.5 && AvgImp < 0.5? False.
        // Else -> LATER.
        //
        // Desired logic: Should be SCHEDULE (Q2) as a fallback for Q1 items that aren't quite "Impactful" enough.

        val confusingTask = Task(
            rawText = "Confusing",
            immediate = 0.0f,
            longTerm = 0.0f,
            proximity = 1.0f,
            accumulation = 1.0f
        )

        // This assertion documents the DESIRED behavior, not the current buggy behavior.
        assertEquals(EisenhowerQuadrant.SCHEDULE, confusingTask.calculateQuadrant())
    }
}
