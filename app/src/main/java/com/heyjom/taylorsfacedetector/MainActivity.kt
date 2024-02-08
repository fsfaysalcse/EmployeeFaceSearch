package com.heyjom.taylorsfacedetector

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.mlkit.vision.face.Face
import com.heyjom.taylorsfacedetector.camerax.CameraManager
import com.heyjom.taylorsfacedetector.databinding.ActivityMainBinding
import com.heyjom.taylorsfacedetector.face_detection.FaceContourDetectionListener
import com.heyjom.taylorsfacedetector.model.Employee
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONObject
import java.io.ByteArrayOutputStream

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
        //  Log.d(TAG, "onFaceDetected: Face Detected ${faces.size}")

        if (faces.isEmpty()) {
            runOnUiThread {
                binding.infoLayout.visibility = android.view.View.INVISIBLE
            }
            return
        }

        val stream = ByteArrayOutputStream()
        captureBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "photo",
                "filename.jpg",
                byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("http://192.168.1.103:8080/search")
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to upload image", e)
                runOnUiThread {
                    Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_SHORT).show()
                    binding.infoLayout.visibility = android.view.View.INVISIBLE
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBodyString = response.body?.string()
                    Log.d(TAG, "Image uploaded successfully: $responseBodyString")

                    runOnUiThread {
                        responseBodyString?.let {
                            // Parse the JSON response manually
                            val jsonResponse = JSONObject(it)
                            val employeeJson = jsonResponse.getJSONObject("employee")
                            val name = employeeJson.getString("name")

                            binding.infoLayout.visibility = android.view.View.VISIBLE
                            binding.tvName.text = name
                            binding.face.setImageBitmap(captureBitmap)
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to upload image: ${response.message}")
                    runOnUiThread {
                        binding.infoLayout.visibility = android.view.View.INVISIBLE
                    }
                }
            }
        })
    }

    override fun onErrors(e: Exception) {
        Log.d(TAG, "onErrors: ${e.message}")
        binding.infoLayout.visibility = android.view.View.INVISIBLE
    }

}