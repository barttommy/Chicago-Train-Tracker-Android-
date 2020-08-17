package com.tommybart.chicagotraintracker.ui.activities.main.arrivals.arrivalsstate

import androidx.lifecycle.LiveData
import com.tommybart.chicagotraintracker.data.models.Route
import kotlinx.coroutines.Deferred

/*
 * ArrivalsFragment state after user selects a station manually from searching
 */
class SearchState(
    private val searchMapId: Int
): ArrivalsState {
    override suspend fun getRouteDataAsync(
        arrivalsStateContext: ArrivalsStateContext
    ): Deferred<LiveData<List<Route>>> {
        arrivalsStateContext.arrivalsState = this
        return arrivalsStateContext.arrivalsViewModel.getRouteDataSearchAsync(searchMapId)
    }
}