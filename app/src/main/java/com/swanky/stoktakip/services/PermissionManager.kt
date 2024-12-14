package com.swanky.stoktakip.services

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private var permissionCallback: PermissionCallback) {

    fun takeGalleryPermission(context: Context, permissionString: String){
        if (ContextCompat.checkSelfPermission(context, permissionString) != PackageManager.PERMISSION_GRANTED){
            // Request denied ->
            // if show rationale
            if(ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, permissionString)){
                // Show rationale
                permissionCallback.requireRationale()
            }else{
                // No Permission
                permissionCallback.onPermissionDenied()
            }
        }else{
            // Permission is granted.
            permissionCallback.onPermissionGranted()
        }
    }

    fun takeCameraPermission(context: Context){
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            // Request permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, android.Manifest.permission.CAMERA)){
                // Show rationale and request permission
                permissionCallback.requireRationale()
            }else{
                // Request Permission
                permissionCallback.onPermissionDenied()
            }
        }else{
            // Permission is granted go to camera
            permissionCallback.onPermissionGranted()
        }
    }

}