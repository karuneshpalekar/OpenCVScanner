package com.karunesh_palekar.opencameracollab32.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.karunesh_palekar.opencameracollab32.R
import com.karunesh_palekar.opencameracollab32.fragments.HomeFragmentDirections
import com.karunesh_palekar.opencameracollab32.model.Pdf
import kotlinx.android.synthetic.main.home_card_layout.view.*

class HomeAdapter(): RecyclerView.Adapter<HomeAdapter.MyViewHolder>() {


    var items = ArrayList<Pdf>()


    fun setListData(data : ArrayList<Pdf>){
        this.items = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HomeAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context).inflate(R.layout.home_card_layout, parent, false)
        return MyViewHolder(inflater)
    }

    override fun onBindViewHolder(holder: HomeAdapter.MyViewHolder, position: Int) {
        val currentPdf = items[position]
        holder.bind(items[position])

        holder.itemView.card_view.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToPdfViewFragment(currentPdf)
            holder.itemView.findNavController().navigate(action)
        }

    }

    override fun getItemCount(): Int  = items.size

    class MyViewHolder(view:View) : RecyclerView.ViewHolder(view){
        val pdfName = view.findViewById<TextView>(R.id.itemTitle)

        fun bind(data : Pdf){
            pdfName.text = data.name
        }

    }

}
