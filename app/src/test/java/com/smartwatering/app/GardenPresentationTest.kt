package com.smartwatering.app

import com.smartwatering.app.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GardenPresentationTest {
    @Test fun plantNamesAreTrimmedAndBlankNamesRejected() {
        assertEquals("Kitchen basil", PlantInput.validName("  Kitchen basil  "))
        assertNull(PlantInput.validName("    "))
    }

    @Test fun suggestsOnlyUnambiguousPlantTypes() {
        assertEquals(Species.HERB, PlantInput.suggest("Kitchen BASIL"))
        assertEquals(Species.LEAFY_VEGETABLE, PlantInput.suggest("Sukuma-Wiki"))
        assertEquals(Species.FERN, PlantInput.suggest("Boston fern"))
        assertEquals(Species.SUCCULENT, PlantInput.suggest("Aloe"))
        assertEquals(Species.FLOWERING, PlantInput.suggest("Roses"))
        assertNull(PlantInput.suggest("Minty"))
        assertNull(PlantInput.suggest("Basil and roses"))
    }

    @Test fun rainfallUsesOneDecimalAndExplicitUnits() {
        assertEquals("7.3 mm", formatRainfall(7.26))
        assertEquals("0.0 mm", formatRainfall(0.0))
    }

    @Test fun reasonsUseSentenceCaseAndTheSharedRainfallFormat() {
        val decision = Decision(false, listOf(Step(4, 2.0, false, 7.26f), Step(2, 50.0, false, 65f)))
        assertEquals("The air was humid yesterday and 7.3 mm of rain fell yesterday.", ReasonLine.of(decision))
        assertEquals("The weather was mild and the air was humid yesterday.", ReasonLine.display("Mild weather, Humid yesterday"))
        assertEquals("It rained 7.0 mm yesterday.", ReasonLine.display("Rained 7 mm yesterday"))
    }

    @Test fun mapsWeatherCodesToDistinctGlyphs() {
        val expected = mapOf(0 to WeatherGlyph.SUN, 2 to WeatherGlyph.PARTLY_CLOUDY,
            3 to WeatherGlyph.CLOUD, 45 to WeatherGlyph.FOG, 51 to WeatherGlyph.DRIZZLE,
            61 to WeatherGlyph.RAIN, 82 to WeatherGlyph.RAIN, 95 to WeatherGlyph.THUNDER)
        expected.forEach { (code, glyph) -> assertEquals(glyph, WeatherGlyph.forCode(code)) }
        assertEquals(WeatherGlyph.UNKNOWN, WeatherGlyph.forCode(null))
    }
}
