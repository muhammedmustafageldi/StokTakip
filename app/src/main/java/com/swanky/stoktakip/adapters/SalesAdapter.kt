package com.swanky.stoktakip.adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.squareup.picasso.Picasso
import com.swanky.stoktakip.R
import com.swanky.stoktakip.databinding.LayoutWarningDailogBinding
import com.swanky.stoktakip.databinding.SalesRowBinding
import com.swanky.stoktakip.models.Sale
import com.swanky.stoktakip.services.CustomAlert
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SalesAdapter(val context: Context, private val saleList: ArrayList<Sale>): RecyclerView.Adapter<SalesAdapter.ViewHolder>() {

    private var itemRemovedListener: OnItemRemovedListener? = null
    private var itemStateUpdateListener: OnUpdateStateListener? = null

    fun setRemovedListener(listener: OnItemRemovedListener){
        this.itemRemovedListener = listener
    }

    fun setUpdateListener(listener: OnUpdateStateListener){
        this.itemStateUpdateListener = listener
    }

    class ViewHolder(val binding: SalesRowBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SalesRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return saleList.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sale = saleList[position]
        val product = sale.product

        // Put the data in place
        Picasso.get().load(product?.imageUrl).placeholder(R.drawable.loading).into(holder.binding.productImgSalesRow)

        if (sale.state == 0L){
            holder.binding.statusTxtSales.text = "Ödeme Bekleniyor..."
            holder.binding.statusTxtSales.setTextColor(ContextCompat.getColor(context, R.color.my_orange))
            // Define expandable items click listener
            holder.itemView.setOnClickListener {
                setExpandableLayout(holder.binding, position)
                holder.binding.deleteSaleButton.setOnClickListener { deleteSale(position = position, statusTxt = holder.binding.statusTxtSales) }
                holder.binding.updateStateButton.setOnClickListener { updateState(position = position) }
            }
        }else{
            holder.binding.statusTxtSales.text = "Tamamlandı."
            holder.binding.statusTxtSales.setTextColor(ContextCompat.getColor(context, R.color.my_green))
        }

        // Place other data in fields
        holder.binding.productNameSalesRow.text = product!!.productName
        holder.binding.customerTxtSales.text = "Satın alan: ${sale.customer}"
        holder.binding.dateTxtSales.text = "Son işlem tarihi: ${convertTimStampToDate(sale.updateDate)}"
        holder.binding.priceTxtSaleRow.text = "Fiyat: ${sale.price} ₺"
    }

    @SuppressLint("SetTextI18n")
    private fun deleteSale(position: Int, statusTxt: TextView) {
        val alert = CustomAlert(context, "Satışı Sil",
            "Seçilen satış silinecek. Onaylıyor musunuz?\n(Bu işlem geri alınamaz.)",
            "Evet", "Vazgeç", R.drawable.ico_delete)

        val dialogAndBinding = alert.createDialog()
        val alertDialog = dialogAndBinding["alertDialog"] as AlertDialog
        val dialogBinding = dialogAndBinding["dialogBinding"] as LayoutWarningDailogBinding

        dialogBinding.buttonPositive.setOnClickListener {
            // Notify fragment that item has been deleted
            statusTxt.text = "Siliniyor..."
            itemRemovedListener.let {
                it?.onItemRemoved(position = position)
            }
            alertDialog.cancel()
        }

        dialogBinding.buttonNegative.setOnClickListener {
            alertDialog.cancel()
        }

        alertDialog.show()
    }

    private fun updateState(position: Int) {
        val alert = CustomAlert(context, "Satışı Tamamla",
        "Seçilen satış işlemi tamamlandı olarak işaretlensin mi?",
        "Evet", "Vazgeç", R.drawable.sale)

        val dialogAndBinding = alert.createDialog()
        val alertDialog = dialogAndBinding["alertDialog"] as AlertDialog
        val dialogBinding = dialogAndBinding["dialogBinding"] as LayoutWarningDailogBinding

        dialogBinding.buttonPositive.setOnClickListener {
            // Notify fragment that item has been deleted
            itemStateUpdateListener.let {
                it?.onItemUpdated(position = position)
            }
            alertDialog.cancel()
        }

        dialogBinding.buttonNegative.setOnClickListener {
            alertDialog.cancel()
        }

        alertDialog.show()
    }

    private fun convertTimStampToDate(timestamp: Timestamp): String{
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = Date(timestamp.toDate().time)
        return dateFormat.format(date)
    }

    private fun setExpandableLayout(binding: SalesRowBinding, position: Int){
        val targetLayout = binding.expandableConst
        if (targetLayout.visibility == View.GONE){
            TransitionManager.beginDelayedTransition(binding.root, AutoTransition())
            targetLayout.visibility = View.VISIBLE
        }else{
            TransitionManager.beginDelayedTransition(binding.root, AutoTransition())
            targetLayout.visibility = View.GONE
            notifyItemChanged(position)
        }
    }

    interface OnUpdateStateListener{
        fun onItemUpdated(position: Int)
    }

    interface OnItemRemovedListener{
        fun onItemRemoved(position: Int)
    }

}