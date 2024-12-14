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
import com.swanky.stoktakip.databinding.FragmentCompletedBinding

class CompletedFragment : Fragment() {

    private lateinit var binding: FragmentCompletedBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCompletedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setRecyclerView()
        binding.refreshLayoutCompleted.apply {
            setOnRefreshListener {
                setRecyclerView()
                this.isRefreshing = false
                Toast.makeText(requireContext(), "Liste güncellendi.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setRecyclerView() {
        val activity = activity as SalesActivity
        val saleList = activity.getCompletedData()

        val saleAdapter = SalesAdapter(context = requireContext(), saleList = saleList)

        val recyclerView = binding.recyclerViewCompleted
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = saleAdapter
    }

}