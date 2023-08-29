package com.ej.ui

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.ej.cameratest.R
import com.ej.cameratest.databinding.FragmentCameraBinding
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraFragment : Fragment() {
    lateinit var binding: FragmentCameraBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_camera,container, false)
        binding.lifecycleOwner = this.viewLifecycleOwner

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imageCaptureButton.setOnClickListener { takePhoto() }
//        binding.videoCaptureButton.setOnClickListener { captureVideo() }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }



    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(requireContext(),
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                activity?.finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            activity?.baseContext!!, it) == PackageManager.PERMISSION_GRANTED
    }
    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
//        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
//            .format(System.currentTimeMillis())
        val name = "photo"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        val imageCaptureCallback: OnImageCapturedCallback = object : OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                // ImageProxy에서 Bitmap으로 변환합니다.
                val bitmap: Bitmap = imageToBitmap(image)

                // Bitmap을 사용하거나 처리합니다.

                // ImageProxy를 닫습니다.
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                // 캡처 오류 처리
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(requireActivity().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()


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
                    binding.cameraImg.setImageBitmap(rotateBM)
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
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onError(exc: ImageCaptureException) {
//                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
//                }
//
//                override fun onImageSaved(output: ImageCapture.OutputFileResults){
//                    try {
//                        val imageUrl = output.savedUri!!
//
//                        var bitmap : Bitmap? = null
//                        try {
//                            bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, imageUrl)
//                        } catch (e: IOException) {
//                            e.printStackTrace()
//                        }
//
//                        val resizedBitmap = Bitmap.createScaledBitmap(bitmap!!, bitmap.getWidth() / 5, bitmap.getHeight() / 5, false);
//                        val outStream = ByteArrayOutputStream()
//                        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 30, outStream)
//                        val byteArray = outStream.toByteArray()
//                        val compressedBitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
//
//                        binding.cameraImg.setImageBitmap(compressedBitmap)
////                        parentFragmentManager.popBackStack()
//
//                    }catch (e: Exception) {
//                        Log.e(TAG, "Photo capture not exist: ${e.message}")
//
//                    }
//
//                }
//            }
        )
    }

    private fun imageToBitmap(image: ImageProxy): Bitmap {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
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
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireActivity()))
    }

    private fun rotateBitmap(bitmap: Bitmap, ): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90F)

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    companion object {

        val TAG = this::class.java.simpleName

        @JvmStatic
        fun newInstance() =
            CameraFragment()

        val PHOTO_IMAGE = "photo_image"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}