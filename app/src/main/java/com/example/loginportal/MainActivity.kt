package com.example.loginportal

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.EditText
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import org.mindrot.jbcrypt.BCrypt
import android.os.AsyncTask

class MainActivity : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loginButton = findViewById(R.id.button)
        email = findViewById(R.id.editTextTextEmailAddress)
        password = findViewById(R.id.editTextTextPassword2)

        loginButton.setOnClickListener {
            // Call a function to insert test data
            if (validateData(email, password)) {
                Log.d("MainActivity", "Login Successful")

                val intent = Intent(this, LandingPage::class.java)
                intent.putExtra("email", email.text.toString())
                startActivity(intent)

                finish()
            } else {
                Log.d("MainActivity", "Invalid username or password")
            }
        }
    }

    private fun validateData(email: EditText, password: EditText): Boolean {
        val conSql = ConSQL()
        val connection: Connection? = conSql.conclass()
        var storedHashedPassword: String? = null

        try {
            if (connection != null) {
                val query = "SELECT Password FROM Users WHERE Email = ?"
                val statement: PreparedStatement = connection.prepareStatement(query)
                statement.setString(1, email.text.toString())

                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    storedHashedPassword = resultSet.getString("Password")
                }
                resultSet.close()
                statement.close()
            } else {
                Log.e("MainActivity", "Failed to Establish DB Connection")
            }
        } catch (e: Exception){
            Log.e("Main Activity", "SQL Exception", e)
        } finally {
            connection?.close()
        }

        return if (storedHashedPassword != null) {
            BCrypt.checkpw(password.text.toString() , storedHashedPassword)
        } else {
            false // Username not found
        }
    }

    private fun insertTestData(email: EditText, password: EditText) {
        val conSql = ConSQL()
        val connection: Connection? = conSql.conclass()

        try {
            if (connection != null) {
                val query = "INSERT INTO Users (Email, Password) VALUES (?, ?)"
                val statement: PreparedStatement = connection.prepareStatement(query)
                val hashedPassword = BCrypt.hashpw(password.text.toString(), BCrypt.gensalt())
                statement.setString(1, email.text.toString())
                statement.setString(2, hashedPassword)
                statement.executeUpdate()

                Log.d("MainActivity", "Data inserted successfully")

                statement.close()
            } else {
                Log.e("MainActivity", "Failed to establish DB connection")
            }
        } catch (e: SQLException) {
            Log.e("MainActivity", "SQL Exception", e)
        } finally {
            connection?.close()
        }
    }
}
