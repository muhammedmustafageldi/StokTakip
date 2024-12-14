package com.swanky.stoktakip.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.swanky.stoktakip.R
import com.swanky.stoktakip.adapters.FragmentPagerAdapter
import com.swanky.stoktakip.databinding.ActivitySalesBinding
import com.swanky.stoktakip.models.Product
import com.swanky.stoktakip.models.Sale

class SalesActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalesBinding
    private lateinit var unCompletedList: ArrayList<Sale>
    private lateinit var completedList: ArrayList<Sale>
    private lateinit var db : FirebaseFirestore
    private lateinit var listener: OnResultListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setLoadingScreen(loading = true)
        initDatabaseObjects()
        getSalesData()

        binding.makeSaleButton.setOnClickListener {
            Snackbar.make(it, "Satış için ürünler sayfasına yönlendirileceksiniz.", Snackbar.LENGTH_INDEFINITE)
                .setActionTextColor(ContextCompat.getColor(this, R.color.my_orange))
                .setAction("Devam"){
                startActivity(Intent(this, ProductsActivity:: class.java))
            }.show()
        }
    }

    private fun setTabLayout() {
        val viewPager2 = binding.viewpager2
        val tabLayout = binding.tabLayout

        // Create adapter and bind to viewpager.
        val adapter = FragmentPagerAdapter(this)
        viewPager2.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            when (position) {
                0 -> tab.text = "Beklenen (${unCompletedList.size})"
                1 -> tab.text = "Tamamlanan (${completedList.size})"
            }
        }.attach()
    }

    private fun setLoadingScreen(loading: Boolean) {
        val loadingLinear = binding.loadingLinearSales
        val loadingAnim = binding.loadingAnimSales
        val salesConstraint = binding.salesConstraint

        if (loading) {
            loadingLinear.visibility = View.VISIBLE
        } else {
            loadingLinear.visibility = View.GONE
            loadingAnim.cancelAnimation()
            salesConstraint.visibility = View.VISIBLE
        }

    }

    private fun initDatabaseObjects(){
        db = Firebase.firestore
    }

    private fun getSalesData() {
        val salesList = ArrayList<Sale>()

        db.collection("Sales")
            .orderBy("updateDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                if (result != null) {
                    val sales = result.documents

                    val tasks = mutableListOf<Task<DocumentSnapshot>>()

                    for (sale in sales) {
                        val productId = sale.get("productId") as String

                        val productTask = db.collection("Products").document(productId).get()
                        tasks.add(productTask)

                        productTask.addOnSuccessListener { productResult ->
                            if (productResult != null) {
                                val product = productResult.data

                                if (product != null) {
                                    val productName = product["productName"] as String
                                    val category = product["category"] as String
                                    val imageUrl = product["imageUrl"] as String
                                    val productPrice = product["price"] as Double
                                    val quantity = product["quantity"] as Long
                                    val numberOfSales = product["numberOfSales"] as Long

                                    val currentProduct =
                                        Product(
                                            productId,
                                            category,
                                            productName,
                                            productPrice,
                                            imageUrl,
                                            quantity,
                                            numberOfSales
                                        )

                                    val id = sale.id
                                    val customer = sale.get("customer") as String
                                    val state = sale.get("state") as Long
                                    val price = sale.get("price") as Double
                                    val updateDate = sale.get("updateDate") as Timestamp

                                    val currentSale = Sale(
                                        id,
                                        productId,
                                        customer,
                                        state,
                                        price,
                                        updateDate,
                                        currentProduct
                                    )
                                    salesList.add(currentSale)
                                }
                            }
                        }
                    }

                    Tasks.whenAllComplete(tasks)
                        .addOnSuccessListener {
                            // Set recycler and show data
                            setLoadingScreen(loading = false)
                            // Group products
                            groupProducts(salesList)
                        }
                        .addOnFailureListener {
                            Snackbar.make(
                                binding.root,
                                "Satış verileri alınırken bir hata oluştu.",
                                Snackbar.LENGTH_INDEFINITE
                            ).setAction("Yeniden dene") {
                                getSalesData()
                            }.show()
                        }
                }
            }
    }

    private fun groupProducts(saleList: ArrayList<Sale>) {
        unCompletedList = ArrayList()
        completedList = ArrayList()
        for (sale in saleList) {
            if (sale.state == 0L) unCompletedList.add(sale) else completedList.add(sale)
        }
        setTabLayout()
    }

    fun getCompletedData(): ArrayList<Sale> {
        return completedList
    }

    fun getUnCompletedData(): ArrayList<Sale> {
        return unCompletedList
    }

    fun deleteSale(saleId: String, listener: OnResultListener) {
        this.listener = listener

        db.collection("Sales").document(saleId).get().addOnSuccessListener { saleSnapshot ->
            // Get the Sale object, we get the productId
            val productId = saleSnapshot.getString("productId") ?: return@addOnSuccessListener

            // Before the deletion is performed, we will use productId to update the product information
            db.collection("Sales").document(saleId).delete().addOnSuccessListener {
                // Sales deletion successful, update product data
                val productRef = db.collection("Products").document(productId)

                db.runTransaction { transaction ->
                    val productSnapshot = transaction.get(productRef)

                    // Check if the relevant product is available
                    if (productSnapshot.exists()) {
                        // Get 'numberOfSales' and 'quantity' values
                        val currentNumberOfSales = productSnapshot.getLong("numberOfSales") ?: 0L
                        val currentQuantity = productSnapshot.getLong("quantity") ?: 0L

                        // update numberOfSales and quantity
                        transaction.update(productRef, "numberOfSales", currentNumberOfSales - 1)
                        transaction.update(productRef, "quantity", currentQuantity + 1)
                    }
                }.addOnSuccessListener {
                    // Update successful, send result as feedback
                    listener.resultCallback(1)
                }.addOnFailureListener {
                    // Feedback in case of error
                    listener.resultCallback(0)
                }
            }.addOnFailureListener {
                // Deletion failed, feedback
                listener.resultCallback(0)
            }
        }.addOnFailureListener {
            // Failed to receive Sale document, error status
            listener.resultCallback(0)
        }
    }

    fun completeSale(saleId: String, listener: OnResultListener){
        this.listener = listener

        val allTask = arrayListOf<Task<*>>()

        val updates = hashMapOf<String, Any>(
            "state" to 1L,
            "updateDate" to Timestamp.now()
        )
        val sale = unCompletedList.firstOrNull { it.id == saleId }

        val saleUpdateTask = db.collection("Sales").document(saleId).update(updates)

        allTask.add(saleUpdateTask)

        Tasks.whenAllComplete(allTask).addOnSuccessListener {
            // Update the item place in the list
            sale?.let {
                completedList.add(it)
            }
            // Notify fragment success
            listener.resultCallback(1)
        }.addOnFailureListener {
            // Notify fragment failed
            listener.resultCallback(0)
        }
    }

    interface OnResultListener{
        fun resultCallback(result: Int)
    }

}