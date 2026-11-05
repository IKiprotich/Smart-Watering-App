package com.smartwatering.app

import com.smartwatering.app.model.DecisionTree
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class TreeParityTest {
    @Test
    fun kotlinTreeMatchesNotebook() {
        val loader = javaClass.classLoader!!
        val tree = DecisionTree(loader.getResource("decision_tree.json")!!.readText())
        val cases = JSONObject(loader.getResource("android_parity_cases.json")!!.readText()).getJSONArray("cases")
        for (i in 0 until cases.length()) {
            val c = cases.getJSONObject(i)
            val input = c.getJSONArray("features").let { a -> FloatArray(a.length()) { a.getDouble(it).toFloat() } }
            assertEquals("case $i", c.getInt("expected_class"), if (tree.predict(input).water) 1 else 0)
        }
    }
}
