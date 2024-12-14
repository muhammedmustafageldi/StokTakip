package com.swanky.stoktakip.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.swanky.stoktakip.R
import com.swanky.stoktakip.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.makeSaleCard.setOnClickListener {
            startNewActivity(SalesActivity::class.java)
        }

        binding.productsCard.setOnClickListener {
            startNewActivity(ProductsActivity::class.java)
        }

        binding.supportCard.setOnClickListener {
            goToSupportFromEmail()
        }

        binding.analysisCard.setOnClickListener {
            startNewActivity(AnalysesActivity::class.java)
        }

    }

    private fun startNewActivity(destinationActivity: Class<*>) {
        startActivity(Intent(this, destinationActivity))
    }

    private fun goToSupportFromEmail() {
        val intent = Intent(Intent.ACTION_SENDTO)
        val emailAddress = "mailto:muhammedmustafageldi@gmail.com"
        val subject = "Yardım Talebi"

        val uri = Uri.parse("$emailAddress?subject=${Uri.encode(subject)}")
        intent.data = uri
        startActivity(intent)
    }


}