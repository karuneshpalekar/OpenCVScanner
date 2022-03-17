package com.karunesh_palekar.opencameracollab32.fragments

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.*
import android.hardware.display.DisplayManager
import android.media.Image
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.*
import androidx.annotation.FloatRange
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture.Metadata
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.karunesh_palekar.opencameracollab32.KEY_EVENT_ACTION
import com.karunesh_palekar.opencameracollab32.KEY_EVENT_EXTRA
import com.karunesh_palekar.opencameracollab32.MainActivity
import com.karunesh_palekar.opencameracollab32.R
import com.karunesh_palekar.opencameracollab32.model.Pdf
import com.karunesh_palekar.opencameracollab32.utils.ShortenSeekBarChangeListener
import com.karunesh_palekar.opencameracollab32.utils.simulateClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


typealias LumaListener = (luma: Double) -> Unit


class CameraFragment : Fragment() {


    private val loaderCallback = object : BaseLoaderCallback(context) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.d(TAG, "Successfully loaded ")
                }
                else ->
                    super.onManagerConnected(status)
            }
        }
    }

    private lateinit var container: FrameLayout
    private lateinit var viewFinder: PreviewView
    private lateinit var outputDirectory: File
    private lateinit var broadcastManager: LocalBroadcastManager
    private lateinit var seekBar: SeekBar
    private lateinit var focusButton: ImageButton
    private lateinit var focusRing: ImageView
//    private lateinit var viewmodel: SharedViewModel

    private var displayId: Int = -1
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var flashMode = ImageCapture.FLASH_MODE_OFF


    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    /** Volume down button receiver used to trigger shutter */
    private val volumeDownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(KEY_EVENT_EXTRA, KeyEvent.KEYCODE_UNKNOWN)) {
                // When the volume down button is pressed, simulate a shutter button click
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val shutter = container
                        .findViewById<ImageButton>(R.id.camera_capture_button)
                    shutter.simulateClick()
                }
            }
        }
    }


    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                imageCapture?.targetRotation = view.display.rotation
                imageAnalyzer?.targetRotation = view.display.rotation
            }
        } ?: Unit
    }

    private fun changeFlashMode() {
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            flashMode = when (flashMode) {
                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                else -> ImageCapture.FLASH_MODE_OFF
            }
            imageCapture?.flashMode = flashMode
            updateFlashModeButton()
        }
    }

    private fun updateFlashAvailable(isEnabled: Boolean) {
        focusButton.isEnabled = isEnabled
        updateFlashModeButton()
    }

    private fun updateFlashNotAvailable(isEnabled: Boolean) {
        focusButton.isEnabled = isEnabled
        updateFlashModeUnavailable()
    }

    private fun updateFlashModeUnavailable() {
        focusButton.setImageResource(R.drawable.ic_no_flash)
    }

    private fun updateFlashModeButton() {

        when (flashMode) {
            ImageCapture.FLASH_MODE_ON -> focusButton.setImageResource(R.drawable.ic_flash_on)
            ImageCapture.FLASH_MODE_OFF -> focusButton.setImageResource(R.drawable.ic_flash_off)
        }

    }
//

    override fun onResume() {
        super.onResume()
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.camera_container).navigate(
                CameraFragmentDirections.actionCameraToPermissions()
            )
        }

        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, context, loaderCallback)
        } else {
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()

        cameraExecutor.shutdown()
        broadcastManager.unregisterReceiver(volumeDownReceiver)
        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_camera, container, false)

