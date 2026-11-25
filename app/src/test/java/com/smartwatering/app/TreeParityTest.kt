package com.smartwatering.app

import com.smartwatering.app.model.DecisionTree
import com.smartwatering.app.model.DailyWeather
import com.smartwatering.app.model.FeatureBuilder
import com.smartwatering.app.model.Species
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class TreeParityTest {
    @Test
    fun kotlinTreeMatchesNotebook() {
        val loader = javaClass.classLoader!!
        val export = loader.getResource("decision_tree.json")!!.readText()
        val tree = DecisionTree(export)
        val parity = JSONObject(loader.getResource("android_parity_cases.json")!!.readText())
        val featureOrder = parity.getJSONArray("feature_order")
            .let { array -> List(array.length()) { index -> array.getString(index) } }

        assertEquals("feature order", featureOrder, tree.features)
        assertEquals("Android feature order", featureOrder, FeatureBuilder.featureOrder)

        val cases = parity.getJSONArray("cases")
        val leafPaths = mutableSetOf<String>()
        for (index in 0 until cases.length()) {
            val testCase = cases.getJSONObject(index)
            val input = testCase.getJSONArray("features")
                .let { array -> FloatArray(array.length()) { item -> array.getDouble(item).toFloat() } }
            val decision = tree.predict(input)
            val predictedClass = if (decision.water) 1 else 0
            assertEquals("case $index", testCase.getInt("expected_class"), predictedClass)
            if (testCase.has("expected_path")) {
                val path = decision.path.joinToString("") { if (it.wentLeft) "L" else "R" }
                assertEquals("path for case $index", testCase.getString("expected_path"), path)
                leafPaths += path
            }
        }
        val children = JSONObject(export).getJSONArray("children_left")
        val leafCount = (0 until children.length()).count { children.getInt(it) == -1 }
        assertEquals("leaf path coverage", leafCount, leafPaths.size)
        assertEquals("original and structural fixture count",
            parity.getInt("reference_case_count") + leafCount + 2 * (children.length() - leafCount),
            cases.length())
    }

    @Test
    fun featuresUseCompletedWeatherDaysAndEffectiveRainThreshold() {
        val weather = DailyWeather(
            dates = listOf("2026-11-17", "2026-11-18", "2026-11-19", "2026-11-20"),
            tempMean = doubleArrayOf(18.0, 19.0, 20.0, 99.0),
            tempMax = doubleArrayOf(22.0, 23.0, 24.0, 99.0),
            humidityMean = doubleArrayOf(80.0, 70.0, 60.0, 99.0),
            windMax = doubleArrayOf(14.0, 15.0, 16.0, 99.0),
            rain = doubleArrayOf(2.0, 1.0, 0.5, 99.0),
            et0 = doubleArrayOf(3.0, 4.0, 5.0, 99.0),
            weatherCode = IntArray(4)
        )
        assertArrayEquals(
            floatArrayOf(20f, 24f, 60f, 16f, 0.5f, 3.5f, 5f, 12f, 2f, 7f, 0.3f, 21f),
            FeatureBuilder.build(weather, Species.HERB, daysSinceWatered = 7),
            0.0001f
        )
    }

    @Test
    fun rejectsImplausibleWeatherBeforePrediction() {
        val valid = floatArrayOf(20f, 24f, 60f, 16f, 0.5f, 3.5f, 5f, 12f, 2f, 7f, 0.3f, 21f)
        assertTrue(FeatureBuilder.acceptsForPrediction(valid))
        for ((index, value) in listOf(1 to 19f, 2 to 140f, 4 to -2f, 6 to Float.NaN, 9 to 0f)) {
            val invalid = valid.copyOf().also { it[index] = value }
            assertFalse("invalid feature $index", FeatureBuilder.acceptsForPrediction(invalid))
        }
    }

    @Test
    fun dryStreakCrossesTheTreesLongestThreshold() {
        val weather = DailyWeather(
            dates = List(72) { "day $it" },
            tempMean = DoubleArray(72) { 20.0 },
            tempMax = DoubleArray(72) { 25.0 },
            humidityMean = DoubleArray(72) { 60.0 },
            windMax = DoubleArray(72) { 15.0 },
            rain = DoubleArray(72),
            et0 = DoubleArray(72) { 4.0 },
            weatherCode = IntArray(72)
        )
        assertEquals(71f, FeatureBuilder.build(weather, Species.HERB, 7)[8], 0f)
        weather.rain[0] = Double.NaN
        org.junit.Assert.assertTrue(FeatureBuilder.build(weather, Species.HERB, 7)[8].isNaN())
    }
}
