package com.swanky.stoktakip.models

import com.google.firebase.Timestamp
import java.io.Serializable

data class Sale(
    val id: String,
    val productId: String,
    val customer: String,
    val state: Long,
    val price: Double,
    val updateDate: Timestamp,
    val product: Product ?= null
) : Serializable