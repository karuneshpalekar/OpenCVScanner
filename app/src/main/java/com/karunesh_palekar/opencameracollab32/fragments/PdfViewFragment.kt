package com.karunesh_palekar.opencameracollab32.fragments

import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.karunesh_palekar.opencameracollab32.R
import com.karunesh_palekar.opencameracollab32.adapter.PdfViewAdapter
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*


class PdfViewFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var cardView: CardView
    private lateinit var toolbar_Pdf: Button
    private lateinit var recyclerAdapter: PdfViewAdapter
    private val document: PdfDocument = PdfDocument()
    private val args by navArgs<PdfViewFragmentArgs>()
    private lateinit var strings : List<String>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_pdf_view, container, false)
        recyclerView = view.findViewById(R.id.pdf_view_recycler_view)
        cardView = view.findViewById(R.id.toolbar_cardView)
        toolbar_Pdf = view.findViewById(R.id.toolbar_pdf)
        toolbar_Pdf.setBackgroundResource(R.drawable.ic_pdf)
        cardView.setBackgroundColor(resources.getColor(R.color.grey))
        strings = args.currentpdf.uris
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val manager = GridLayoutManager(context, 2)
        recyclerView.layoutManager = manager

        recyclerAdapter = PdfViewAdapter(strings)
        recyclerView.adapter = recyclerAdapter
        val itemTouchHelper = ItemTouchHelper(simpleCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)


        toolbar_Pdf.setOnClickListener {

            val uris: List<Uri> = PdfViewAdapter(strings).uris
            val stringss: MutableList<String> = mutableListOf()

            for (uri in uris) {
                stringss.add(uri.toString())
            }

            for (string in stringss) {
                val bitmap = BitmapFactory.decodeFile(string)
                val index = strings.indexOf(string) + 1
                addBitmap(bitmap, index)
            }

            val file: File =
                File(Environment.getExternalStorageDirectory(), "${args.currentpdf.name}.pdf")
            val uri = Uri.fromFile(file)

            try {
                document.writeTo(FileOutputStream(file))

            } catch (e: Exception) {
                e.printStackTrace()
            }


            val intent = Intent()
            intent.setAction(Intent.ACTION_SEND)
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            startActivity(Intent.createChooser(intent, "Share"))

            document.close()

        }
    }


    private var simpleCallback = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or (ItemTouchHelper.DOWN) or (ItemTouchHelper.LEFT) or (ItemTouchHelper.RIGHT),
        0
    ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            var startPosition = viewHolder.adapterPosition
            var endPosition = target.adapterPosition

            Collections.swap(strings, startPosition, endPosition)
            recyclerView.adapter?.notifyItemMoved(startPosition, endPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            // Not to be implemented
        }
    }


    private fun addBitmap(bitmap: Bitmap, pageNumber: Int) {

        val pageInfo =
            PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, pageNumber).create()
        val page = document.startPage(pageInfo)

        val canvas: Canvas = page.canvas
        val paint: Paint = Paint()
        paint.setColor(Color.parseColor("#ffffff"))
        canvas.drawPaint(paint)
        canvas.drawBitmap(bitmap, 0f, 0f, null);
        document.finishPage(page)
    }

}