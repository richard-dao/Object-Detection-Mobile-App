package com.example.loginportal

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.loginportal.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.util.*
import android.content.Intent
import java.io.ByteArrayOutputStream
import org.tensorflow.lite.Tensor
import java.io.File
import androidx.core.content.FileProvider



class LandingPage : AppCompatActivity() {

    lateinit var labels: List<String>
    var colors = listOf<Int>(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED
    )
    val paint = Paint()
    lateinit var imageProcessor: ImageProcessor
    lateinit var bitmap: Bitmap
    lateinit var imageView: ImageView
    lateinit var cameraDevice: CameraDevice
    lateinit var handler: Handler
    lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView
    lateinit var model: SsdMobilenetV11Metadata1

    companion object {
        private const val REQUEST_CAMERA_PERMISSION_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing_page)
        get_permission()

        labels = FileUtil.loadLabels(this, "labels.txt")
        imageProcessor = ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()
        model = SsdMobilenetV11Metadata1.newInstance(this)
        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        imageView = findViewById(R.id.imageView)
        textureView = findViewById(R.id.textureView)
        val captureButton: Button = findViewById(R.id.btnCapture)
        captureButton.setOnClickListener {
            captureImage()
        }

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                open_camera()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                bitmap = textureView.bitmap!!
                var image = TensorImage.fromBitmap(bitmap)
                image = imageProcessor.process(image)

                val outputs = model.process(image)
                val locations = outputs.locationsAsTensorBuffer.floatArray
                val classes = outputs.classesAsTensorBuffer.floatArray
                val scores = outputs.scoresAsTensorBuffer.floatArray

                var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutable)

                val h = mutable.height.toFloat()
                val w = mutable.width.toFloat()
                paint.textSize = h / 15f
                paint.strokeWidth = h / 85f

                scores.forEachIndexed { index, fl ->
                    if (fl > 0.5) {
                        val x = index * 4
                        paint.color = colors[index % colors.size]
                        paint.style = Paint.Style.STROKE
                        canvas.drawRect(
                            RectF(
                                locations[x + 1] * w,
                                locations[x] * h,
                                locations[x + 3] * w,
                                locations[x + 2] * h
                            ), paint
                        )
                        paint.style = Paint.Style.FILL
                        canvas.drawText(
                            "${labels[classes[index].toInt()]} ${fl.toString()}",
                            locations[x + 1] * w,
                            locations[x] * h,
                            paint
                        )
                    }
                }

                imageView.setImageBitmap(mutable)
            }
        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }


    private fun captureImage() {
        // Get the current frame as a Bitmap from TextureView
        val bitmapToCapture = textureView.bitmap ?: return

        // Perform object detection on the captured bitmap
        var image = TensorImage.fromBitmap(bitmapToCapture)
        image = imageProcessor.process(image)
        val outputs = model.process(image)
        val locations = outputs.locationsAsTensorBuffer.floatArray
        val classes = outputs.classesAsTensorBuffer.floatArray
        val scores = outputs.scoresAsTensorBuffer.floatArray

        // Create a mutable copy of the bitmap to draw on
        val mutableBitmap = bitmapToCapture.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        // Get dimensions of the bitmap
        val h = mutableBitmap.height.toFloat()
        val w = mutableBitmap.width.toFloat()

        // Configure paint for drawing the detection box
        val paint = Paint()
        paint.textSize = h / 15f
        paint.strokeWidth = h / 85f

        // Draw detection boxes on the bitmap
        scores.forEachIndexed { index, fl ->
            if (fl > 0.5) {
                val x = index * 4
                paint.color = colors[index % colors.size]
                paint.style = Paint.Style.STROKE
                canvas.drawRect(
                    RectF(
                        locations[x + 1] * w,
                        locations[x] * h,
                        locations[x + 3] * w,
                        locations[x + 2] * h
                    ), paint
                )
                paint.style = Paint.Style.FILL
                canvas.drawText(
                    "${labels[classes[index].toInt()]} ${fl.toString()}",
                    locations[x + 1] * w,
                    locations[x] * h,
                    paint
                )
            }
        }

        // Save bitmap with detection overlay to internal storage
        val filename = "captured_image_with_detection.jpg"
        val fileOutputStream = openFileOutput(filename, Context.MODE_PRIVATE)
        mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
        fileOutputStream.close()

        // Get the file URI
        val file = File(filesDir, filename)
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        Log.d("LandingPage", "URI and File Apparently works")

        // Create an Intent to start the ImagePreview activity
        val email: String? = intent.getStringExtra("email")

        val intent = Intent(this, ImagePreview::class.java)
        // Pass the URI as an extra to the intent
        intent.putExtra("captured_image_uri", uri)
        intent.putExtra("class", labels[classes[0].toInt()])
        intent.putExtra("email", email)
        startActivity(intent)
    }




    @SuppressLint("MissingPermission")
    private fun open_camera() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                cameraManager.openCamera(cameraManager.cameraIdList[0], object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        createCameraPreviewSession()
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        cameraDevice.close()
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        cameraDevice.close()
                    }
                }, handler)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION_CODE
            )
        }
    }

    private fun createCameraPreviewSession() {
        try {
            val texture = textureView.surfaceTexture
            if (texture != null) {
                texture.setDefaultBufferSize(1920, 1080)
            }
            val surface = Surface(texture)

            val captureRequestBuilder =
                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(surface)

            cameraDevice.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        try {
                            session.setRepeatingRequest(
                                captureRequestBuilder.build(),
                                null,
                                handler
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                },
                handler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                open_camera()
            } else {
                // Handle permission denied case
                Log.d("LandingPage", "Camera permission denied")
            }
        }
    }

    fun get_permission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION_CODE
            )
        }
    }
}
