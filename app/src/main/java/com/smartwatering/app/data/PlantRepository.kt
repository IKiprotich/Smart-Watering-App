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

    suspend fun allCheckIns(): List<CheckIn> = dao.allCheckIns()

    suspend fun allDecisions(): List<DecisionRecord> = dao.allDecisions()

    suspend fun addPlant(name: String, species: Species): Long =
        dao.insertPlant(Plant(name = name.trim(), species = species))

    suspend fun logWatering(plantId: Long): Long = dao.insertCheckInAndReturnId(
        CheckIn(plantId = plantId, type = CheckInType.WATERED)
    )

    suspend fun undoCheckIn(id: Long) = dao.deleteCheckIn(id)

    suspend fun removePlant(id: Long) = database.withTransaction {
        dao.deletePlantCheckIns(id)
        dao.deletePlantDecisions(id)
        dao.deletePlantRecord(id)
    }

    suspend fun lastWatered(plantId: Long): Long? = dao.lastWatered(plantId)

    suspend fun saveDecision(decision: DecisionRecord) = dao.saveDecision(decision)
}
