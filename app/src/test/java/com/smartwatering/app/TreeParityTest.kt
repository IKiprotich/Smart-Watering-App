package com.smartwatering.app

import com.smartwatering.app.model.DecisionTree
import com.smartwatering.app.model.DailyWeather
import com.smartwatering.app.model.FeatureBuilder
import com.smartwatering.app.model.Species
import org.junit.Assert.assertArrayEquals
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class TreeParityTest {
    @Test
    fun kotlinTreeMatchesNotebook() {
        val loader = javaClass.classLoader!!
        val tree = DecisionTree(loader.getResource("decision_tree.json")!!.readText())
        val parity = JSONObject(loader.getResource("android_parity_cases.json")!!.readText())
        val featureOrder = parity.getJSONArray("feature_order")
            .let { array -> List(array.length()) { index -> array.getString(index) } }

        assertEquals("feature order", featureOrder, tree.features)
        assertEquals("Android feature order", featureOrder, FeatureBuilder.featureOrder)

        val cases = parity.getJSONArray("cases")
        for (index in 0 until cases.length()) {
            val testCase = cases.getJSONObject(index)
            val input = testCase.getJSONArray("features")
                .let { array -> FloatArray(array.length()) { item -> array.getDouble(item).toFloat() } }
            val predictedClass = if (tree.predict(input).water) 1 else 0
            assertEquals("case $index", testCase.getInt("expected_class"), predictedClass)
        }
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
