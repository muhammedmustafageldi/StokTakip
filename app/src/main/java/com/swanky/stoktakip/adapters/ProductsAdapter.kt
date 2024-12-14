package com.swanky.stoktakip.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.core.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.swanky.stoktakip.R
import com.swanky.stoktakip.activities.ProductDetailsActivity
import com.swanky.stoktakip.databinding.ProductsRowBinding
import com.swanky.stoktakip.models.Product

class ProductsAdapter(private val context: Context, private var productList: ArrayList<Product>, private val launcher: ActivityResultLauncher<Intent>): RecyclerView.Adapter<ProductsAdapter.ViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun setProductList(newList: ArrayList<Product>){
        this.productList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProductsRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = productList[position]

        Picasso.get().load(product.imageUrl).placeholder(R.drawable.loading).into(holder.binding.productsRowImg)
        if (product.quantity == 0L){
            // Product is sold out.
            holder.binding.productsRowImg.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f)})
            holder.binding.soldOutImg.visibility = View.VISIBLE
        }

        holder.binding.productsRowName.text = product.productName
        holder.binding.productsRowPrice.text = product.price.toString() + " ₺"
        holder.binding.productsRowQuantity.text = "Stokta: " + product.quantity

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ProductDetailsActivity:: class.java)
            intent.putExtra("product", product)

            val imagePair = Pair.create<View, String>(holder.binding.productsRowImg, ViewCompat.getTransitionName(holder.binding.productsRowImg) ?: "")

            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                context as Activity,
                imagePair,
            )

            launcher.launch(intent, options)
        }
    }

    class ViewHolder(val binding: ProductsRowBinding): RecyclerView.ViewHolder(binding.root)


}