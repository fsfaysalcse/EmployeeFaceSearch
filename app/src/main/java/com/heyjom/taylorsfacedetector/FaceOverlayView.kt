package com.heyjom.taylorsfacedetector

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.face.Face

class FaceOverlayView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    var faces: List<Face> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    var widthScaleFactor = 1.0f
    var heightScaleFactor = 1.0f
    var isFrontCamera = false

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (face in faces) {
            val bounds = face.boundingBox
            val left = bounds.left * widthScaleFactor
            val top = bounds.top * heightScaleFactor
            val right = bounds.right * widthScaleFactor
            val bottom = bounds.bottom * heightScaleFactor
            val rect = if (isFrontCamera) {
                RectF(width - right, top, width - left, bottom)
            } else {
                RectF(left, top, right, bottom)
            }
            canvas.drawRect(rect, paint)
        }
    }
}