//    private fun setGalleryThumbnail(uri: Uri) {
//        // Reference of the view that holds the gallery thumbnail
////        val thumbnail = container.findViewById<ImageButton>(R.id.photo_view_button)
//
//        // Run the operations in the view's thread
//        thumbnail.post {
//
//            // Remove thumbnail padding
//            thumbnail.setPadding(resources.getDimension(R.dimen.stroke_small).toInt())
//
//            // Load thumbnail into circular button using Glide
//            Glide.with(thumbnail)
//                .load(uri)
//                .apply(RequestOptions.circleCropTransform())
//                .into(thumbnail)
//        }
//    }

    @SuppressLint("MissingPermission", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view as FrameLayout
        viewFinder = container.findViewById(R.id.view_finder)
//        mSelectionImageView = view.findViewById(R.id.image_view)


//        viewmodel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)



        cameraExecutor = Executors.newSingleThreadExecutor()

        broadcastManager = LocalBroadcastManager.getInstance(view.context)


        val filter = IntentFilter().apply { addAction(KEY_EVENT_ACTION) }
        broadcastManager.registerReceiver(volumeDownReceiver, filter)


        displayManager.registerDisplayListener(displayListener, null)


        outputDirectory = MainActivity.getOutputDirectory(requireContext())

        changeZoomLevel(0f)

        hasFlashUnits()

        viewFinder.post {


            displayId = viewFinder.display.displayId


            updateCameraUi()


            setUpCamera()

        }




    }


    private fun hasFlashUnits() {
        camera?.cameraInfo?.hasFlashUnit()?.let {
            if (it) {
                updateFlashAvailable(it)
            } else {
                updateFlashNotAvailable(it)
            }
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun tapToFocus() {
        viewFinder.setOnTouchListener { _, event ->
            return@setOnTouchListener when (event.action) {
                MotionEvent.ACTION_DOWN -> {
//                    camera?.cameraControl!!.cancelFocusAndMetering()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val meteringPoint = DisplayOrientedMeteringPointFactory(
                        viewFinder.display, camera?.cameraInfo!!,
                        viewFinder.width.toFloat(), viewFinder.height.toFloat()
                    ).createPoint(event.x, event.y)
                    val action = FocusMeteringAction.Builder(meteringPoint).build()
                    camera?.cameraControl!!.startFocusAndMetering(action)
                    val width: Float = focusRing.width.toFloat()
                    val height: Float = focusRing.height.toFloat()
                    focusRing.x = event.x - width / 2
                    focusRing.y = event.y - height / 2

                    focusRing.visibility = View.VISIBLE
                    focusRing.alpha = 1F

                    focusRing.animate()
                        .setStartDelay(200)
                        .setDuration(500)
                        .alpha(0F)
                        .setListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(animation: Animator?) {
                                Log.d(TAG, "Animation Started")
                            }

                            override fun onAnimationEnd(animation: Animator?) {
                                focusRing.visibility = View.INVISIBLE
                            }

                            override fun onAnimationCancel(animation: Animator?) {
                                Log.d(TAG, "Animation Canceled")
                            }

                            override fun onAnimationRepeat(animation: Animator?) {
                                Log.d(TAG, "Animation repeat")
                            }

                        })


                    true
                }
                else -> false
            }
        }

    }



    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Redraw the camera UI controls
        updateCameraUi()

        // Enable or disable switching between cameras
        updateCameraSwitchButton()
    }


    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {

            // CameraProvider
            cameraProvider = cameraProviderFuture.get()

            // Select lensFacing depending on the available cameras
            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }

            // Enable or disable switching between cameras
            updateCameraSwitchButton()


            // Build and bind the camera use cases
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }



    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = viewFinder.display.rotation

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()


        // Preview
        preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()


        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setFlashMode(flashMode)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(screenAspectRatio)

            .setTargetRotation(rotation)
            .build()

        // ImageAnalysis
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, imageAnalyzer
            )
            preview?.setSurfaceProvider(viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }

