package com.swanky.stoktakip.activities

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.swanky.stoktakip.R
import com.swanky.stoktakip.adapters.AnalysesAdapter
import com.swanky.stoktakip.databinding.ActivityAnalysesBinding
import com.swanky.stoktakip.models.Analyses
import com.swanky.stoktakip.models.Product
import com.swanky.stoktakip.models.Sale
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AnalysesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalysesBinding
    private lateinit var database: FirebaseFirestore
    private lateinit var salesList: ArrayList<Sale>
    private lateinit var productsList: ArrayList<Product>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setLoadingScreen(loading = true)
        initDatabaseObjects()
        getAnalysesData()
        getRunDate()
    }

    private fun initDatabaseObjects(){
        database = Firebase.firestore
    }

    private fun getAnalysesData(){
        setLoadingScreen(loading = true)
        salesList = ArrayList()
        productsList = ArrayList()
        val allTasks = arrayListOf<Task<*>>()

        val getSalesDataTask = database.collection("Sales").get().addOnSuccessListener {results ->
            if (results != null){
                val sales = results.documents

                for (sale in sales){
                    val id = sale.id
                    val customer = sale.get("customer") as String
                    val state = sale.get("state") as Long
                    val price = sale.get("price") as Double
                    val updateDate = sale.get("updateDate") as Timestamp
                    val productId = sale.get("productId") as String

                    val currentSale = Sale(
                        id,
                        productId,
                        customer,
                        state,
                        price,
                        updateDate)
                    salesList.add(currentSale)
                }
            }
        }

        val getProductsTask = database.collection("Products").get().addOnSuccessListener {result ->
            if (result != null){
                val products = result.documents
                for (product in products){
                    val id = product.id
                    val productName = product.get("productName") as String
                    val category = product.get("category") as String
                    val imageUrl = product.get("imageUrl") as String
                    val price = product.get("price") as Double
                    val quantity = product.get("quantity") as Long
                    val numberOfSales = product.get("numberOfSales") as Long

                    val currentProduct = Product(id, category, productName, price, imageUrl, quantity, numberOfSales)

                    productsList.add(currentProduct)
                }
            }
        }

        allTasks.add(getSalesDataTask)
        allTasks.add(getProductsTask)

        Tasks.whenAllComplete(allTasks).addOnSuccessListener {
            // Do analyses
            doAnalyses()
        }.addOnFailureListener {
            Snackbar.make(binding.root, "Analiz verileri yüklenirken bir hata oluştu.", Snackbar.LENGTH_INDEFINITE).setAction("Yeniden dene"){
                getAnalysesData()
            }.show()
        }

    }

    private fun doAnalyses(){
        // Calculate total sale price
        var totalSales = 0.0
        var totalSalesWithinOneWeek = 0.0
        var totalSalesWithinOneMonth = 0.0
        var totalSalesWithinOneDay = 0.0
        var totalExpectedSales = 0.0
        val totalSaleCount = salesList.size
        var totalQuantityCount = 0.0

        // Take now time as Timestamp
        val currentTimeStamp = Timestamp.now()

        // Calculate times 1 week and 1 month and 1 day ago
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTimeStamp.seconds * 1000 // Convert seconds to milliseconds
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val oneWeekAgo = Timestamp(calendar.time)

        calendar.timeInMillis = currentTimeStamp.seconds * 1000 // Reset calendar
        calendar.add(Calendar.MONTH, -1)
        val oneMonthAgo = Timestamp(calendar.time)

        calendar.timeInMillis = currentTimeStamp.seconds * 1000 // Reset calendar
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val oneDayAgo = Timestamp(calendar.time)

        salesList.forEach {
            totalSales += it.price

            // Calculate expected sale
            if (it.state == 0L){
                totalExpectedSales += it.price
            }

            // Check if the sale is within one week
            if (it.updateDate.toDate().after(oneWeekAgo.toDate())) {
                totalSalesWithinOneWeek += it.price
            }

            // Check if the sale is within one month
            if (it.updateDate.toDate().after(oneMonthAgo.toDate())) {
                totalSalesWithinOneMonth += it.price
            }

            // Check if the sale is within one day
            if (it.updateDate.toDate().after(oneDayAgo.toDate())) {
                totalSalesWithinOneDay += it.price
            }

        }

        // Find max sales product
        var maxSalesProduct: Product? = null
        var maxSales = Long.MIN_VALUE

        productsList.forEach {
            totalQuantityCount += it.quantity
            if (it.numberOfSales > maxSales){
                maxSales = it.numberOfSales
                maxSalesProduct = it
            }
        }

        val maxSalesValue: String = if (maxSalesProduct != null){
            maxSalesProduct!!.productName
        }else{
            "Henüz ürün satışı yapılmadı."
        }

        val analysesData = ArrayList<Analyses>()

        analysesData.add(Analyses("Bugün yapılan işlem tutarı", totalSalesWithinOneDay.toString(), R.drawable.day_icon))
        analysesData.add(Analyses("Ödeme bekleyen satış tutarı", totalExpectedSales.toString(), R.drawable.payment_icon))
        analysesData.add(Analyses("Toplam yapılan satış miktarı", totalSaleCount.toString(), R.drawable.sale))
        analysesData.add(Analyses("Toplam stok sayısı", totalQuantityCount.toString(), R.drawable.stock_icon))
        analysesData.add(Analyses("En çok satılan ürün", maxSalesValue, R.drawable.best_product))

        placeData(totalSales, totalSalesWithinOneWeek, totalSalesWithinOneMonth, analysesData)
        setLoadingScreen(loading = false)
    }

    @SuppressLint("SetTextI18n")
    private fun placeData(totalSale: Double, totalSalesWithinOneWeek: Double, totalSalesWithinOneMonth: Double, analysesData: ArrayList<Analyses>){
        animateTextView(totalSale.toFloat(), binding.totalSaleTxt)
        animateTextView(totalSalesWithinOneWeek.toFloat(), binding.totalSaleWeekTxt)
        animateTextView(totalSalesWithinOneMonth.toFloat(), binding.totalSaleMonthTxt)

        // Set recyclerview
        val recyclerView = binding.analysesRecycler
        val analysesAdapter = AnalysesAdapter(analysesData)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = analysesAdapter
    }

    private fun setLoadingScreen(loading: Boolean){
        val analysesConst = binding.analysesConst
        val analysesAnim = binding.loadingAnimAnalyses
        val loadingLinear = binding.loadingLinearAnalyses

        if (loading){
            analysesConst.visibility = View.GONE
            loadingLinear.visibility = View.VISIBLE
        }else{
            analysesAnim.cancelAnimation()
            loadingLinear.visibility = View.GONE
            analysesConst.visibility = View.VISIBLE
        }
    }

    @SuppressLint("SetTextI18n")
    private fun animateTextView(targetValue: Float, textView: TextView) {
        val startValue = 0f
        val animator = ValueAnimator.ofFloat(startValue, targetValue)
        animator.duration = 3000
        animator.addUpdateListener { animation: ValueAnimator ->
            val animatedValue = animation.animatedValue as Float
            val decimalFormat = DecimalFormat("#,##0")
            val formattedValue: String = decimalFormat.format(animatedValue)
            textView.text = "$formattedValue₺"
        }
        animator.start()
    }

    @SuppressLint("SetTextI18n")
    private fun getRunDate() {
        val sharedPreferences = getSharedPreferences("com.swanky.stoktakip", Context.MODE_PRIVATE)
        val firstRunTime = sharedPreferences.getLong("firstRunTime", 0)

        if (firstRunTime != 0L){
            // Get first run time
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val savedDate = Date(firstRunTime)
            val formattedSavedDate = dateFormat.format(savedDate)

            // Get current time
            val currentTimeMillis = Calendar.getInstance().timeInMillis
            val currentTimeDate = Date(currentTimeMillis)
            val formattedCurrentTime = dateFormat.format(currentTimeDate)

            binding.timeIntervalTxt.text = "$formattedSavedDate ile $formattedCurrentTime arası"
        }
    }

}