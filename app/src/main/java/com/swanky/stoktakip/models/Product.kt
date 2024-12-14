package com.swanky.stoktakip.models

import java.io.Serializable

data class Product(
    var id: String,
    var category: String,
    var productName: String,
    var price: Double,
    var imageUrl: String,
    var quantity: Long,
    var numberOfSales: Long,
) : Serializable