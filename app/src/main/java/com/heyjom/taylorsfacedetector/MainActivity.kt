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
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

        Log.d(TAG, "onFaceDetected: ${faces.size}")

        if (faces.isEmpty()) {
            runOnUiThread {
                binding.infoLayout.visibility = android.view.View.INVISIBLE
            }
            return
        }

        val stream = ByteArrayOutputStream()
        captureBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()

        // Launch a coroutine on the IO dispatcher for network operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = HttpClient()
                val response: HttpResponse = client.submitFormWithBinaryData(
                    url = "http://192.168.1.107:9090/search",
                    formData = formData {
                        append("photo", byteArray, Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=\"filename.jpg\"")
                        })
                    }
                ) {
                    method = HttpMethod.Post
                }

                // Check if the response is successful
                if (response.status == HttpStatusCode.OK) {
                    val responseBodyString = response.bodyAsText()
                    withContext(Dispatchers.Main) {
                        // Parse the JSON response
                        val jsonResponse = JSONObject(responseBodyString)
                        val employeeJson = jsonResponse.getJSONObject("employee")
                        val name = employeeJson.getString("name")

                        binding.infoLayout.visibility = android.view.View.VISIBLE
                        binding.tvName.text = name
                        binding.face.setImageBitmap(captureBitmap)
                    }
                } else {
                    Log.e(TAG, "Failed to upload image: ${response.status}")
                    withContext(Dispatchers.Main) {
                        binding.infoLayout.visibility = android.view.View.INVISIBLE
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload image", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_SHORT).show()
                    binding.infoLayout.visibility = android.view.View.INVISIBLE
                }
            }
        }
    }


    override fun onErrors(e: Exception) {
        Log.d(TAG, "onErrors: ${e.message}")
        binding.infoLayout.visibility = android.view.View.INVISIBLE
    }

}