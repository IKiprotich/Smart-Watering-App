package com.smartwatering.app.ui.util

import android.content.Context
import android.net.Uri
import com.smartwatering.app.data.PlantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CsvExporter(private val repository: PlantRepository) {
    suspend fun export(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val plants = repository.allPlants().associateBy { it.id }
        val decisions = repository.allDecisions()
        val checkIns = repository.allCheckIns()
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { out ->
            fun quote(value: String) = "\"${value.replace("\"", "\"\"")}\""
            out.write("record,date_time,plant,species,value,reason,features\n")
            decisions.forEach { record ->
                val plant = plants[record.plantId]
                val decision = if (record.water) "water" else "wait"
                val species = plant?.species?.name ?: "?"
                out.write(
                    "decision,${record.date},${quote(plant?.name ?: "?")}," +
                        "$species,$decision,${quote(record.reason)},${quote(record.features)}\n"
                )
            }
            checkIns.forEach { record ->
                val plant = plants[record.plantId]
                val species = plant?.species?.name ?: "?"
                out.write(
                    "check_in,${record.timestamp},${quote(plant?.name ?: "?")}," +
                        "$species,${record.type.name},,\n"
                )
            }
        } ?: error("Could not open the selected file")
    }
}