//        zoom()
        tapToFocus()
    }


    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_4_3
    }

    private fun updateCameraUi() {

        // Remove previous UI if any
        container.findViewById<ConstraintLayout>(R.id.camera_ui_container)?.let {
            container.removeView(it)
        }

        val controls = View.inflate(requireContext(), R.layout.camera_ui_container, container)

        // In the background, load latest photo taken (if any) for gallery thumbnail
//        lifecycleScope.launch(Dispatchers.IO) {
//            outputDirectory.listFiles { file ->
//                EXTENSION_WHITELIST.contains(file.extension.toUpperCase(Locale.ROOT))
//            }?.max()?.let {
//                setGalleryThumbnail(Uri.fromFile(it))
//            }
//        }

        seekBar = controls.findViewById<SeekBar>(R.id.seekBarZoom)
        seekBar.visibility = View.GONE


        focusButton = controls.findViewById<ImageButton>(R.id.camera_flash)

        focusButton.setOnClickListener { changeFlashMode() }


        focusRing = controls.findViewById<ImageView>(R.id.focus_ring)

        controls.findViewById<ImageButton>(R.id.photo_zoom).setOnClickListener {

            seekBar.visibility = View.VISIBLE
            seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener)


        }
        controls.findViewById<ImageButton>(R.id.camera_home).setOnClickListener {
            findNavController().navigate(R.id.action_camera_fragment_to_homeFragment)
        }

        controls.findViewById<ImageButton>(R.id.camera_check).setOnClickListener {


            NameDialogFragment()
                .show(childFragmentManager,"")

//            viewmodel.getData()
//
//            viewmodel._pdf?.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
//                for (pdf in it){
//                    Toast.makeText(context,"$pdf",Toast.LENGTH_SHORT).show()
//                }
//
//            })
////            viewmodel.message.observe(viewLifecycleOwner, androidx.lifecycle.Observer {listuri ->
////                for (uri in listuri){
////                    Toast.makeText(context,"Uri $uri",Toast.LENGTH_SHORT).show()
////                }
////                val pdf = Pdf(0,"First",listuri)
////                viewmodel.addData(pdf)
////                Toast.makeText(context,"Added",Toast.LENGTH_SHORT).show()
////
////            })



        }

        // Listener for button used to capture photo
        controls.findViewById<ImageButton>(R.id.camera_capture_button).setOnClickListener {

            seekBar.visibility = View.INVISIBLE


            // Get a stable reference of the modifiable image capture use case
            imageCapture?.let { imageCapture ->

                imageCapture.takePicture(
                    cameraExecutor,
                    object : ImageCapture.OnImageCapturedCallback() {

                        @SuppressLint("UnsafeExperimentalUsageError")
                        override fun onCaptureSuccess(image: ImageProxy) {
                            image.image?.let {
                                val bitmap = imageToBitmap(it)
//                                val orig = Mat()
//                                Utils.bitmapToMat(bitmap, orig)

                                val resize = resizeBitmap(bitmap)

                                lifecycleScope.launch(Dispatchers.Main) {


                                    findNavController().navigate(
                                        CameraFragmentDirections.actionCameraFragmentToScanFragment(
                                            resize,
                                            bitmap
                                        )
                                    )
                                }

//                                findNavController().navigate(
//                                    CameraFragmentDirections.actionCameraFragmentToHomographyFragment(bitmap)
//                                )


//                                val src = Mat()
//                                Utils.bitmapToMat(resize, src)
//                                val gray = Mat(src.rows(), src.cols(), src.type())
//                                Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
//                                Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
//                                val kernel = Imgproc.getStructuringElement(
//                                    Imgproc.MORPH_ELLIPSE, Size(
//                                        5.0,
//                                        5.0
//                                    )
//                                )
//                                Imgproc.morphologyEx(
//                                    gray,
//                                    gray,
//                                    Imgproc.MORPH_CLOSE,
//                                    kernel
//                                ) // fill holes
//                                Imgproc.morphologyEx(
//                                    gray,
//                                    gray,
//                                    Imgproc.MORPH_OPEN,
//                                    kernel
//                                ) //remove noise
//                                Imgproc.dilate(gray, gray, kernel)
//                                val edges = Mat(src.rows(), src.cols(), src.type())
//                                Imgproc.Canny(gray, edges, 75.0, 200.0)
//                                val contours = ArrayList<MatOfPoint>()
//                                val hierarchy = Mat()
//                                Imgproc.findContours(
//                                    edges, contours, hierarchy, Imgproc.RETR_LIST,
//                                    Imgproc.CHAIN_APPROX_SIMPLE
//                                )
//                                val idx = ContourIndex().findLargestContour(contours)
//                                Imgproc.drawContours(src, contours, idx, Scalar(0.0, 255.0, 0.0), 3)
////                                val approxCurve = MatOfPoint2f()
////                                val contour2f = MatOfPoint2f()
////                                contours[contourIndex].convertTo(contour2f, CvType.CV_32FC2)
////                                val approxDistance = Imgproc.arcLength(contour2f, true) * 0.02
////                                Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true)
////                                val points = MatOfPoint()
////                                val limit :Int = 10000
////                                val limitLong :Long = limit.toLong()
////                                if (approxCurve.total() == limitLong  && Imgproc.contourArea(contours[contourIndex]) > 150) {
////                                    approxCurve.convertTo(points, CvType.CV_32SC2)
////                                }
////                                val rect = Imgproc.boundingRect(points)
////                                Imgproc.rectangle(
////                                    src, Point(rect.x.toDouble(), rect.y.toDouble()),
////                                    Point(
////                                        (rect.x + rect.width).toDouble(),
////                                        (rect.y + rect.height).toDouble()
////                                    ), Scalar(255.0, 0.0, 0.0, 255.0), 3
////                                )
//                                val approxCurve = MatOfPoint2f()
//                                val contour2f = MatOfPoint2f()
//                                val largestContour = contours[idx]
//                                largestContour.convertTo(contour2f, CvType.CV_32FC2)
//                                val approxDistance = Imgproc.arcLength(contour2f, true) * 0.02
//                                Imgproc.approxPolyDP(
//                                    contour2f,
//                                    approxCurve,
//                                    approxDistance,
//                                    true
//                                )
//                                val points = sortPoints(largestContour)
//
//                                val transformed: Mat = ContourIndex().perspectiveTransform(
//                                    orig,
//                                    points
//                                )
//                                Imgproc.cvtColor(transformed, transformed, Imgproc.COLOR_BGR2GRAY)
//                                Imgproc.GaussianBlur(transformed, transformed, Size(5.0, 5.0), 0.0)
//                                Imgproc.adaptiveThreshold(
//                                    transformed,
//                                    transformed,
//                                    255.0,
//                                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
//                                    Imgproc.THRESH_BINARY,
//                                    11,
//                                    2.0
//                                )
//
//                                val bm = Bitmap.createBitmap(
//                                    transformed.width(),
//                                    transformed.height(),
//                                    Bitmap.Config.ARGB_8888
//                                )
//                                Utils.matToBitmap(transformed, bm)
//
//                                lifecycleScope.launch(Dispatchers.Main) {
//                                    imageView.setImageBitmap(bm)
//                                }
                            }
                            super.onCaptureSuccess(image)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            super.onError(exception)
                            Toast.makeText(
                                context,
                                "The error of ImageCaptured is $exception",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })

            }
        }

        // Setup for button used to switch cameras
        controls.findViewById<ImageButton>(R.id.camera_switch_button).let {


            // Disable the button until the camera is set up
            it.isEnabled = false

            // Listener for button used to switch cameras. Only called if the button is enabled
            it.setOnClickListener {
//                changeFlashMode()
                lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                    CameraSelector.LENS_FACING_BACK
                } else {

                    CameraSelector.LENS_FACING_FRONT
                }
                // Re-bind use cases to update selected camera
                bindCameraUseCases()
            }
        }


    }

    /** Enabled or disabled a button to switch cameras depending on the available cameras */
    private fun updateCameraSwitchButton() {
        val switchCamerasButton = container.findViewById<ImageButton>(R.id.camera_switch_button)
        try {
            switchCamerasButton.isEnabled = hasBackCamera() && hasFrontCamera()
        } catch (exception: CameraInfoUnavailableException) {
            switchCamerasButton.isEnabled = false
        }
    }

    private val onSeekBarChangeListener = object : ShortenSeekBarChangeListener() {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            changeZoomLevel(progress / 50f)
        }
    }


    private fun imageToBitmap(image: Image): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
    }

    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    private class LuminosityAnalyzer(listener: LumaListener? = null) : ImageAnalysis.Analyzer {
        private val frameRateWindow = 8
        private val frameTimestamps = ArrayDeque<Long>(5)
        private val listeners = ArrayList<LumaListener>().apply { listener?.let { add(it) } }
        private var lastAnalyzedTimestamp = 0L
        var framesPerSecond: Double = -1.0
            private set


        fun onFrameAnalyzed(listener: LumaListener) = listeners.add(listener)

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }


        override fun analyze(image: ImageProxy) {
            if (listeners.isEmpty()) {
                image.close()
                return
            }

            val currentTime = System.currentTimeMillis()
            frameTimestamps.push(currentTime)

            while (frameTimestamps.size >= frameRateWindow) frameTimestamps.removeLast()
            val timestampFirst = frameTimestamps.peekFirst() ?: currentTime
            val timestampLast = frameTimestamps.peekLast() ?: currentTime
            framesPerSecond = 1.0 / ((timestampFirst - timestampLast) /
                    frameTimestamps.size.coerceAtLeast(1).toDouble()) * 1000.0

            lastAnalyzedTimestamp = frameTimestamps.first

            // Since format in ImageAnalysis is YUV, image.planes[0] contains the luminance plane
            val buffer = image.planes[0].buffer

            // Extract image data from callback object
            val data = buffer.toByteArray()

            // Convert the data into an array of pixel values ranging 0-255
            val pixels = data.map { it.toInt() and 0xFF }

            // Compute average luminance for the image
            val luma = pixels.average()

            // Call all listeners with new value
            listeners.forEach { it(luma) }

            image.close()
        }


