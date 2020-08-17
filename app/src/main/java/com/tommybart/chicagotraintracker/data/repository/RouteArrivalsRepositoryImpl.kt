package com.tommybart.chicagotraintracker.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.tommybart.chicagotraintracker.data.db.RouteArrivalsDao
import com.tommybart.chicagotraintracker.data.db.RouteArrivalsInfoDao
import com.tommybart.chicagotraintracker.data.db.RouteArrivalsRequestDao
import com.tommybart.chicagotraintracker.data.db.entity.RouteArrivalsRequestEntry
import com.tommybart.chicagotraintracker.data.models.Route
import com.tommybart.chicagotraintracker.data.models.Route.CHICAGO_ZONE_ID
import com.tommybart.chicagotraintracker.data.network.chicagotransitauthority.CTA_FETCH_DELAY_MINUTES
import com.tommybart.chicagotraintracker.data.network.chicagotransitauthority.CtaApiResponse
import com.tommybart.chicagotraintracker.data.network.chicagotransitauthority.RouteArrivalsNetworkDataSource
import com.tommybart.chicagotraintracker.data.provider.RequestedStationsProvider
import com.tommybart.chicagotraintracker.internal.extensions.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

class RouteArrivalsRepositoryImpl(
    private val routeArrivalsDao: RouteArrivalsDao,
    private val routeArrivalsInfoDao: RouteArrivalsInfoDao,
    private val routeArrivalsRequestDao: RouteArrivalsRequestDao,
    private val routeArrivalsNetworkDataSource: RouteArrivalsNetworkDataSource,
    private val requestedStationsProvider: RequestedStationsProvider
) : RouteArrivalsRepository {

    init {
        routeArrivalsNetworkDataSource.downloadRouteData.observeForever { ctaApiResponse ->
            persistFetchedData(ctaApiResponse)
        }
    }

    override suspend fun getRouteData(): LiveData<List<Route>> {
        val currentDateTime = ZonedDateTime.now(ZoneId.of(CHICAGO_ZONE_ID)).toLocalDateTime()
        return withContext(Dispatchers.IO) {
            initRouteData(currentDateTime)
            return@withContext Transformations
                .map(routeArrivalsDao.getRoutesWithArrivals()) { routesWithArrivals ->
                    routesWithArrivals.map { it.toRoute() }
                }
        }
    }

    override suspend fun getRouteDataSearch(searchMapId: Int): LiveData<List<Route>> {
        val currentDateTime = ZonedDateTime.now(ZoneId.of(CHICAGO_ZONE_ID)).toLocalDateTime()
        return withContext(Dispatchers.IO) {
            initRouteDataSearch(currentDateTime, searchMapId)
            return@withContext Transformations
                .map(routeArrivalsDao.getRoutesWithArrivals()) { routesWithArrivals ->
                    routesWithArrivals.map { it.toRoute() }
                }
        }
    }

    private suspend fun initRouteData(currentDateTime: LocalDateTime) {
        val lastRequestMapIds = routeArrivalsRequestDao.getLastRequestSync()?.lastRequestMapIds
        if (lastRequestMapIds == null ||
            requestedStationsProvider.hasRequestedStationsChanged(lastRequestMapIds)
        ) {
            Log.d(TAG, "Getting new stations to request arrivals for")
            val requestMapIds = requestedStationsProvider.getNewRequestMapIds()
            requestMapIds?.let { updateRouteData(it, currentDateTime, true) }
        } else {
            Log.d(TAG, "Request has not changed")
            updateRouteData(
                lastRequestMapIds,
                currentDateTime,
                isFetchRouteDataNeeded(currentDateTime)
            )
        }
    }

    private suspend fun initRouteDataSearch(currentDateTime: LocalDateTime, searchMapId: Int) {
        val searchMapIdList = listOf(searchMapId)
        val lastRequestMapIds = routeArrivalsRequestDao.getLastRequestSync()?.lastRequestMapIds
        if (lastRequestMapIds == null || lastRequestMapIds != searchMapIdList) {
            Log.d(TAG, "Search: Requesting data at new search station")
            updateRouteData(searchMapIdList, currentDateTime, true)
        } else {
            Log.d(TAG, "Search: Request has not changed")
            updateRouteData(
                lastRequestMapIds,
                currentDateTime,
                isFetchRouteDataNeeded(currentDateTime)
            )
        }
    }

    private fun isFetchRouteDataNeeded(currentDateTime: LocalDateTime): Boolean {
        val responseInfo = routeArrivalsInfoDao.getRouteArrivalsInfoSync() ?: return true
        val fetchDateTime = responseInfo.transmissionTime
        val delay = currentDateTime.minusMinutes(CTA_FETCH_DELAY_MINUTES)
        Log.d(TAG, "Route: Delay: $delay | Last Update: $fetchDateTime")
        return fetchDateTime.isBefore(delay)
    }

    private suspend fun updateRouteData(
        requestedStationMapIds: List<Int>,
        currentDateTime: LocalDateTime,
        isFetchNeeded: Boolean
    ) {
        deleteOldData(requestedStationMapIds, currentDateTime)
        if (isFetchNeeded) fetchRouteData(requestedStationMapIds)
    }

    private suspend fun fetchRouteData(requestedStationMapIds: List<Int>) {
        Log.d(TAG, "Fetching new route data")
        persistRequest(requestedStationMapIds)
        routeArrivalsNetworkDataSource.fetchRouteData(requestedStationMapIds)
    }

    private fun deleteOldData(requestedStationMapIds: List<Int>, currentDateTime: LocalDateTime) {
        GlobalScope.launch(Dispatchers.IO) {
            var deletedArrivals = routeArrivalsDao.deleteArrivalsAtOldStations(requestedStationMapIds)
            deletedArrivals += routeArrivalsDao.deleteOldArrivals(currentDateTime)
            val deletedRoutes = routeArrivalsDao.deleteRoutesWithoutArrivals()
            Log.d(TAG, "Deleted $deletedArrivals arrivals and $deletedRoutes routes.")
        }
    }

    private fun persistFetchedData(ctaApiResponse: CtaApiResponse) {
        GlobalScope.launch(Dispatchers.IO) {
            routeArrivalsDao.upsertAllRouteArrivals(ctaApiResponse.routeArrivalsList)
            routeArrivalsInfoDao.upsert(ctaApiResponse.routeArrivalsInfo)
        }
    }

    private fun persistRequest(requestedStationMapIds: List<Int>) {
        GlobalScope.launch(Dispatchers.IO) {
            routeArrivalsRequestDao.upsert(RouteArrivalsRequestEntry(requestedStationMapIds))
        }
    }
}