package com.tommybart.chicagotraintracker.internal

import java.io.IOException

class NoNetworkConnectionException : IOException()

class LocationPermissionNotGrantedException : IOException()

// TODO: location permissions, arrivals exceptions(no nearby trains, etc)
