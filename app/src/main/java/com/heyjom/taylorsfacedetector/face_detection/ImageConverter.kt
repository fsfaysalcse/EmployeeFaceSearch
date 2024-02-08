package com.heyjom.taylorsfacedetector.face_detection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

fun ImageProxy.toBitmapp(): Bitmap {
    val nv21 = yuv420888ToNv21(this)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, this.width, this.height), 100, out)
    val imageBytes = out.toByteArray()
    var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    val matrix = Matrix().apply {
        postRotate(this@toBitmapp.imageInfo.rotationDegrees.toFloat())
    }
    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    return bitmap
}

fun yuv420888ToNv21(image: ImageProxy): ByteArray {
    val pixelCount = image.cropRect.width() * image.cropRect.height()
    val pixelSizeBits = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)
    val output = ByteArray(pixelCount * pixelSizeBits / 8)
    val yBuffer = image.planes[0].buffer
    val uBuffer = image.planes[1].buffer
    val vBuffer = image.planes[2].buffer

    yBuffer.get(output, 0, yBuffer.remaining())
    val uBufferRemaining = uBuffer.remaining()
    val vBufferRemaining = vBuffer.remaining()
    var position = pixelCount
    for (i in 0 until uBufferRemaining step 2) {
        output[position++] = vBuffer[i]
        output[position++] = uBuffer[i]
    }
    return output
}
