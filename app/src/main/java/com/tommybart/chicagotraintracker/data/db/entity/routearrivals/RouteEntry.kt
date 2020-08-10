package com.tommybart.chicagotraintracker.data.db.entity.routearrivals

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tommybart.chicagotraintracker.internal.TrainLine

@Entity(
    tableName = "route_data",
    indices = [Index(value = ["mapId", "destinationName", "trainLine"], unique = true)]
)
data class RouteEntry(
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null,
    // See note in StationEntry.kt about mapId naming
    val mapId: Int,
    val stationName: String,
    val destinationName: String,
    val trainLine: TrainLine
) {

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        else if (other !is RouteEntry) return false
        return mapId == other.mapId
            && destinationName == other.destinationName
            && trainLine === other.trainLine
    }

    @Ignore
    private var hashCode: Int = 0
    override fun hashCode(): Int {
        if (hashCode == 0) {
            hashCode = 17
            hashCode = 37 * hashCode + trainLine.hashCode()
            hashCode = 37 * hashCode + mapId
            hashCode = 37 * hashCode + destinationName.hashCode()
        }
        return hashCode
    }
}