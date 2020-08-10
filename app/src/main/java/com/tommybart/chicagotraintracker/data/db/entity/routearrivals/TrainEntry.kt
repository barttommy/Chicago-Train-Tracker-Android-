package com.tommybart.chicagotraintracker.data.db.entity.routearrivals

import androidx.room.*
import com.tommybart.chicagotraintracker.data.models.Location
import com.tommybart.chicagotraintracker.internal.TrainLine

@Entity(tableName = "train_data", indices = [Index(value = ["id", "runNumber"], unique = true)])
data class TrainEntry(
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null,
    var routeId: Long? = null,
    // See note in StationEntry.kt about mapId naming
    val mapId: Int,
    val runNumber: Int,
    val trainLine: TrainLine,
    val predictionTime: String,
    val arrivalTime: String,
    val isApproaching: Boolean,
    val isDelayed: Boolean,
    val bearing: Double?,
    @Embedded
    val location: Location?
)