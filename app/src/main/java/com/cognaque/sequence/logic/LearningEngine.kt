package com.cognaque.sequence.logic

import com.cognaque.sequence.data.KeywordWeight
import com.cognaque.sequence.data.LearningDao

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
