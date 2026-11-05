package com.smartwatering.app.model

enum class Species(val label: String, val kc: Float, val thresholdMm: Float) {
    SUCCULENT("Succulent", 0.20f, 24.5f),
    HERB("Herb", 0.30f, 21.0f),
    FLOWERING("Flowering", 0.50f, 14.0f),
    FERN("Fern", 0.80f, 9.8f),
    LEAFY_VEGETABLE("Leafy veg", 1.00f, 18.9f),
}
