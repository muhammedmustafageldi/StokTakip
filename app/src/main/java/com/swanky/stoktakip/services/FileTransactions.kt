package com.swanky.stoktakip.services

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class FileTransactions(private val context: Context) {

    // Returns a reduced version of photo
    fun makeSmallerImage(image: Bitmap): Bitmap {
        // Get image dimensions
        var width = image.width
        var height = image.height

        // Is the image horizontal or vertical
        val bitmapRatio = width.toFloat() / height.toFloat()

        if (bitmapRatio > 1) {
            // image is landscape
            width = 300
            height = (width / bitmapRatio).toInt()
        } else {
            // image is portrait
            height = 300
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    // Create empty file in phone for save image
    fun createEmptyFile(): File? {
        // Create a new file and return a reference to it
        val uid = UUID.randomUUID()
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return if (storageDir != null) {
            File.createTempFile(uid.toString(), ".jpg", storageDir)
        } else {

            null
        }
    }

    // Save image to empty file
    fun saveImageToFile(file: File, imageBitmap: Bitmap){
        // Write bitmap to file
        val outputStream = FileOutputStream(file)
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
    }

}