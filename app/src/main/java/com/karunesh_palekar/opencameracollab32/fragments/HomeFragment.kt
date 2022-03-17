package com.karunesh_palekar.opencameracollab32.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.karunesh_palekar.opencameracollab32.R
import com.karunesh_palekar.opencameracollab32.adapter.HomeAdapter
import com.karunesh_palekar.opencameracollab32.viewmodel.HomeViewModel


class HomeFragment : Fragment() {


    private lateinit var viewModel: HomeViewModel
    lateinit var homeAdapter: HomeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        viewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        view?.findViewById<RecyclerView>(R.id.home_recycler_view)?.apply {
            homeAdapter = HomeAdapter()
            adapter = homeAdapter

        }

        viewModel.getData()

        viewModel._pdf?.observe(viewLifecycleOwner, Observer {
            homeAdapter.setListData(ArrayList(it))
        })

        view?.findViewById<FloatingActionButton>(R.id.homtocameraftnbtn)?.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_camera_fragment)

        }



    }


}