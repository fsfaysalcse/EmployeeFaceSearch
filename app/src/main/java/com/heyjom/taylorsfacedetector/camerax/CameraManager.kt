package com.heyjom.taylorsfacedetector.camerax

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.heyjom.taylorsfacedetector.face_detection.FaceContourDetectionListener
import com.heyjom.taylorsfacedetector.face_detection.FaceContourDetectionProcessor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val finderView: PreviewView,
    private val lifecycleOwner: LifecycleOwner,
    private val graphicOverlay: GraphicOverlay,
    private val listener: FaceContourDetectionListener
) {
    private var preview: Preview? = null
    private var camera: Camera? = null
    var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraSelectorOption = CameraSelector.LENS_FACING_FRONT
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null



    fun ImageProxy.toBitmapp(): Bitmap? {
        val planeProxy = planes[0]
        val buffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }


    fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            preview = Preview.Builder().build()

            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, FaceContourDetectionProcessor(
                        graphicOverlay,
                        listener
                    ))
                }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(cameraSelectorOption)
                .build()

            try {
                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
                preview?.setSurfaceProvider(finderView.surfaceProvider)
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun changeCameraSelector() {
        cameraProvider?.unbindAll()
        cameraSelectorOption = if (cameraSelectorOption == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        graphicOverlay.toggleSelector()
        startCamera()
    }

    companion object {
        private const val TAG = "CameraXBasic"
    }
}