//
//                                var maxArea = -1.0
//                                var maxAreaIdx = -1
//                                var tempContour = contours[0]
//                                var approxCurve = MatOfPoint2f()
//                                var largestContour = contours[0]
//                                var largestContours: List<MatOfPoint> = ArrayList()
//                                for (idx in 0..contours.size) {
//                                    tempContour = contours[0]
//                                    val contourArea :Double = Imgproc.contourArea(tempContour)
//
//                                    if (contourArea > maxArea){
//                                        val newMat = MatOfPoint2f(tempContour)
//                                        val contourSize : Int = tempContour.total().toInt()
//                                        val approxCurveTemp = MatOfPoint2f()
//                                        Imgproc.approxPolyDP(newMat,approxCurveTemp,contourSize * 0.02,true)
//                                        if (approxCurveTemp.total().toInt() == 4){
//                                            maxArea = contourArea
//                                            maxAreaIdx = idx
//                                            approxCurve =  approxCurveTemp
//                                            largestContour = tempContour
//                                        }
//                                    }
//
//                                }
//                                Imgproc.drawContours(src,largestContours,-1,Scalar(0.0,255.0,0.0),2)
//
//
//                                        for (contour in contours) {
//                                    val approxCurve = MatOfPoint2f()
//                                    val contour2f = MatOfPoint2f()
//
//                                    contour.convertTo(contour2f, CvType.CV_32FC2)
//                                    val approxDistance = Imgproc.arcLength(contour2f, true) * 0.02
//                                    Imgproc.approxPolyDP(
//                                        contour2f,
//                                        approxCurve,
//                                        approxDistance,
//                                        true
//                                    )
//                                    val points = MatOfPoint()
//                                    approxCurve.convertTo(points, CvType.CV_32SC2)
//                                    val rect = Imgproc.boundingRect(points)
//                                    Imgproc.rectangle(
//                                        src, Point(rect.x.toDouble(), rect.y.toDouble()),
//                                        Point(
//                                            (rect.x + rect.width).toDouble(),
//                                            (rect.y + rect.height).toDouble()
//                                        ), Scalar(255.0, 0.0, 0.0, 255.0), 3
//                                    )
//
//                                }
//                                val mat = Mat()
//                                Utils.bitmapToMat(bitmap,mat)
//                                Imgproc.cvtColor(mat,mat,Imgproc.COLOR_RGB2GRAY)
//
//                                        val binary = Mat(src.rows(), src.cols(), src.type(), Scalar(0.0))
//                                Imgproc.threshold(
//                                    gray,
//                                    binary,
//                                    100.0,
//                                    255.0,
//                                    Imgproc.THRESH_BINARY_INV
//                                )
//                                val contours: List<MatOfPoint> = ArrayList()
//                                val hierarchy = Mat()
//                                Imgproc.findContours(
//                                    binary, contours, hierarchy, Imgproc.RETR_TREE,
//                                    Imgproc.CHAIN_APPROX_SIMPLE
//                                )
//                                val color :Scalar = Scalar(0.0,0.0,255.0)
//                                val point:Point = Point()
//                                Imgproc.drawContours(src,contours,-1,color,2,Imgproc.LINE_8,hierarchy,2,point)

    }

    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
        val maxHeight = 500
        val ratio = bitmap.height / maxHeight
        val width = bitmap.width / ratio
        return Bitmap.createScaledBitmap(bitmap, width, maxHeight, false)
    }

    private fun changeZoomLevel(@FloatRange(from = 0.0, to = 1.0) level: Float) {
        camera?.cameraControl?.setLinearZoom(level)
    }

