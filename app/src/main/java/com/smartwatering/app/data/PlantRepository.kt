package com.smartwatering.app.data

import kotlinx.coroutines.flow.Flow
import com.smartwatering.app.model.Species
import com.smartwatering.app.model.PlantInput
import androidx.room.withTransaction

class PlantRepository(private val database: AppDatabase) {
    private val dao = database.dao()
    fun plants(): Flow<List<Plant>> = dao.plants()

    fun plant(id: Long): Flow<Plant?> = dao.plant(id)

    fun history(plantId: Long): Flow<List<CheckIn>> = dao.history(plantId)

    fun decisions(date: String): Flow<List<DecisionRecord>> = dao.decisions(date)

    suspend fun allPlants(): List<Plant> = dao.allPlants()

    fun observeCheckIns() = dao.observeCheckIns()

    suspend fun allCheckIns(): List<CheckIn> = dao.allCheckIns()

    suspend fun currentDecisions(date: String) = dao.currentDecisions(date)

    suspend fun allDecisions(): List<DecisionRecord> = dao.allDecisions()

    suspend fun addPlant(name: String, species: Species, lastWateredAt: Long?): Long =
        database.withTransaction {
            val cleanName = requireNotNull(PlantInput.validName(name))
            val id = dao.insertPlant(Plant(name = cleanName, species = species))
            if (lastWateredAt != null) {
                logCheckIn(id, CheckInType.WATERED, lastWateredAt)
            }
            id
        }

    suspend fun logCheckIn(
        plantId: Long,
        type: CheckInType,
        timestamp: Long = System.currentTimeMillis()
    ): Long = database.withTransaction {
        check(dao.plantExists(plantId)) { "Plant was removed" }
        dao.insertCheckInAndReturnId(CheckIn(plantId = plantId, type = type, timestamp = timestamp))
    }

    suspend fun undoCheckIn(id: Long) = dao.deleteCheckIn(id)

    suspend fun removePlant(id: Long) = database.withTransaction {
        dao.deletePlantCheckIns(id)
        dao.deletePlantDecisions(id)
        dao.deletePlantRecord(id)
    }

    suspend fun updateSpecies(id: Long, species: Species, today: String) = database.withTransaction {
        check(dao.updateSpecies(id, species) == 1)
        dao.deleteDecision(id, today)
    }

    suspend fun lastWatered(plantId: Long): Long? = dao.lastWatered(plantId)

    suspend fun saveDecision(decision: DecisionRecord, lastWateredAt: Long?, species: Species) {
        database.withTransaction {
            if (dao.speciesFor(decision.plantId) == species && dao.lastWatered(decision.plantId) == lastWateredAt) {
                dao.saveDecision(decision)
            }
        }
    }
}
