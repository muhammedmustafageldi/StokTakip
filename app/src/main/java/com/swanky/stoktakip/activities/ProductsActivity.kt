package com.swanky.stoktakip.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.swanky.stoktakip.adapters.ProductsAdapter
import com.swanky.stoktakip.databinding.ActivityProductsBinding
import com.swanky.stoktakip.models.Product
import java.lang.Exception
import android.view.inputmethod.InputMethodManager
import android.widget.ToggleButton
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.swanky.stoktakip.R
import com.swanky.stoktakip.adapters.CategoriesAdapter
import com.swanky.stoktakip.databinding.FilterBottomSheetBinding
import com.swanky.stoktakip.models.CategoryFilters
import com.swanky.stoktakip.models.SortFilters

class ProductsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductsBinding
    private lateinit var productList: ArrayList<Product>
    private lateinit var productsAdapter: ProductsAdapter
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private var sortFilters = SortFilters()
    private lateinit var categoryFilters : ArrayList<CategoryFilters>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registerLauncher()
        setLoadingScreen(true)
        getDataFromDatabase()

        searchInit()

        binding.addProductFab.setOnClickListener {
            startActivity(Intent(this, AddProductActivity::class.java))
        }

        binding.filterButton.setOnClickListener {
            showBottomFilterDialog()
        }

    }

    private fun registerLauncher() {
        launcher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    try {
                        val deletedProduct =
                            result.data?.getSerializableExtra("deletedProduct") as Product
                        val position = productList.indexOf(deletedProduct)
                        if (position != -1) {
                            productList.removeAt(position)
                            productsAdapter.notifyItemRemoved(position)
                        }
                    } catch (e: Exception) {
                        println(e.message)
                        Toast.makeText(
                            this,
                            "Bir sorunla karşılaşıldı, ürünler tekrar sıralanıyor.",
                            Toast.LENGTH_LONG
                        ).show()
                        val restartIntent = Intent(this, ProductsActivity::class.java)
                        startActivity(restartIntent)
                        finish()
                    }
                }
            }
    }

    private fun getDataFromDatabase() {
        productList = ArrayList()
        val db = Firebase.firestore
        db.collection("Products")
            .get()
            .addOnSuccessListener { result ->

                val products = result.documents

                for (product in products) {
                    val id = product.id
                    val productName = product.get("productName") as String
                    val category = product.get("category") as String
                    val imageUrl = product.get("imageUrl") as String
                    val price = product.get("price") as Double
                    val quantity = product.get("quantity") as Long
                    val numberOfSales = product.get("numberOfSales") as Long

                    val currentProduct =
                        Product(id, category, productName, price, imageUrl, quantity, numberOfSales)
                    productList.add(currentProduct)
                }
                // Subsequent processing
                getCategoriesData()
                setLoadingScreen(false)
                setRecyclerView(productList)
            }.addOnFailureListener {
                println(it.message)
            }

    }

    private fun setRecyclerView(productList: ArrayList<Product>) {
        val recyclerView = binding.productsRecyclerView
        productsAdapter =
            ProductsAdapter(context = this, productList = productList, launcher = launcher)

        recyclerView.apply {
            this.adapter = productsAdapter
            this.layoutManager = GridLayoutManager(this@ProductsActivity, 2, GridLayoutManager.VERTICAL, false)
            this.setHasFixedSize(true)
        }
    }

    private fun setLoadingScreen(loading: Boolean) {
        val loadingLinear = binding.loadingLinear
        val loadingAnim = binding.loadingAnim
        val productConst = binding.productsConst

        if (loading) {
            loadingLinear.visibility = View.VISIBLE
        } else {
            loadingLinear.visibility = View.GONE
            loadingAnim.cancelAnimation()
            productConst.visibility = View.VISIBLE
        }
    }

    private fun searchInit() {
        val searchView = binding.searchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(searchString: String): Boolean {
                searchTransaction(searchString)
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchView.windowToken, 0)
                return true
            }

            override fun onQueryTextChange(searchString: String): Boolean {
                searchTransaction(searchString)
                return true
            }
        })
    }

    private fun searchTransaction(searchValue: String) {
        val searchedList = ArrayList<Product>()
        for (product in productList) {
            if (product.productName.lowercase().contains(searchValue.lowercase())) {
                searchedList.add(product)
            }
        }

        productsAdapter.setProductList(searchedList)

        if (searchedList.isEmpty()) {
            // Not found
            //Close keyboard
            val imm =
                binding.searchView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.searchView.windowToken, 0)

            Toast.makeText(this, "Bu isimde eşleşen ürün bulunamadı.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBottomFilterDialog() {
        val filterDialogBinding = FilterBottomSheetBinding.inflate(LayoutInflater.from(this))

        val filterDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        filterDialog.apply {
            setContentView(filterDialogBinding.root)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            setCancelable(true)
        }

        // Define toggle buttons ->
        val toggleAlphabetical = filterDialogBinding.toggleAlphabetical
        val toggleBestSelling = filterDialogBinding.toggleBestSelling

        // Control selected sort filters and activate toggles
        toggleAlphabetical.isChecked = sortFilters.alphabeticalFilters
        toggleBestSelling.isChecked = sortFilters.bestSellingFilters

        // Set two toggles so that only one is selectable
        selectableValidator(toggleAlphabetical, toggleBestSelling)

        // Set category filter recyclerview
        setCategoriesRecycler(filterDialogBinding)

        filterDialogBinding.buttonApplyFilter.setOnClickListener {
            // List selected categories ->
            val filteredCategories = categoryFilters.filter { it.filterState }.map { it.categoryName }

            // Get sort filters
            val alphabetic = toggleAlphabetical.isChecked
            val bestSelling = toggleBestSelling.isChecked

            // Set selected sort filters ->
            sortFilters.alphabeticalFilters = alphabetic
            sortFilters.bestSellingFilters = bestSelling

            val filteredList = applyFilters(filteredCategories, alphabetic, bestSelling)
            productsAdapter.setProductList(filteredList)
            updateFilterCountTxt()
            filterDialog.dismiss()
        }


        filterDialogBinding.buttonClearFilter.setOnClickListener {
            clearAllFilters(filterBottomBinding = filterDialogBinding)
            filterDialog.dismiss()
        }

        filterDialog.show()
    }

    private fun setCategoriesRecycler(filterDialogBinding: FilterBottomSheetBinding) {
        // Set recyclerview
        val categoriesAdapter = CategoriesAdapter(categoryFilters)
        val categoriesRecycler = filterDialogBinding.categoriesRecycler
        categoriesRecycler.layoutManager = StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)
        categoriesRecycler.adapter = categoriesAdapter
    }

    private fun getCategoriesData() {
        val categoriesList = productList.map { it.category }.toSet().toTypedArray()
        categoryFilters = ArrayList()
        for (category in categoriesList){
            categoryFilters.add(CategoryFilters(category, false))
        }
    }

    private fun applyFilters(filteredCategories: List<String>, alphabetic: Boolean, bestSelling: Boolean): ArrayList<Product> {
        var filteredList = mutableListOf<Product>()

        if (filteredCategories.isEmpty()){
            filteredList.addAll(productList)
        }else{
            filteredCategories.forEach {filter ->
                filteredList.addAll(productList.filter { it.category == filter })
            }
        }

        if (alphabetic) {
            filteredList = filteredList.sortedBy { it.productName }.toMutableList()
        }

        if (bestSelling) {
            filteredList = filteredList.sortedByDescending { it.numberOfSales }.toMutableList()
        }

        return ArrayList(filteredList)
    }

    private fun clearAllFilters(filterBottomBinding: FilterBottomSheetBinding) {
        filterBottomBinding.toggleAlphabetical.isChecked = false
        filterBottomBinding.toggleBestSelling.isChecked = false

        // Reset list in recyclerview
        productsAdapter.setProductList(productList)

        // Reset selected filters list
        sortFilters = SortFilters()

        // Reset categories list and recycler
        getCategoriesData()
        setCategoriesRecycler(filterBottomBinding)

        // Update filter count txt
        updateFilterCountTxt()

        Toast.makeText(this, "Tüm filtreler temizlendi.", Toast.LENGTH_SHORT).show()
    }

    private fun selectableValidator(
        alphabeticToggle: ToggleButton,
        bestSellingToggle: ToggleButton
    ) {
        alphabeticToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                bestSellingToggle.isChecked = false
            }
        }

        bestSellingToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                alphabeticToggle.isChecked = false
            }
        }

    }

    private fun updateFilterCountTxt() {
        val countText = binding.filterCountTextView

        var count = arrayOf(
            sortFilters.alphabeticalFilters,
            sortFilters.bestSellingFilters,
        ).count { it }

        count += categoryFilters.count { it.filterState }

        if (count != 0) countText.visibility = View.VISIBLE else countText.visibility = View.GONE
        countText.text = count.toString()
    }

}