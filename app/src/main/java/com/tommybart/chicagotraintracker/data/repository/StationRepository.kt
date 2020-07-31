package com.tommybart.chicagotraintracker.data.repository

import androidx.lifecycle.LiveData
import com.tommybart.chicagotraintracker.data.db.entity.StationEntry

interface StationRepository {
    suspend fun getStationData(): LiveData<List<StationEntry>>
}