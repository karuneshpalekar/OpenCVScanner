package com.karunesh_palekar.opencameracollab32.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.karunesh_palekar.opencameracollab32.R
import com.karunesh_palekar.opencameracollab32.model.Pdf
import com.karunesh_palekar.opencameracollab32.viewmodel.SharedViewModel
import kotlinx.android.synthetic.main.fragment_name_dialog.*

class NameDialogFragment : DialogFragment() {

    private lateinit var viewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_name_dialog, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        return view
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_Dialog_MinWidth)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        view?.findViewById<Button>(R.id.submit_name)?.setOnClickListener {
            //Update with Material Button
            val name = name_of_document.text.toString()
            viewModel.message.observe(viewLifecycleOwner, androidx.lifecycle.Observer {listuri ->
                val pdf = Pdf(0,name,listuri)
                viewModel.addData(pdf)
            })

            findNavController().navigate(R.id.action_camera_fragment_to_homeFragment)

        }

    }

}