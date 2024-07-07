package com.example.loginportal
import android.annotation.SuppressLint
import android.os.StrictMode
import android.util.Log

import java.sql.Connection;
import java.sql.DriverManager

class ConSQL {

    fun conclass(): Connection? {
        val ip="192.168.12.122"
        val port ="1433"
        val db = "UCSC-Object-Detection-Database"
        val username = "TestLogin"
        val password = "Password123!"
        var con: Connection? = null

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        var connectUrl: String? = null
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver")
            connectUrl = "jdbc:jtds:sqlserver://$ip:$port;databasename=$db;user=$username;password=$password;"
            con = DriverManager.getConnection(connectUrl)
        } catch (e: Exception) {
            Log.e("Error :", e.message ?: "Unknown error");
        }
        return con
    }

}