//    private fun findLargestContours(contours: ArrayList<MatOfPoint>): MatOfPoint2f {
//
//        contours.sortWith(Comparator { o1, o2 ->
//            val area1: Double = Imgproc.contourArea(o1)
//            val area2: Double = Imgproc.contourArea(o2)
//            area2.toInt() - area1.toInt()
//        })
//
//        if (contours.size > 5) {
//            contours.subList(4, contours.size - 1).clear()
//        }
//        var largest: MatOfPoint2f = MatOfPoint2f()
//
//        for (contour: MatOfPoint in contours) {
//            val approx: MatOfPoint2f = MatOfPoint2f()
//            val c: MatOfPoint2f = MatOfPoint2f()
//            val approxDistance = Imgproc.arcLength(c, true) * 0.02
//            contour.convertTo(c, CvType.CV_32FC2)
//            Imgproc.approxPolyDP(c, approx, approxDistance, true)
//
//            if (approx.total().toInt() == 4 && Imgproc.contourArea(contour) > 150) {
//                largest = approx
//                break
//            }
//
//        }
//        return largest
//    }

//    private fun sortPoints(largestContours: MatOfPoint): List<PointF> {
//
//        val point: Array<Point> = largestContours.toArray()
//        val sortPoints: Array<Point> = ContourIndex().sortPoints(point)
//        val pointFloat: MutableList<PointF> = ContourIndex().findPoints(sortPoints)
//
//        return pointFloat
//    }


