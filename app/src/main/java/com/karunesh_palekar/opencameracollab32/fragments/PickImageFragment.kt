package com.karunesh_palekar.opencameracollab32.fragments

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.PointF
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.karunesh_palekar.opencameracollab32.ContourIndex
import com.karunesh_palekar.opencameracollab32.R
import com.karunesh_palekar.opencameracollab32.utils.ImagePicker
import kotlinx.android.synthetic.main.fragment_pick_image.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc


class PickImageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pick_image, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        img_pick_btn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                pickImageFromGallery()
            } else {
                val permissionRequest = arrayOf(Manifest.permission.CAMERA)
                requestPermissions(permissionRequest, PERMISSION_CODE)
            }
        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickImageFromGallery()
                } else {
                    Toast.makeText(context, "Permissions not granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_CODE) {
                if (data != null && data.data != null) {
                    val image = data.data
                    val bitmap =  ImagePicker().getBitmap(image,context)
                    scanPhoto(bitmap)
                }
            }
        }

    }

    private fun imageToBitmap(image: Image): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
    }

    private fun scanPhoto( bitmap: Bitmap) {
//        val resize = resizeBitmap(bitmap)
//        val orig = Mat()
//        Utils.bitmapToMat(bitmap, orig)
//        val src = Mat()
//        Utils.bitmapToMat(resize, src)
//        val gray = Mat(src.rows(), src.cols(), src.type())
//        //GrayScaling
//        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
//        //Blurring the image for better detection
//        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
//        //Initiating kernel
//        val kernel = Imgproc.getStructuringElement(
//            Imgproc.MORPH_ELLIPSE, Size(
//                5.0,
//                5.0
//            )
//        )
//        //Performing morphologyEx
//        Imgproc.morphologyEx(
//            gray,
//            gray,
//            Imgproc.MORPH_CLOSE,
//            kernel
//        ) // fill holes
//        Imgproc.morphologyEx(
//            gray,
//            gray,
//            Imgproc.MORPH_OPEN,
//            kernel
//        ) //remove noise
//        //Dilating
//        Imgproc.dilate(gray, gray, kernel)
//        val edges = Mat(src.rows(), src.cols(), src.type())
//        //Finding the edges
//        Imgproc.Canny(gray, edges, 75.0, 200.0)
//        val contours = ArrayList<MatOfPoint>()
//        val hierarchy = Mat()
//        //Getting the contours
//        Imgproc.findContours(
//            edges, contours, hierarchy, Imgproc.RETR_LIST,
//            Imgproc.CHAIN_APPROX_SIMPLE
//        )
//        //Getting the maxContour
//        val idx = ContourIndex().findLargestContour(contours)
//        Toast.makeText(context, "The value of idx is $idx", Toast.LENGTH_LONG).show()
//        Imgproc.drawContours(src, contours, 0, Scalar(0.0, 255.0, 0.0), 3)
//        val approxCurve = MatOfPoint2f()
//        val contour2f = MatOfPoint2f()
//        val largestContour = contours[idx]
//        //Getting the max 4 Points from contours
//        largestContour.convertTo(contour2f, CvType.CV_32FC2)
//        val approxDistance = Imgproc.arcLength(contour2f, true) * 0.02
//        Imgproc.approxPolyDP(
//            contour2f,
//            approxCurve,
//            approxDistance,
//            true
//        )
//
//        //Sorting the points based of their placements
//        val points = sortPoints(largestContour)
//
//        //Performing perspective Transform (Birds eye view)
//        val transformed: Mat = ContourIndex().perspectiveTransform(
//            orig,
//            points
//        )
//
//        Imgproc.cvtColor(transformed, transformed, Imgproc.COLOR_BGR2GRAY)
//        Imgproc.GaussianBlur(transformed, transformed, Size(5.0, 5.0), 0.0)
//        Imgproc.adaptiveThreshold(
//            transformed,
//            transformed,
//            255.0,
//            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
//            Imgproc.THRESH_BINARY,
//            11,
//            2.0
//        )
//
//        val bm = Bitmap.createBitmap(
//            transformed.width(),
//            transformed.height(),
//            Bitmap.Config.ARGB_8888
//        )
//        Utils.matToBitmap(transformed, bm)
//
//        lifecycleScope.launch(Dispatchers.Main) {
//            image_view.setImageBitmap(bm)
//        }
//
//
//    }
//
//    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
//        val maxHeight = 500
//        val ratio = bitmap.height / maxHeight
//        val width = bitmap.width / ratio
//        return Bitmap.createScaledBitmap(bitmap, width, maxHeight, false)
    }

//    private fun sortPoints(largestContours: MatOfPoint): ArrayList<PointF> {
//
//        val point: Array<Point> = largestContours.toArray()
//        val sortPoints: Array<Point> = ContourIndex().sortPoints(point)
////        val pointFloat: ArrayList<PointF> = ContourIndex().findPoints(sortPoints)
//
//        return pointFloat
//    }

    companion object {
        const val IMAGE_PICK_CODE = 1000
        const val PERMISSION_CODE = 1001
    }

}