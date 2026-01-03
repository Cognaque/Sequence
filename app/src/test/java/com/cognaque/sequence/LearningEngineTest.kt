package com.cognaque.sequence

import com.cognaque.sequence.data.KeywordWeight
import com.cognaque.sequence.data.LearningDao
import com.cognaque.sequence.logic.LearningEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

// Mock LearningDao manually since we can't use Mockito easily in this environment without setup
class MockLearningDao : LearningDao {
    val weights = mutableMapOf<String, KeywordWeight>()

    override suspend fun getWeight(keyword: String): KeywordWeight? {
        return weights[keyword]
    }

    override suspend fun getWeightsForKeywords(keywords: List<String>): List<KeywordWeight> {
        return keywords.mapNotNull { weights[it] }
    }

    override suspend fun saveWeight(weight: KeywordWeight) {
        weights[weight.keyword] = weight
    }

    override suspend fun deleteAllWeights() {
        weights.clear()
    }

    override suspend fun pruneExcessKeywords(limit: Int) {
        // Not needed for basic tests
    }
}

class LearningEngineTest {

    private lateinit var learningDao: MockLearningDao
    private lateinit var learningEngine: LearningEngine

    @Before
    fun setUp() {
        learningDao = MockLearningDao()
        learningEngine = LearningEngine(learningDao)
    }

    @Test
    fun testLearnFromTask() = runBlocking {
        val text = "Pay bills"

        learningEngine.learnFromTask(
            text = text,
            imm = 1.0f,
            lt = 0.5f,
            prox = 0.8f,
            acc = 0.2f,
            eff = 0.4f
        )

        val payWeight = learningDao.getWeight("pay")
        assertNotNull(payWeight)
        assertEquals(1, payWeight!!.count)
        assertEquals(1.0f, payWeight.avgImm, 0.001f)

        val billsWeight = learningDao.getWeight("bills")
        assertNotNull(billsWeight)

        val bigramWeight = learningDao.getWeight("pay bills")
        assertNotNull(bigramWeight)
    }

    @Test
    fun testPredict() = runBlocking {
        // Teach it "urgent"
        learningEngine.learnFromTask("urgent", 1.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        learningEngine.learnFromTask("urgent", 1.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        learningEngine.learnFromTask("urgent", 1.0f, 0.0f, 0.0f, 0.0f, 0.0f)

        // Prediction should be high confidence and high immediate
        val prediction = learningEngine.predict("urgent task")

        assertTrue(prediction.confidence)
        assertEquals(1.0f, prediction.imm, 0.1f)
    }

    @Test
    fun testPredictLowConfidence() = runBlocking {
        // Teach it just once, total weight might be low
        learningEngine.learnFromTask("rare", 1.0f, 0.0f, 0.0f, 0.0f, 0.0f)

        val prediction = learningEngine.predict("rare")
        assertFalse(prediction.confidence)
    }

    @Test
    fun testIncrementalLearning() = runBlocking {
        learningEngine.learnFromTask("test", 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        var w = learningDao.getWeight("test")
        assertEquals(1, w!!.count)
        assertEquals(0.0f, w.avgImm, 0.001f)

        // Learn again with different values
        learningEngine.learnFromTask("test", 1.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        w = learningDao.getWeight("test")
        assertEquals(2, w!!.count)
        assertEquals(0.5f, w.avgImm, 0.001f) // (0 + 1) / 2 = 0.5
    }
}
