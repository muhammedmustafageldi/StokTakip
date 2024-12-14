package com.swanky.stoktakip.adapters

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.swanky.stoktakip.databinding.AnalysesRowBinding
import com.swanky.stoktakip.models.Analyses
import java.text.DecimalFormat


class AnalysesAdapter(private val analysesList: ArrayList<Analyses>) :
    RecyclerView.Adapter<AnalysesAdapter.ViewHolder>() {

    class ViewHolder(val binding: AnalysesRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AnalysesRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return analysesList.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.analysesTitle.text = analysesList[position].label
        holder.binding.analysesIcon.setImageResource(analysesList[position].icon)
        if (position != 4) {
            animateTextView(analysesList[position].value.toFloat(), holder.binding.analysesValue)
        } else {
            holder.binding.analysesValue.text = analysesList[position].value
        }
    }

    private fun animateTextView(targetValue: Float, textView: TextView) {
        val startValue = 0f
        val animator = ValueAnimator.ofFloat(startValue, targetValue)
        animator.duration = 3000
        animator.addUpdateListener { animation: ValueAnimator ->
            val animatedValue = animation.animatedValue as Float
            val decimalFormat = DecimalFormat("#,##0")
            val formattedValue: String = decimalFormat.format(animatedValue)
            textView.text = formattedValue
        }
        animator.start()
    }

}

