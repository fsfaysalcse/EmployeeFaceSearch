package com.heyjom.taylorsfacedetector.camerax

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.heyjom.taylorsfacedetector.face_detection.toBitmapp
import java.io.ByteArrayOutputStream

abstract class BaseImageAnalyzer<T> : ImageAnalysis.Analyzer {

    abstract val graphicOverlay: GraphicOverlay

    @OptIn(ExperimentalGetImage::class) @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        val bitmap = imageProxy.toBitmapp()

        mediaImage?.let { image ->
            detectInImage(InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees))
                .addOnSuccessListener { results ->
                    onSuccess(
                        results,
                        graphicOverlay,
                        image.cropRect,
                        bitmap
                    )
                    imageProxy.close()
                }
                .addOnFailureListener {
                    onFailure(it)
                    imageProxy.close()
                }
        }
    }


    protected abstract fun detectInImage(image: InputImage): Task<T>

    abstract fun stop()

    protected abstract fun onSuccess(
        results: T,
        graphicOverlay: GraphicOverlay,
        rect: Rect,
        bitmap: Bitmap
    )

    protected abstract fun onFailure(e: Exception)

}