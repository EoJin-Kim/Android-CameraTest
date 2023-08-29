package com.ej.cameratest

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.ContextUtil.getApplicationContext
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ej.cameratest.databinding.FragmentCameraBinding
import com.ej.cameratest.databinding.FragmentCameraMlBinding
import com.ej.cameratest.objectdetector.ObjectDetectorProcessor
import com.ej.cameratest.objectdetector.ObjectGraphic
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.camera.CameraSourceConfig
import com.google.mlkit.vision.camera.CameraXSource
import com.google.mlkit.vision.camera.DetectionTaskCallback
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import java.util.*


class CameraMlFragment : Fragment() {

    lateinit var binding: FragmentCameraMlBinding
    lateinit var viewModel : CameraMlViewModel

    private var previewView: PreviewView? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var cameraXSource: CameraXSource? = null
    private var customObjectDetectorOptions: CustomObjectDetectorOptions? = null
    private var lensFacing: Int = CameraSourceConfig.CAMERA_FACING_BACK
    private var targetResolution: Size? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_camera_ml,container, false)
        binding.lifecycleOwner = this.viewLifecycleOwner


        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[CameraMlViewModel::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        previewView = binding.previewView
        graphicOverlay = binding.graphicOverlay
    }

    override fun onResume() {
        super.onResume()
        if (cameraXSource != null &&
            PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(requireContext(), localModel)
                .equals(customObjectDetectorOptions) &&
            PreferenceUtils.getCameraXTargetResolution(requireActivity().applicationContext, lensFacing) != null &&
            (Objects.requireNonNull(
                PreferenceUtils.getCameraXTargetResolution(requireActivity().applicationContext, lensFacing)
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
            PreferenceUtils.getCameraXTargetResolution(requireContext().applicationContext, lensFacing)
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
        for (`object` in results) {
            graphicOverlay!!.add(ObjectGraphic(graphicOverlay!!, `object`))
        }
        graphicOverlay!!.add(InferenceInfoGraphic(graphicOverlay!!))
        graphicOverlay!!.postInvalidate()
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