package com.smartwatering.app.model

import org.json.JSONObject

data class Step(val feature: Int, val threshold: Double, val wentLeft: Boolean, val value: Float)
data class Decision(val water: Boolean, val path: List<Step>)

class DecisionTree(json: String) {
    private val root = JSONObject(json)
    val features: List<String> = root.getJSONArray("features").let { a -> List(a.length()) { a.getString(it) } }
    private val left = root.getJSONArray("children_left").toIntList()
    private val right = root.getJSONArray("children_right").toIntList()
    private val feature = root.getJSONArray("feature").toIntList()
    private val threshold = root.getJSONArray("threshold").let { a -> DoubleArray(a.length()) { a.getDouble(it) } }
    private val predicted = root.getJSONArray("predicted_class").toIntList()

    fun predict(x: FloatArray): Decision {
        require(x.size == features.size) { "Expected ${features.size} features, got ${x.size}" }
        require(x.all { it.isFinite() }) { "Features must be finite numbers" }
        val path = mutableListOf<Step>()
        var node = 0
        while (left[node] != -1) {
            val f = feature[node]
            val goLeft = x[f].toDouble() <= threshold[node]
            path += Step(f, threshold[node], goLeft, x[f])
            node = if (goLeft) left[node] else right[node]
        }
        return Decision(predicted[node] == 1, path)
    }

    private fun org.json.JSONArray.toIntList() = IntArray(length()) { getInt(it) }
}
