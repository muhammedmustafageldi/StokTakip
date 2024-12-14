package com.swanky.stoktakip.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.swanky.stoktakip.databinding.CategoriesRowBinding
import com.swanky.stoktakip.models.CategoryFilters

class CategoriesAdapter(private val categoryFiltersList: ArrayList<CategoryFilters>): RecyclerView.Adapter<CategoriesAdapter.ViewHolder>() {

    class ViewHolder(val binding: CategoriesRowBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
      val binding = CategoriesRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
      return ViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val categoryFilter = categoryFiltersList[position]
        val categoryToggle = holder.binding.recyclerCategoryToggle
        categoryToggle.text = categoryFilter.categoryName
        categoryToggle.textOn = "✓ ${categoryFilter.categoryName}"
        categoryToggle.textOff = categoryFilter.categoryName
        categoryToggle.isChecked = categoryFilter.filterState

        categoryToggle.setOnCheckedChangeListener { _, b ->
            categoryFilter.filterState = b
        }

    }

    override fun getItemCount(): Int {
        return categoryFiltersList.size
    }

}