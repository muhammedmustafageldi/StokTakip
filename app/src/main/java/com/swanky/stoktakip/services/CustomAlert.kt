package com.swanky.stoktakip.services

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import com.swanky.stoktakip.R
import com.swanky.stoktakip.databinding.LayoutWarningDailogBinding

class CustomAlert(
    val context: Context,
    private val title: String,
    private val message: String,
    private val positiveText: String,
    private val negativeText: String,
    private val icon: Int
) {

    fun createDialog(): HashMap<String, Any> {
        val alertDialogBuilder = AlertDialog.Builder(context, R.style.DialogTheme)
        val dialogBinding = LayoutWarningDailogBinding.inflate(LayoutInflater.from(context))

        alertDialogBuilder.setView(dialogBinding.root)

        // Customize the dialog
        dialogBinding.alertTitleTxt.text = title
        dialogBinding.alertMessageTxt.text = message
        dialogBinding.alertIcon.setImageResource(icon)
        dialogBinding.buttonPositive.text = positiveText
        dialogBinding.buttonNegative.text = negativeText

        val alertDialog = alertDialogBuilder.create()

        if (alertDialog.window != null) {
            alertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        // Return alert and binding
        return hashMapOf(
            "alertDialog" to alertDialog,
            "dialogBinding" to dialogBinding
        )
    }

}