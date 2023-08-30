package com.ej.ui

import android.content.ContentValues
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.ej.cameratest.*
import com.ej.cameratest.R
import com.ej.cameratest.databinding.FragmentCameraMlBinding
import com.ej.cameratest.objectdetector.ObjectGraphic
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.camera.CameraSourceConfig
import com.google.mlkit.vision.camera.CameraXSource
import com.google.mlkit.vision.camera.DetectionTaskCallback
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraMlFragment : Fragment() {

    lateinit var binding: FragmentCameraMlBinding

    private var previewView: PreviewView? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var cameraXSource: CameraXSource? = null
    private var customObjectDetectorOptions: CustomObjectDetectorOptions? = null
    private var lensFacing: Int = CameraSourceConfig.CAMERA_FACING_BACK
    private var targetResolution: Size? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_camera_ml,container, false)
        binding.lifecycleOwner = this.viewLifecycleOwner

        startCamera()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        previewView = binding.previewView
        graphicOverlay = binding.graphicOverlay

        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    override fun onResume() {
        super.onResume()
        if (cameraXSource != null &&
            PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(
                requireContext(),
                localModel
            )
                .equals(customObjectDetectorOptions) &&
            PreferenceUtils.getCameraXTargetResolution(
                requireActivity().applicationContext,
                lensFacing
            ) != null &&
            (Objects.requireNonNull(
                PreferenceUtils.getCameraXTargetResolution(
                    requireActivity().applicationContext,
                    lensFacing
                )
            ) == targetResolution)
        ) {
            cameraXSource!!.start()
        } else {
            createThenStartCameraXSource()
        }
    }

    override fun onPause() {
        super.onPause()
        if (cameraXSource != null) {
            cameraXSource!!.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (cameraXSource != null) {
            cameraXSource!!.stop()
        }
        cameraExecutor.shutdown()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())


        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview : Preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                val cameraControl = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

                // 카메라 줌 비율 1이면 최대 줌
//                cameraControl.cameraControl.setLinearZoom(10/100f)

            } catch(exc: Exception) {
                Log.e(CameraFragment.TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireActivity()))
    }

    private fun createThenStartCameraXSource() {
        if (cameraXSource != null) {
            cameraXSource!!.close()
        }
        customObjectDetectorOptions =
            PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(
                requireActivity().applicationContext,
                localModel
            )
        val objectDetector: ObjectDetector = ObjectDetection.getClient(customObjectDetectorOptions!!)
        var detectionTaskCallback: DetectionTaskCallback<List<DetectedObject>> =
            DetectionTaskCallback<List<DetectedObject>> { detectionTask ->
                detectionTask
                    .addOnSuccessListener { results -> onDetectionTaskSuccess(results) }
                    .addOnFailureListener { e -> onDetectionTaskFailure(e) }
            }
        val builder: CameraSourceConfig.Builder =
            CameraSourceConfig.Builder(requireContext().applicationContext, objectDetector!!, detectionTaskCallback)
                .setFacing(lensFacing)
        targetResolution =
            PreferenceUtils.getCameraXTargetResolution(
                requireContext().applicationContext,
                lensFacing
            )
        if (targetResolution != null) {
            builder.setRequestedPreviewSize(targetResolution!!.width, targetResolution!!.height)
        }
        cameraXSource = CameraXSource(builder.build(), previewView!!)
        needUpdateGraphicOverlayImageSourceInfo = true
        cameraXSource!!.start()
    }


    private fun onDetectionTaskSuccess(results: List<DetectedObject>) {
        graphicOverlay!!.clear()
        if (needUpdateGraphicOverlayImageSourceInfo) {
            val size: Size = cameraXSource!!.getPreviewSize()!!
            if (size != null) {
                Log.d(TAG, "preview width: " + size.width)
                Log.d(TAG, "preview height: " + size.height)
                val isImageFlipped =
                    cameraXSource!!.getCameraFacing() == CameraSourceConfig.CAMERA_FACING_FRONT
                if (isPortraitMode) {
                    // Swap width and height sizes when in portrait, since it will be rotated by
                    // 90 degrees. The camera preview and the image being processed have the same size.
                    graphicOverlay!!.setImageSourceInfo(size.height, size.width, isImageFlipped)
                } else {
                    graphicOverlay!!.setImageSourceInfo(size.width, size.height, isImageFlipped)
                }
                needUpdateGraphicOverlayImageSourceInfo = false
            } else {
                Log.d(TAG, "previewsize is null")
            }
        }
        Log.v(TAG, "Number of object been detected: " + results.size)
        for (result in results) {
            val boundingBox = result.boundingBox
            for (label in result.labels) {
                if(label.index == 122) {
                    graphicOverlay!!.add(ObjectGraphic(graphicOverlay!!, result))
                    takePhoto()
                }
            }
        }
        graphicOverlay!!.add(InferenceInfoGraphic(graphicOverlay!!))
        graphicOverlay!!.postInvalidate()
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireActivity()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    val bitmap: Bitmap = imageToBitmap(image)
//                    binding.cameraImg.setImageBitmap(bitmap)

                    val resizedBitmap = Bitmap.createScaledBitmap(bitmap!!, bitmap.getWidth() / 5, bitmap.getHeight() / 5, false);

                    val outStream = ByteArrayOutputStream()
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 30, outStream)
                    val byteArray = outStream.toByteArray()
                    val compressedBitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                    val rotateBM = rotateBitmap(compressedBitmap)
                    binding.imgCapture.setImageBitmap(rotateBM)
                    // Bitmap을 사용하거나 처리합니다.

                    // ImageProxy를 닫습니다.

                    // Bitmap을 사용하거나 처리합니다.

                    // ImageProxy를 닫습니다.
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                }
            }
        )
    }
    private fun rotateBitmap(bitmap: Bitmap, ): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90F)

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun imageToBitmap(image: ImageProxy): Bitmap {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun onDetectionTaskFailure(e: Exception) {
        graphicOverlay!!.clear()
        graphicOverlay!!.postInvalidate()
        val error = "Failed to process. Error: " + e.localizedMessage
        Toast.makeText(
            graphicOverlay!!.getContext(),
            """
   $error
   Cause: ${e.cause}
      """.trimIndent(),
            Toast.LENGTH_SHORT
        )
            .show()
        Log.d(TAG, error)
    }

    private val isPortraitMode: Boolean
        private get() =
            (requireActivity().applicationContext.resources.configuration.orientation !==
                    Configuration.ORIENTATION_LANDSCAPE)

    companion object {
        val TAG = this::class.java.simpleName
        fun newInstance() = CameraMlFragment()

        private val localModel: LocalModel =
            LocalModel.Builder().setAssetFilePath("custom_models/object_labeler.tflite").build()
    }
}