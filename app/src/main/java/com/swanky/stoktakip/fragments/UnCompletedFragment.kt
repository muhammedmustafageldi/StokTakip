package com.swanky.stoktakip.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.swanky.stoktakip.activities.SalesActivity
import com.swanky.stoktakip.adapters.SalesAdapter
import com.swanky.stoktakip.databinding.FragmentUnCompletedBinding


class UnCompletedFragment : Fragment() {

    private lateinit var binding: FragmentUnCompletedBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentUnCompletedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setRecyclerView()
        binding.refreshLayoutUnCompleted.apply {
            setOnRefreshListener {
                setRecyclerView()
                this.isRefreshing = false
                Toast.makeText(requireContext(), "Liste güncellendi.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setRecyclerView() {
        val activity = activity as SalesActivity
        val saleList = activity.getUnCompletedData()

        val saleAdapter = SalesAdapter(context = requireContext(), saleList = saleList)

        val recyclerView = binding.recyclerViewUnCompleted
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = saleAdapter

        saleAdapter.setRemovedListener(object : SalesAdapter.OnItemRemovedListener{
            override fun onItemRemoved(position: Int) {

                activity.deleteSale(saleList[position].id, object: SalesActivity.OnResultListener{
                    override fun resultCallback(result: Int) {
                        if (result == 1){
                            // Delete transaction is success
                            saleList.removeAt(position)
                            saleAdapter.notifyItemRemoved(position)
                            saleAdapter.notifyItemRangeChanged(position, saleList.size)
                            Toast.makeText(requireContext(), "Silme işlemi başarılı.", Toast.LENGTH_SHORT).show()
                        }else{
                            // Delete transaction is failed
                            Toast.makeText(requireContext(), "Silme işlemi başarısız.", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            }
        })

        saleAdapter.setUpdateListener(object: SalesAdapter.OnUpdateStateListener{
            override fun onItemUpdated(position: Int) {

                activity.completeSale(saleList[position].id, object: SalesActivity.OnResultListener{
                    override fun resultCallback(result: Int) {
                        // Remove item and update state
                        if(result == 1){
                            // Update transaction is success
                            saleList.removeAt(position)
                            saleAdapter.notifyItemRemoved(position)
                            Toast.makeText(requireContext(), "Ürün ''tamamlanan'' kısmına gönderildi.", Toast.LENGTH_SHORT).show()
                        }else{
                            Toast.makeText(requireContext(), "Ürün durumu güncelleme işlemi başarısız.", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            }
        })
    }


}