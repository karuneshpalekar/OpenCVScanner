package com.karunesh_palekar.opencameracollab32.fragments

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.karunesh_palekar.opencameracollab32.R
import kotlinx.android.synthetic.main.photo_view_dialog_fragment.*

class PhotoViewDialogFragment : DialogFragment() {


    private val args by navArgs<PhotoViewDialogFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.photo_view_dialog_fragment, container, false)
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_Dialog_MinWidth)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val bitmap = BitmapFactory.decodeFile(args.uri)
        photo_view.setImageBitmap(bitmap)
    }


}