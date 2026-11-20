package com.smartwatering.app.data

import kotlinx.coroutines.flow.Flow
import com.smartwatering.app.model.Species
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
            require(name.isNotBlank())
            val id = dao.insertPlant(Plant(name = name.trim(), species = species))
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

    suspend fun lastWatered(plantId: Long): Long? = dao.lastWatered(plantId)

    suspend fun saveDecision(decision: DecisionRecord, lastWateredAt: Long?) {
        database.withTransaction {
            if (dao.plantExists(decision.plantId) && dao.lastWatered(decision.plantId) == lastWateredAt) {
                dao.saveDecision(decision)
            }
        }
    }
}
