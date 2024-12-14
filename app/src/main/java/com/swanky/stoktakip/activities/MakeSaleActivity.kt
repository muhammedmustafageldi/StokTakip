package com.swanky.stoktakip.activities

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import com.swanky.stoktakip.R
import com.swanky.stoktakip.databinding.ActivityMakeSaleBinding
import com.swanky.stoktakip.models.Product

class MakeSaleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMakeSaleBinding
    private var state = 2L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMakeSaleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent
        val product = intent.getSerializableExtra("productToSell") as Product

        putDataInPlace(product = product)

        binding.makeSaleBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun putDataInPlace(product: Product){
        val productImage = binding.makeSaleImg
        val productName = binding.makeSaleProductName
        val productPrice = binding.makeSalePriceTxt
        val productPriceLayout = binding.makeSalePriceLayout

        // Put data
        Picasso.get().load(product.imageUrl).placeholder(R.drawable.loading).into(productImage)
        productName.text = product.productName
        productPrice.setText(product.price.toString())
        initDropDown()

        binding.diffirentPriceCheck.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked){
                TransitionManager.beginDelayedTransition(productPriceLayout, AutoTransition())
                productPriceLayout.visibility = View.VISIBLE
            }else{
                TransitionManager.beginDelayedTransition(productPriceLayout, AutoTransition())
                productPriceLayout.visibility = View.GONE
            }
        }

        binding.makeSaleComplete.setOnClickListener {
            completeSale(product.id, binding.customerNameEdit)
        }
    }

    private fun completeSale(productId: String, customerText: TextInputEditText) {
        if (editTextValidator(customerText, binding.makeSalePriceTxt)) {
            // Data is valid
            buttonLoading(loading = true)
            val sale = hashMapOf(
                "customer" to customerText.text.toString(),
                "price" to binding.makeSalePriceTxt.text.toString().toDouble(),
                "productId" to productId,
                "state" to state,
                "updateDate" to Timestamp.now()
            )
            val db = Firebase.firestore
            db.collection("Sales").add(sale).addOnSuccessListener {
                // If the sale is successful
                Toast.makeText(this, "Satış tamamlandı.", Toast.LENGTH_SHORT).show()

                // Update product information in the Products collection
                val productRef = db.collection("Products").document(productId)

                db.runTransaction { transaction ->
                    val productSnapshot = transaction.get(productRef)

                    // Check if the relevant product is available
                    if (productSnapshot.exists()) {
                        // 'numberOfSales' ve 'quantity' değerlerini al ve integer (long) olarak kullan
                        val currentNumberOfSales = productSnapshot.getLong("numberOfSales") ?: 0L
                        val currentQuantity = productSnapshot.getLong("quantity") ?: 0L

                        // update numberOfSales and quantity
                        transaction.update(productRef, "numberOfSales", currentNumberOfSales + 1)
                        transaction.update(productRef, "quantity", currentQuantity - 1)
                    }
                }.addOnSuccessListener {
                    // Sale completed, redirect to main screen
                    val intentToMain = Intent(this, MainActivity::class.java)
                    intentToMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intentToMain)
                    finish()
                }.addOnFailureListener {
                    // Notify the user in case of error
                    Toast.makeText(this, "Ürün güncelleme işlemi başarısız oldu.", Toast.LENGTH_SHORT).show()
                }

            }.addOnFailureListener {
                // Sale transaction failed
                Toast.makeText(this, "Satış işleminde bir hata gerçekleşti.", Toast.LENGTH_SHORT).show()
                buttonLoading(loading = false)
            }
        }
    }

    private fun initDropDown(){
        val res = resources
        val stateStrings = res.getStringArray(R.array.stateArray)
        val arrayAdapter = ArrayAdapter(this, R.layout.dropdown_item, stateStrings)

        binding.autoStateText.apply {
            setAdapter(arrayAdapter)
            setOnItemClickListener{ _, _, position, _ ->
                state = position.toLong()
            }
        }
    }

    private fun editTextValidator(
        customerNameTxt: TextInputEditText,
        priceTxt: TextInputEditText
    ): Boolean {
        val customerName = customerNameTxt.text
        val price = priceTxt.text

        return if (customerName!!.isEmpty()) {
            customerNameTxt.error = "Geçersiz müşteri ismi."
            false
        } else if (price!!.isEmpty() || price.toString().toDouble() < 0) {
            priceTxt.error = "Geçersiz fiyat bilgisi."
            false
        } else if (state == 2L) {
            binding.autoStateText.error = "Lütfen bir seçim yapın."
            false
        } else {
            true
        }
    }

    @SuppressLint("SetTextI18n")
    private fun buttonLoading(loading: Boolean){
        if (loading){
            binding.makeSaleComplete.apply {
                text = ""
                isEnabled = false
            }
            binding.makeSaleProgress.visibility = View.VISIBLE
        }else{
            binding.makeSaleComplete.apply {
                text = "TAMAMLA"
                isEnabled = true
            }
            binding.makeSaleProgress.visibility = View.GONE
        }
    }

}