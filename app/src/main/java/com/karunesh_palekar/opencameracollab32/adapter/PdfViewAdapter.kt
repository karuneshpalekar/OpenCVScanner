package com.karunesh_palekar.opencameracollab32.adapter

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.contentValuesOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.karunesh_palekar.opencameracollab32.R
import com.karunesh_palekar.opencameracollab32.fragments.PdfViewFragmentDirections
import com.squareup.picasso.Picasso
import java.io.File

class PdfViewAdapter(listOfString: List<String>) : RecyclerView.Adapter<PdfViewAdapter.ViewHolder>() {

    private var strings: List<String> = listOfString
    var uris: MutableList<Uri> = mutableListOf()
    var listener: RecyclerViewClickListener? = null

    init {
        for (string in strings) {
            val uri = Uri.parse(string)
            uris.add(uri)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewAdapter.ViewHolder {
        val inflater = LayoutInflater.from(parent.context).inflate(R.layout.pdf_view_card_layout, parent, false)
        return ViewHolder(inflater)
    }


    override fun onBindViewHolder(holder: PdfViewAdapter.ViewHolder, position: Int) {
        val photoUri = uris[position]
        holder.itemView.setOnClickListener {
            val action = PdfViewFragmentDirections.actionPdfViewFragmentToPhotoViewDialogFragment(photoUri.toString())
            holder.itemView.findNavController().navigate(action)
        }
        holder.bind(photoUri)
//        holder.imageView.setOnClickListener {
//            listener?.onRecyclerViewItemClicked(it,uris[position].toString())
//        }

    }

    override fun getItemCount(): Int = strings.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val imageView = itemView.findViewById<ImageView>(R.id.itemImages)

        fun bind(photoUri: Uri){

            val uri = File(photoUri.path)

            Glide.with(itemView)
                .load(uri)
                .into(imageView)
           // imageView.setImageURI(photoUri)


        }

    }
}