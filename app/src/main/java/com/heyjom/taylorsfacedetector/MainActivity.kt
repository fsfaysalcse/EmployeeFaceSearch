package com.heyjom.taylorsfacedetector

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.face.Face
import com.heyjom.taylorsfacedetector.camerax.CameraManager
import com.heyjom.taylorsfacedetector.databinding.ActivityMainBinding
import com.heyjom.taylorsfacedetector.face_detection.FaceContourDetectionListener

private const val TAG = "MainActivityXX"

class MainActivity : AppCompatActivity(), FaceContourDetectionListener {

    private lateinit var cameraManager: CameraManager

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        createCameraManager()
        checkForPermission()
        onClicks()
    }

    private fun checkForPermission() {
        if (allPermissionsGranted()) {
            cameraManager.startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun onClicks() {
        binding.btnSwitch.setOnClickListener {
            cameraManager.changeCameraSelector()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                cameraManager.startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    private fun createCameraManager() {
        cameraManager = CameraManager(
            this,
            binding.previewViewFinder,
            this,
            binding.graphicOverlayFinder,
            this
        )


    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
    }

    override fun onFaceDetected(faces: List<Face>, captureBitmap: Bitmap) {
        Log.d(TAG, "onFaceDetected: Face Detected ${faces.size}")

        if (faces.isEmpty()) {
            runOnUiThread {
                binding.infoLayout.visibility = android.view.View.INVISIBLE
            }
            return
        }

        faces.forEachIndexed { index, face ->
            runOnUiThread {
                binding.face.setImageBitmap(captureBitmap)
                binding.infoLayout.visibility = android.view.View.VISIBLE
            }
        }
    }


    override fun onErrors(e: Exception) {
        Log.d(TAG, "onErrors: ${e.message}")
        binding.infoLayout.visibility = android.view.View.INVISIBLE
    }

}