//                                var tempDouble: DoubleArray = approxCurve.get(0, 0)
//                                val p1: Point = Point(tempDouble[0], tempDouble[1])
//                                circle(src, Point(p1.x, p1.y), 20, Scalar(0.0, 255.0, 0.0))
//
//                                tempDouble = approxCurve.get(1, 0)
//                                val p2: Point = Point(tempDouble[0], tempDouble[1])
//                                circle(src, Point(p2.x, p2.y), 20, Scalar(0.0, 255.0, 0.0))
//
//                                tempDouble = approxCurve.get(2, 0)
//                                val p3: Point = Point(tempDouble[0], tempDouble[1])
//                                circle(src, Point(p3.x, p3.y), 20, Scalar(0.0, 255.0, 0.0))
//
//                                tempDouble = approxCurve.get(3, 0)
//                                val p4: Point = Point(tempDouble[0], tempDouble[1])
//                                circle(src, Point(p4.x, p4.y), 20, Scalar(0.0, 255.0, 0.0))

//                                val points = MatOfPoint()
//                                approxCurve.convertTo(points, CvType.CV_32SC2)
//                                val rect = Imgproc.boundingRect(points)
//                                Imgproc.rectangle(
//                                    src, Point(rect.x.toDouble(), rect.y.toDouble()),
//                                    Point(
//                                        (rect.x + rect.width).toDouble(),
//                                        (rect.y + rect.height).toDouble()
//                                    ), Scalar(255.0, 0.0, 0.0, 255.0), 3
//                                )

    companion object {

        private const val TAG = "CameraXBasic"
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

    }

}

