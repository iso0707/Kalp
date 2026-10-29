package com.kalp.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

class LocationService : Service(), LocationListener {

    private lateinit var locationManager: LocationManager

    override fun onCreate() {
        super.onCreate()

        val channelId = "kalp_location"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Kalp konum paylaşımı",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Kalp")
            .setContentText("Konum paylaşımı açık")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()

        startForeground(10, notification)

        locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            60000L,
            20f,
            this
        )
    }

    override fun onLocationChanged(location: Location) {
        val prefs = getSharedPreferences("kalp_data", Context.MODE_PRIVATE)

        val uid = prefs.getString("uid", null) ?: return
        val token = prefs.getString("token", null) ?: return
        val pairId = prefs.getString("pair_id", null) ?: return

        Thread {
            try {
                val row = JSONObject()
                    .put("pair_id", pairId)
                    .put("user_id", uid)
                    .put("latitude", location.latitude)
                    .put("longitude", location.longitude)
                    .put("updated_at", Instant.now().toString())

                val connection = URL(
                    SupabaseStore.BASE_URL +
                    "/rest/v1/locations?on_conflict=pair_id,user_id"
                ).openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty(
                    "apikey",
                    SupabaseStore.PUBLISHABLE_KEY
                )
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $token"
                )
                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )
                connection.setRequestProperty(
                    "Prefer",
                    "resolution=merge-duplicates"
                )
                connection.doOutput = true

                connection.outputStream.use {
                    it.write(
                        JSONArray().put(row).toString()
                            .toByteArray(Charsets.UTF_8)
                    )
                }

                connection.responseCode
                connection.disconnect()

            } catch (_: Exception) {
            }
        }.start()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(this)
        }
        super.onDestroy()
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    override fun onStatusChanged(
        provider: String?,
        status: Int,
        extras: Bundle?
    ) {}
}
