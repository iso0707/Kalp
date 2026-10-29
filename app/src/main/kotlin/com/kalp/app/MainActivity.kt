package com.kalp.app

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import android.app.DatePickerDialog
import android.content.Intent
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("kalp", Context.MODE_PRIVATE) }
    private lateinit var dateText: TextView
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(15, 18, 29)
        window.navigationBarColor = Color.rgb(15, 18, 29)
        buildScreen()
        refresh()
    }

    private fun buildScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 46, 28, 24)
            setBackgroundColor(Color.rgb(15, 18, 29))
        }
        val heart = TextView(this).apply {
            text = "♥"; textSize = 38f; setTextColor(Color.rgb(242, 141, 181))
            gravity = Gravity.CENTER
        }
        val heading = TextView(this).apply {
            text = "Kalp"; textSize = 25f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); gravity = Gravity.CENTER
        }
        val sub = TextView(this).apply {
            text = "Mesafeniz değişir, bağınız kalır."; textSize = 13f
            setTextColor(Color.rgb(173, 180, 198)); gravity = Gravity.CENTER
        }
        dateText = TextView(this).apply {
            textSize = 16f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setPadding(18, 22, 18, 22)
            setBackgroundColor(Color.rgb(31, 37, 55))
            setOnClickListener { chooseDate() }
        }
        val dateButton = Button(this).apply {
            text = "Birliktelik tarihini belirle"
            setOnClickListener { chooseDate() }
        }
        val toggle = Switch(this).apply {
            text = "Widget ve sayaç açık"
            setTextColor(Color.WHITE); textSize = 15f
            isChecked = prefs.getBoolean("enabled", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("enabled", checked).apply()
                refreshWidget()
                refresh()
            }
        }
        statusText = TextView(this).apply {
            textSize = 13f; setTextColor(Color.rgb(173, 180, 198))
            gravity = Gravity.CENTER; setPadding(8, 24, 8, 18)
        }
        val locationButton = Button(this).apply {
            text = "Konum iznini ver"
            setOnClickListener {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 41)
                else Toast.makeText(this@MainActivity, "Konum izni açık. İki telefonun eşleşmesi için sunucu bağlantısı kurulmalı.", Toast.LENGTH_LONG).show()
            }
        }
        page.addView(heart)
        page.addView(heading)
        page.addView(sub)
        page.addView(dateText, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 34 })
        page.addView(dateButton)
        page.addView(toggle, LinearLayout.LayoutParams(-2, -2).apply { topMargin = 24 })
        page.addView(statusText)
        page.addView(locationButton)
        setContentView(page)
    }

    private fun chooseDate() {
        val cal = Calendar.getInstance()
        val stored = prefs.getLong("start_date", 0L)
        if (stored > 0) cal.timeInMillis = stored
        DatePickerDialog(this, { _, y, m, d ->
            val c = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
            prefs.edit().putLong("start_date", c.timeInMillis).apply()
            refresh(); refreshWidget()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun refresh() {
        val enabled = prefs.getBoolean("enabled", true)
        val start = prefs.getLong("start_date", 0L)
        dateText.text = if (start == 0L) "Başlangıç tarihi seçilmedi"
            else "♥  " + SimpleDateFormat("d MMMM yyyy", Locale("tr")).format(Date(start))
        statusText.text = if (!enabled) "Sayaç kapalı"
            else if (start == 0L) "Başlamak için birlikte olduğunuz tarihi seçin."
            else {
                val days = ((System.currentTimeMillis() - start).coerceAtLeast(0) / 86400000L)
                "$days gündür birliktesiniz ♥\nMesafe: iki telefon eşleştirildiğinde gösterilecek."
            }
    }

    private fun refreshWidget() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(ComponentName(this, LoveWidget::class.java))
        val intent = Intent(this, LoveWidget::class.java).apply { action = AppWidgetManager.ACTION_APPWIDGET_UPDATE; putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids) }
        sendBroadcast(intent)
    }
}
