package com.karunesh_palekar.opencameracollab32.fragments

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.karunesh_palekar.opencameracollab32.ContourIndex
import com.karunesh_palekar.opencameracollab32.R
import com.karunesh_palekar.opencameracollab32.viewmodel.SharedViewModel
import com.karunesh_palekar.opencameracollab32.views.PolygonView
import kotlinx.android.synthetic.main.fragment_scan.*
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class ScanFragment : Fragment() {


    private lateinit var polygonView: PolygonView
    private lateinit var viewModel: SharedViewModel
    private lateinit var sourceFrame: FrameLayout
    private lateinit var nextbtn :Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        polygonView = view.findViewById<PolygonView>(R.id.polygonView)
        sourceFrame = view.findViewById<FrameLayout>(R.id.sourceFrame)
        nextbtn = view.findViewById(R.id.scanButton)
        nextbtn.setBackgroundResource(R.drawable.ic_arrow_forward)
        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

    }

    @SuppressLint("SimpleDateFormat")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)


        val bitmapImage = arguments?.getParcelable<Bitmap>("imageBitmap")
        val ogImage = arguments?.getParcelable<Bitmap>("originalBitmap")

        nextbtn.setOnClickListener {
            val points: List<PointF> = polygonView.getsPoints()
            val src = Mat()
            Utils.bitmapToMat(ogImage, src)
            val final2: Mat = ContourIndex().perspectiveTransform(src, points)
            val final: Mat = ContourIndex().applyThreshold(final2)

            val bm = Bitmap.createBitmap(
                final.width(),
                final.height(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(final, bm)

            val wrapper: ContextWrapper = ContextWrapper(context)
            var file: File = wrapper.getDir("Images", MODE_PRIVATE)
            val fileName = SimpleDateFormat("yyyyMMddHHmms").format(Date())
            file = File(file, "UniqueFileName$fileName.jpg")

            try {
                var stream: OutputStream? = null
                stream = FileOutputStream(file)
                bm.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                stream.flush()
                stream.close()
            } catch (e: Exception) {
                Log.d("Tag", "The exception is $e")
            }

            val savedImageUri: Uri = Uri.parse(file.absolutePath)

            viewModel.sendMessage(savedImageUri.toString())

            findNavController().navigate(R.id.action_scanFragment_to_camera_fragment)
        }

        if (bitmapImage != null && ogImage != null) {

            val points = getContours(bitmapImage, ogImage)
            sourceImageView.setImageBitmap(bitmapImage)

            polygonView.setPoints(points)
            polygonView.visibility = View.VISIBLE
            val padding = resources.getDimension(R.dimen.scanPadding).toInt()
            val layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
                bitmapImage.width + 2 * padding,
                bitmapImage.height + 2 * padding
            )
            layoutParams.gravity = Gravity.CENTER
            polygonView.layoutParams = layoutParams

        }
    }


    private fun getContours(resize: Bitmap, ogImage: Bitmap): MutableMap<Int, PointF> {
        val src = Mat()
        Utils.bitmapToMat(resize, src)
        val gray = Mat(src.rows(), src.cols(), src.type())
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)


        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

//        val srcRes = Mat(src.size(), src.type())
//
//        val samples = Mat(src.rows() * src.cols(), 3, CvType.CV_32F)
//        for (y in 0 until src.rows()) {
//            for (x in 0 until src.cols()) {
//                for (z in 0..2) {
//                    samples.put(x + y * src.cols(), z, src[y, x][z])
//                }
//            }
//        }
//
//        val clusterCount = 2
//        val labels = Mat()
//        val attempts = 5
//        val centers = Mat()
//
//        Core.kmeans(
//            samples,
//            clusterCount,
//            labels,
//            TermCriteria(TermCriteria.MAX_ITER or TermCriteria.EPS, 10000, 0.0001),
//            attempts,
//            Core.KMEANS_PP_CENTERS,
//            centers
//        )
//
//        val dstCenter0: Double = calcWhiteDist(
//            centers[0, 0][0],
//            centers[0, 1][0], centers[0, 2][0]
//        )
//        val dstCenter1: Double = calcWhiteDist(
//            centers[1, 0][0],
//            centers[1, 1][0], centers[1, 2][0]
//        )
//
//        val paperCluster = if (dstCenter0 < dstCenter1) 0 else 1
//
//        for (y in 0 until src.rows()) {
//            for (x in 0 until src.cols()) {
//                val cluster_idx = labels[x + y * src.cols(), 0][0].toInt()
//                if (cluster_idx != paperCluster) {
//                    srcRes.put(y, x, 0.0, 0.0, 0.0, 255.0)
//                } else {
//                    srcRes.put(y, x, 255.0, 255.0, 255.0, 255.0)
//                }
//            }
//        }
//        val bm = Bitmap.createBitmap(
//            srcRes.width(),
//            srcRes.height(),
//            Bitmap.Config.ARGB_8888
//        )
//
//        Utils.matToBitmap(srcRes, bm)
//        return bm


        val kernel = Imgproc.getStructuringElement(
            Imgproc.MORPH_ELLIPSE, Size(
                5.0,
                5.0
            )
        )
        Imgproc.morphologyEx(
            gray,
            gray,
            Imgproc.MORPH_CLOSE,
            kernel
        ) // fill holes
        Imgproc.morphologyEx(
            gray,
            gray,
            Imgproc.MORPH_OPEN,
            kernel
        ) //remove noise
        Imgproc.dilate(gray, gray, kernel)
        val edges = Mat(src.rows(), src.cols(), src.type())
        Imgproc.Canny(gray, edges, 75.0, 200.0)
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            edges, contours, hierarchy, Imgproc.RETR_LIST,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        val idx = ContourIndex().findLargestContour(contours)

//        Imgproc.drawContours(src, contours, -1, Scalar(0.0, 255.0, 0.0), 3)
//        val bm = Bitmap.createBitmap(
//            src.width(),
//            src.height(),
//            Bitmap.Config.ARGB_8888
//        )
//        Utils.matToBitmap(src, bm)
//
//        return bm

        if (idx != null) {

            val largestContour = contours[idx]
            val points = sortPoints(largestContour)

            val pointFs: MutableMap<Int, PointF> = HashMap()

            var index = -1
            for (point: PointF in points) {
                pointFs.put(++index, point)
            }

            return pointFs
        } else {
            val largeContour = contours[-1]

            val points = sortPoints(largeContour)

            val pointFs: MutableMap<Int, PointF> = HashMap()

            var index = -1
            for (point: PointF in points) {
                pointFs.put(++index, point)
            }

            return pointFs
        }
    }
//            val approxCurve = MatOfPoint2f()
//        val contour2f = MatOfPoint2f()
//        largestContour.convertTo(contour2f, CvType.CV_32FC2)
//        val approxDistance = Imgproc.arcLength(contour2f, true) * 0.02
//        Imgproc.approxPolyDP(
//            contour2f,
//            approxCurve,
//            approxDistance,
//            true
//        )
//        Imgproc.drawContours(src, contours, idx, Scalar(0.0, 255.0, 0.0), 3)


//    private fun calcWhiteDist(r: Double, g: Double, b: Double): Double {
//        return sqrt((255 - r).pow(2.0) + (255 - g).pow(2.0) + (255 - b).pow(2.0))
//    }

    private fun sortPoints(largestContours: MatOfPoint): ArrayList<PointF> {

        val point: Array<Point> = largestContours.toArray()
        val sortPoints: Array<Point> = ContourIndex().sortPoints(point)
        val pointFloat: ArrayList<PointF> = ContourIndex().findPoints(sortPoints)

        return pointFloat
    }
}




