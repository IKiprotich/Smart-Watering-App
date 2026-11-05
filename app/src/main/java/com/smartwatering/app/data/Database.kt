package com.smartwatering.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import com.smartwatering.app.model.Species
import kotlinx.coroutines.flow.Flow

class DatabaseConverters {
    @TypeConverter
    fun speciesToStorage(value: Species): String = value.name

    @TypeConverter
    fun speciesFromStorage(value: String): Species = Species.valueOf(value)

    @TypeConverter
    fun checkInTypeToStorage(value: CheckInType): String = value.name

    @TypeConverter
    fun checkInTypeFromStorage(value: String): CheckInType = CheckInType.valueOf(value)
}

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val species: Species
)

enum class CheckInType(val label: String) {
    WATERED("Watered"),
    SOIL_DRY("Soil feels dry"),
    SOIL_DAMP("Soil feels damp"),
    DROOPING("Leaves drooping"),
    LOOKS_FINE("Looks fine")
}

@Entity(tableName = "check_ins")
data class CheckIn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long,
    val type: CheckInType,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "decisions", primaryKeys = ["plantId", "date"])
data class DecisionRecord(
    val plantId: Long,
    val date: String,
    val water: Boolean,
    val reason: String,
    val features: String = ""
)

@Dao
interface AppDao {
    @Query("SELECT * FROM plants ORDER BY name")
    fun plants(): Flow<List<Plant>>

    @Query("SELECT * FROM plants")
    suspend fun allPlants(): List<Plant>

    @Query("SELECT * FROM plants WHERE id = :id")
    fun plant(id: Long): Flow<Plant?>

    @Insert
    suspend fun insertPlant(plant: Plant): Long

    @Query("DELETE FROM plants WHERE id = :id")
    suspend fun deletePlantRecord(id: Long)

    @Query("DELETE FROM check_ins WHERE plantId = :id")
    suspend fun deletePlantCheckIns(id: Long)

    @Query("DELETE FROM decisions WHERE plantId = :id")
    suspend fun deletePlantDecisions(id: Long)

    @Insert
    suspend fun insertCheckInAndReturnId(checkIn: CheckIn): Long

    @Query("DELETE FROM check_ins WHERE id = :id")
    suspend fun deleteCheckIn(id: Long)

    @Query("SELECT MAX(timestamp) FROM check_ins WHERE plantId = :id AND type = 'WATERED'")
    suspend fun lastWatered(id: Long): Long?

    @Query("SELECT * FROM check_ins WHERE plantId = :id ORDER BY timestamp DESC")
    fun history(id: Long): Flow<List<CheckIn>>

    @Upsert
    suspend fun saveDecision(record: DecisionRecord)

    @Query("SELECT * FROM decisions WHERE date = :date")
    fun decisions(date: String): Flow<List<DecisionRecord>>

    @Query("SELECT * FROM check_ins ORDER BY timestamp")
    suspend fun allCheckIns(): List<CheckIn>

    @Query("SELECT * FROM decisions ORDER BY date")
    suspend fun allDecisions(): List<DecisionRecord>
}

@Database(
    entities = [Plant::class, CheckIn::class, DecisionRecord::class],
    version = 1
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "smartwatering.db"
            ).build().also { instance = it }
        }
    }
}
