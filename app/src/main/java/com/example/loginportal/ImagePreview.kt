// ImagePreview.kt
package com.example.loginportal

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import kotlin.random.Random
import android.widget.Toast

import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.PutObjectRequest
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException

class ImagePreview : AppCompatActivity() {

    private lateinit var imageViewPreview: ImageView
    private lateinit var btnBackToCamera: Button
    private lateinit var submitButton: Button

    private lateinit var s3Client: AmazonS3Client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)

        s3Client = AmazonS3Client(AnonymousAWSCredentials())

        imageViewPreview = findViewById(R.id.capturedImageView)
        btnBackToCamera = findViewById(R.id.btnBackToCamera)
        submitButton = findViewById(R.id.submitButton)

        // Retrieve the image URI from the Intent extras
        val imageUri: Uri? = intent.getParcelableExtra<Uri>("captured_image_uri")
        imageViewPreview.setImageURI(imageUri)

        val className: String? = intent.getStringExtra("class")
        val email: String? = intent.getStringExtra("email")

        // Log the type of classes[0]
        Log.d("ImagePreview", "Type of classes[0]: $className")

        btnBackToCamera.setOnClickListener {
            finish()  // This will close the ImagePreview activity and return to the LandingPage
        }

        submitButton.setOnClickListener {
            // Upload image to S3 when the submit button is clicked
            if (imageUri != null) {
                uploadImageToS3(imageUri, className, email)
            } else {
                Log.e("ImagePreview", "Image URI is null")
            }
        }
    }

    private fun uploadImageToS3(uri: Uri, filename: String?, email: String?) {
        val bucketName = "ucsc-object-detection" // Replace with your S3 bucket name
        val objectKey = filename + Random.nextInt(10000000).toString() + ".jpg" // Replace with your desired object key in S3

        try {
            val file = File(applicationContext.cacheDir, "temp-file")
            val inputStream = contentResolver.openInputStream(uri)
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Upload the file to S3
            val putObjectRequest = PutObjectRequest(bucketName, objectKey, file)
            s3Client.putObject(putObjectRequest)

            val fullLink = "https://ucsc-object-detection.s3.amazonaws.com/$objectKey"
            insertImageUrl(objectKey, fullLink, email)

            // Show Toast notification
            runOnUiThread {
                Toast.makeText(this@ImagePreview, "Image saved successfully", Toast.LENGTH_SHORT).show()
            }

            Log.d("ImagePreview", "Successfully uploaded image to S3")
        } catch (e: Exception) {
            Log.e("ImagePreview", "Error uploading image to S3: $e")
        }
    }

    private fun insertImageUrl(name: String, link: String, email: String?) {
        val conSql = ConSQL()
        val connection: Connection? = conSql.conclass()

        try {
            if (connection != null) {
                val query = "INSERT INTO ImageData (ImageName, ImageLink, SubmitterEmail) VALUES (?, ?, ?)"
                val statement: PreparedStatement = connection.prepareStatement(query)
                statement.setString(1, name)
                statement.setString(2, link)
                statement.setString(3, email)
                statement.executeUpdate()

                Log.d("ImagePreview", "Data inserted successfully")

                statement.close()
            } else {
                Log.e("ImagePreview", "Failed to establish DB connection")
            }
        } catch (e: SQLException) {
            Log.e("ImagePreview", "SQL Exception", e)
        } finally {
            connection?.close()
        }
    }
}
