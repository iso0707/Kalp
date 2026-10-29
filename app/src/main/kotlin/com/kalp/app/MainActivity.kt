
package com.kalp.app

import android.app.Activity
import android.app.DatePickerDialog
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import android.location.Location

class MainActivity : Activity() {

    private val dark = Color.rgb(12, 14, 25)
    private val pink = Color.rgb(255, 130, 170)
    private val locationRequest = 101

    private lateinit var root: LinearLayout
    private lateinit var info: TextView
    private lateinit var pairCodeInfo: TextView
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var selectedDate: TextView

    private var relationshipDate = Calendar.getInstance()
    private var currentSession: SupabaseStore.Session? = null

    private val prefs by lazy {
        getSharedPreferences("kalp_data", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = dark
        window.navigationBarColor = dark

        val uid = prefs.getString("uid", null)
        val token = prefs.getString("token", null)

        if (uid != null && token != null) {
            currentSession = SupabaseStore.Session(uid, token)
            showHome(currentSession!!)
        } else {
            showLogin()
        }
    }

    private fun makeRoot() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(25, 25, 25, 25)
            setBackgroundColor(dark)
        }
        setContentView(root)
    }

    private fun addText(value: String, size: Float): TextView {
        val v = TextView(this).apply {
            text = value
            textSize = size
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(8, 12, 8, 12)
        }
        root.addView(v)
        return v
    }

    private fun addInput(
        hintText: String,
        secret: Boolean = false
    ): EditText {
        val v = EditText(this).apply {
            hint = hintText
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            inputType = if (secret) 129 else 1
        }
        root.addView(v)
        return v
    }

    private fun addButton(label: String, action: () -> Unit) {
        root.addView(Button(this).apply {
            text = label
            setOnClickListener { action() }
        })
    }

    private fun showLogin() {
        makeRoot()

        addText("♥ Kalp", 36f).setTextColor(pink)
        addText("Mesafeler uzak olsa da kalpler yakın.", 14f)

        username = addInput("Kullanıcı adı")
        password = addInput("Şifre", true)
        info = addText("", 14f)

        addButton("Giriş Yap") {
            authenticate(false)
        }

        addButton("Hesap Oluştur") {
            authenticate(true)
        }
    }

    private fun authenticate(register: Boolean) {
        val user = username.text.toString().trim()
        val pass = password.text.toString()

        if (!user.matches(Regex("[a-zA-Z0-9_]{3,20}"))) {
            info.text = "Kullanıcı adı 3-20 karakter olmalı."
            return
        }

        if (pass.length < 6) {
            info.text = "Şifre en az 6 karakter olmalı."
            return
        }

        info.text = "Bağlanılıyor..."

        thread {
            try {
                val session = if (register) {
                    SupabaseStore.register(user, pass)
                } else {
                    SupabaseStore.login(user, pass)
                }

                prefs.edit()
                    .putString("uid", session.uid)
                    .putString("token", session.token)
                    .apply()

                runOnUiThread {
                    currentSession = session
                    showHome(session)
                }

            } catch (e: Exception) {
                runOnUiThread {
                    info.text = e.message ?: "Bağlantı hatası"
                }
            }
        }
    }

    private fun showHome(session: SupabaseStore.Session) {
        makeRoot()

        addText("♥ Kalp", 36f).setTextColor(pink)
        addText("Birlikte geçen zamanınız", 16f)

        info = addText("Henüz eşleşmediniz.", 17f)

        pairCodeInfo = addText("", 18f)
        pairCodeInfo.setTextColor(pink)

        val savedCode = prefs.getString("pair_code", null)

        if (savedCode != null) {
            pairCodeInfo.text =
                "♥ Eşleşme kodun: $savedCode\nPartnerine gönder."
        }

        val pairId = prefs.getString("pair_id", null)

        if (pairId != null) {
            loadPairInfo(session, pairId)
        }

        val codeInput = addInput("6 haneli eşleşme kodu")

        selectedDate = addText("", 16f)
        updateDateText()

        addButton("♥ Başlangıç tarihini seç") {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    relationshipDate.set(year, month, day)
                    updateDateText()
                },
                relationshipDate.get(Calendar.YEAR),
                relationshipDate.get(Calendar.MONTH),
                relationshipDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        addButton("Yeni eşleşme oluştur") {
            val date = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            ).format(relationshipDate.time)

            val code = (100000..999999).random().toString()

            info.text = "Eşleşme oluşturuluyor..."

            thread {
                try {
                    val id = SupabaseStore.createPair(
                        session,
                        code,
                        date
                    )

                    prefs.edit()
                        .putString("pair_id", id)
                        .putString("pair_code", code)
                        .putString("relationship_start", date)
                        .apply()

                    runOnUiThread {
                        pairCodeInfo.text =
                            "♥ Eşleşme kodun: $code\nPartnerine gönder."

                        info.text = "Eşleşme başarıyla oluşturuldu!"
                        loadPairInfo(session, id)
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        info.text = "Hata: ${e.message}"
                    }
                }
            }
        }

        addButton("Partnerimin koduyla katıl") {
            val code = codeInput.text.toString().trim()

            if (!code.matches(Regex("\\d{6}"))) {
                info.text = "6 haneli kodu gir."
                return@addButton
            }

            info.text = "Eşleşiliyor..."

            thread {
                try {
                    val id = SupabaseStore.joinPair(session, code)

                    prefs.edit()
                        .putString("pair_id", id)
                        .remove("pair_code")
                        .apply()

                    runOnUiThread {
                        pairCodeInfo.text = "♥ Başarıyla eşleştiniz!"
                        info.text = "Eşleşme tamamlandı."
                        loadPairInfo(session, id)
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        info.text = "Hata: ${e.message}"
                    }
                }
            }
        }

        addButton("Konum paylaşımını aç") {
            val id = prefs.getString("pair_id", null)

            if (id == null) {
                info.text = "Önce partnerinle eşleşmelisin."
            } else {
                requestLocationPermission()
            }
        }

        addButton("Konum paylaşımını kapat") {
            stopService(Intent(this, LocationService::class.java))

            prefs.edit()
                .putBoolean("sharing_enabled", false)
                .apply()

            info.text = "Konum paylaşımı kapatıldı."
        }

        addButton("Mesafeyi yenile") {
            val id = prefs.getString("pair_id", null)

            if (id != null) {
                loadDistance(session, id)
            } else {
                info.text = "Önce eşleşmelisin."
            }
        }

        addButton("Bilgileri yenile") {
            val id = prefs.getString("pair_id", null)

            if (id != null) {
                loadPairInfo(session, id)
            }
        }

        addButton("Çıkış yap") {
            stopService(Intent(this, LocationService::class.java))

            prefs.edit()
                .remove("uid")
                .remove("token")
                .apply()

            currentSession = null
            showLogin()
        }
    }

    private fun updateDateText() {
        selectedDate.text = "Seçilen tarih: " +
            SimpleDateFormat(
                "dd.MM.yyyy",
                Locale.getDefault()
            ).format(relationshipDate.time)
    }

    private fun requestLocationPermission() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) !=
                PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missing.toTypedArray(),
                locationRequest
            )
        } else {
            startLocationSharing()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == locationRequest) {
            val locationGranted =
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

            if (locationGranted) {
                startLocationSharing()
            } else {
                info.text =
                    "Konum izni verilmedi. Paylaşım açılmadı."
            }
        }
    }

    private fun startLocationSharing() {
        val intent = Intent(this, LocationService::class.java)

        try {
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            prefs.edit()
                .putBoolean("sharing_enabled", true)
                .apply()

            info.text = "♥ Konum paylaşımı açıldı."

        } catch (e: Exception) {
            info.text = "Konum başlatılamadı: ${e.message}"
        }
    }

    private fun loadPairInfo(
        session: SupabaseStore.Session,
        pairId: String
    ) {
        thread {
            try {
                val pair = SupabaseStore.getPairInfo(session, pairId)
                val start = pair.getString("relationship_start")

                val date = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                ).parse(start.substring(0, 10)) ?: Date()

                val elapsed = (
                    System.currentTimeMillis() - date.time
                ).coerceAtLeast(0L)

                val days = elapsed / 86400000L
                val hours = elapsed / 3600000L

                prefs.edit()
                    .putString("relationship_start", start)
                    .putString("relationship_days", days.toString())
                    .putString("relationship_hours", hours.toString())
                    .apply()

                runOnUiThread {
                    info.text =
                        "♥ Eşleşme aktif\n\n$days gündür birliktesiniz\n$hours saat"

                    loadDistance(session, pairId)
                }

            } catch (e: Exception) {
                runOnUiThread {
                    info.text = "Bilgi alınamadı:\n${e.message}"
                }
            }
        }
    }

    private fun loadDistance(
        session: SupabaseStore.Session,
        pairId: String
    ) {
        thread {
            try {
                val locations =
                    SupabaseStore.getLocations(session, pairId)

                if (locations.length() < 2) {
                    runOnUiThread {
                        info.text =
                            "♥ Eşleşme aktif\nİki tarafın da konum paylaşması bekleniyor."
                    }
                    return@thread
                }

                val first = locations.getJSONObject(0)
                val second = locations.getJSONObject(1)

                val results = FloatArray(1)

                Location.distanceBetween(
                    first.getDouble("latitude"),
                    first.getDouble("longitude"),
                    second.getDouble("latitude"),
                    second.getDouble("longitude"),
                    results
                )

                val km = results[0] / 1000.0
                val distanceText =
                    String.format(Locale.US, "%.1f", km)

                prefs.edit()
                    .putString("distance", distanceText)
                    .apply()

                runOnUiThread {
                    info.text = "♥ Aranızdaki mesafe: $distanceText km"
                }

            } catch (e: Exception) {
                runOnUiThread {
                    info.text = "Mesafe alınamadı: ${e.message}"
                }
            }
        }
    }
}
