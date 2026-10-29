package com.kalp.app

import android.Manifest
import android.app.*
import android.appwidget.AppWidgetManager
import android.content.ComponentName
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            Notification.Builder(this)
        }.setContentTitle("Kalp")
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

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                60000L,
                20f,
                this
            )
        } catch (_: Exception) {
            stopSelf()
        }
    }

    override fun onLocationChanged(location: Location) {
        val prefs = getSharedPreferences("kalp_data", Context.MODE_PRIVATE)

        val uid = prefs.getString("uid", null) ?: return
        val token = prefs.getString("token", null) ?: return
        val pairId = prefs.getString("pair_id", null) ?: return

        Thread {
            try {
                val timestamp = SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    Locale.US
                ).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())

                val row = JSONObject()
                    .put("pair_id", pairId)
                    .put("user_id", uid)
                    .put("latitude", location.latitude)
                    .put("longitude", location.longitude)
                    .put("updated_at", timestamp)

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

                val responseCode = connection.responseCode
                connection.disconnect()

                if (responseCode in 200..299) {
                    refreshDistanceAndWidget(prefs, uid, token, pairId)
                }
            } catch (_: Exception) {
            }
        }.start()
    }

    private fun refreshDistanceAndWidget(
        prefs: android.content.SharedPreferences,
        uid: String,
        token: String,
        pairId: String
    ) {
        try {
            val session = SupabaseStore.Session(uid, token)
            val locations = SupabaseStore.getLocations(session, pairId)

            if (locations.length() >= 2) {
                val first = locations.getJSONObject(0)
                val second = locations.getJSONObject(1)

                val distance = FloatArray(1)

                Location.distanceBetween(
                    first.getDouble("latitude"),
                    first.getDouble("longitude"),
                    second.getDouble("latitude"),
                    second.getDouble("longitude"),
                    distance
                )

                val km = distance[0] / 1000f

                prefs.edit()
                    .putString("distance", String.format(Locale.US, "%.1f", km))
                    .apply()
            }

            val manager = AppWidgetManager.getInstance(this)
            val component = ComponentName(this, LoveWidget::class.java)
            val ids = manager.getAppWidgetIds(component)

            for (id in ids) {
                LoveWidget.updateWidget(this, manager, id)
            }
        } catch (_: Exception) {
        }
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

    @Deprecated("Deprecated in Android")
    override fun onStatusChanged(
        provider: String?,
        status: Int,
        extras: Bundle?
    ) {}
}
