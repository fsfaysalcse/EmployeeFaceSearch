package com.heyjom.taylorsfacedetector.face_detection

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.heyjom.taylorsfacedetector.camerax.BaseImageAnalyzer
import com.heyjom.taylorsfacedetector.camerax.GraphicOverlay
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

interface FaceContourDetectionListener {
    fun onFaceDetected(faces: List<Face>,captureBitmap: Bitmap)
    fun onErrors(e: Exception)
}

class FaceContourDetectionProcessor(
    private val view: GraphicOverlay,
    private val listener: FaceContourDetectionListener
) : BaseImageAnalyzer<List<Face>>() {

    val highAccuracyOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    private val detector = FaceDetection.getClient(highAccuracyOpts)

    override val graphicOverlay: GraphicOverlay
        get() = view

    override fun detectInImage(image: InputImage): Task<List<Face>> {
        return detector.process(image)
    }

    override fun stop() {
        try {
            detector.close()
        } catch (e: IOException) {
            Log.e(TAG, "Exception thrown while trying to close Face Detector: $e")
        }
    }


    override fun onSuccess(
        results: List<Face>,
        graphicOverlay: GraphicOverlay,
        rect: Rect,
        bitmap: Bitmap
    ) {

        Log.d(TAG, "onSuccess: Face Detector succeeded.")
        graphicOverlay.clear()

        if (results.isEmpty()) {
            listener.onFaceDetected(emptyList(),bitmap)
            return
        }

        results.forEach {face ->
            val faceGraphic = FaceContourGraphic(graphicOverlay, face, rect)
            graphicOverlay.add(faceGraphic)


            val faceBoundingBox = face.boundingBox

            val x = max(0, faceBoundingBox.left)
            val y = max(0, faceBoundingBox.top)
            val width = min(faceBoundingBox.width(), bitmap.width - x)
            val height = min(faceBoundingBox.height(), bitmap.height - y)

            try {
                val faceBitmap = Bitmap.createBitmap(bitmap, x, y, width, height)
                listener.onFaceDetected(results,faceBitmap)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Error cropping face bitmap: ${e.message}")
            }


        }

        graphicOverlay.postInvalidate()
    }

    override fun onFailure(e: Exception) {
        listener.onErrors(e)
        Log.w(TAG, "Face Detector failed.$e")
    }

    companion object {
        private const val TAG = "FaceDetectorProcessor"
    }

}