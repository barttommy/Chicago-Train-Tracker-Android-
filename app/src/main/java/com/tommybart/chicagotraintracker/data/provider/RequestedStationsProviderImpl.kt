package com.tommybart.chicagotraintracker.data.provider

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.tommybart.chicagotraintracker.internal.LocationPermissionNotGrantedException
import com.tommybart.chicagotraintracker.internal.extensions.asDeferred
import kotlinx.coroutines.Deferred

class RequestedStationsProviderImpl(
    context: Context,
    private val fusedLocationProviderClient: FusedLocationProviderClient,
    private val nearbyStationsProvider: NearbyStationsProvider,
    private val preferenceProvider: PreferenceProvider
) : RequestedStationsProvider {

    private val appContext: Context = context.applicationContext

    override suspend fun hasRequestedStationsChanged(requestedStationMapIds: List<Int>): Boolean {
        val nearbyStationsChanged = try {
            hasNearbyStationsChanged(requestedStationMapIds)
        } catch (e: LocationPermissionNotGrantedException) {
            false
        }
        val defaultPreferenceChanged =
            if (!isUsingDeviceLocation()) {
                when {
                    // First two cases cover when location is initially disabled
                    requestedStationMapIds.isEmpty() -> true
                    requestedStationMapIds.size > 1 -> true
                    else -> hasDefaultPreferenceChanged(requestedStationMapIds[0])
                }
            } else {
                false
            }

        return nearbyStationsChanged || defaultPreferenceChanged
    }

    // TODO: can we avoid doing things twice between this and hasRequestedStationsChanged?
    override suspend fun getNewRequestMapIds(): List<Int>? {
        if (isUsingDeviceLocation()) {
            try {
                val deviceLocation: Location = getLastDeviceLocationAsync().await()
                    ?: return null
                return nearbyStationsProvider.getNearbyStationMapIds(deviceLocation)
            } catch (e: LocationPermissionNotGrantedException) {
                return null
            }
        } else {
            return listOf(getCustomDefaultStation())
        }
    }

    private suspend fun hasNearbyStationsChanged(requestedStationMapIds: List<Int>): Boolean {
        if (!isUsingDeviceLocation()) return false
        val deviceLocation: Location = getLastDeviceLocationAsync().await() ?: return false
        return requestedStationMapIds != nearbyStationsProvider.getNearbyStationMapIds(
            deviceLocation
        )
    }

    private fun getLastDeviceLocationAsync(): Deferred<Location?> {
        return if (hasLocationPermission())
            fusedLocationProviderClient.lastLocation.asDeferred()
        else
            throw LocationPermissionNotGrantedException()
    }

    private fun isUsingDeviceLocation(): Boolean {
        return preferenceProvider.isAllowingDeviceLocation()
    }

    private fun hasDefaultPreferenceChanged(mapId: Int): Boolean {
        return getCustomDefaultStation() != mapId
    }

    private fun getCustomDefaultStation(): Int {
        return preferenceProvider.getDefaultStation()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}