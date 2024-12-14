package com.swanky.stoktakip.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import com.swanky.stoktakip.R
import java.util.Calendar

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        object : CountDownTimer(3000, 1000) {
            override fun onTick(p0: Long) {}

            override fun onFinish() {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }
        }.start()

        checkFirstRun()

    }

    private fun checkFirstRun() {
        val sharedPreferences = getSharedPreferences("com.swanky.stoktakip", Context.MODE_PRIVATE)
        val isFirstRun = sharedPreferences.getBoolean("firstRun", true)

        if (isFirstRun){
            // The app worked for the first time
            val editor = sharedPreferences.edit()
            editor.putBoolean("firstRun", false)

            // Get current time as millis
            val currentTimeMillis = Calendar.getInstance().timeInMillis
            editor.putLong("firstRunTime", currentTimeMillis)
            editor.apply()
        }
    }

}