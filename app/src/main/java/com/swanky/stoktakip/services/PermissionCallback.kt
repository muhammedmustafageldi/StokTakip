package com.swanky.stoktakip.services

interface PermissionCallback {

    fun onPermissionGranted()
    fun onPermissionDenied()
    fun requireRationale()

}