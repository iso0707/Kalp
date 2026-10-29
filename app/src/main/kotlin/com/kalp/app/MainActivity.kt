package com.kalp.app

import android.app.Activity
import android.app.DatePickerDialog
import android.os.Bundle
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private val dark = Color.rgb(12, 14, 25)
    private val pink = Color.rgb(255, 130, 170)

    private lateinit var root: LinearLayout
    private lateinit var info: TextView
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var selectedDate: TextView

    private var relationshipDate = Calendar.getInstance()

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
            showHome(SupabaseStore.Session(uid, token))
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

    private fun addInput(hintText: String, secret: Boolean = false): EditText {
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

        addButton("Giriş Yap") { authenticate(false) }
        addButton("Hesap Oluştur") { authenticate(true) }
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
                val s = if (register) {
                    SupabaseStore.register(user, pass)
                } else {
                    SupabaseStore.login(user, pass)
                }

                prefs.edit()
                    .putString("uid", s.uid)
                    .putString("token", s.token)
                    .apply()

                runOnUiThread { showHome(s) }

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

        info = addText("Bilgiler yükleniyor...", 18f)

        val pairId = prefs.getString("pair_id", null)
        if (pairId != null) {
            loadPairInfo(session, pairId)
        } else {
            info.text = "Henüz eşleşmediniz."
        }

        val codeInput = addInput("6 haneli eşleşme kodu")

        selectedDate = addText("", 16f)
        updateDateText()

        addButton("♥ Başlangıç tarihini seç") {
            val now = Calendar.getInstance()

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

            info.text = "Eşleşme oluşturuluyor..."

            thread {
                try {
                    val id = SupabaseStore.createPair(session, (100000..999999).random().toString(), date)

                    prefs.edit()
                        .putString("pair_id", id)
                        .putString("pair_code", prefs.getString("pair_code", ""))
                        .putString("relationship_start", date)
                        .apply()

                    runOnUiThread {
                        info.text = "♥ Eşleşme oluşturuldu!\nKodunu aşağıda görebilirsin."
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
                    prefs.edit().putString("pair_id", id).apply()

                    runOnUiThread {
                        info.text = "♥ Başarıyla eşleştiniz!"
                        loadPairInfo(session, id)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        info.text = "Hata: ${e.message}"
                    }
                }
            }
        }

        addButton("Bilgileri yenile") {
            val id = prefs.getString("pair_id", null)
            if (id != null) loadPairInfo(session, id)
        }

        addButton("Çıkış yap") {
            prefs.edit().remove("uid").remove("token").apply()
            showLogin()
        }
    }

    private fun updateDateText() {
        selectedDate.text = "Seçilen tarih: " +
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                .format(relationshipDate.time)
    }

    private fun loadPairInfo(session: SupabaseStore.Session, pairId: String) {
        info.text = "Bilgiler yükleniyor..."

        thread {
            try {
                val pair = SupabaseStore.getPairInfo(session, pairId)
                val start = pair.getString("relationship_start")

                val date = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                ).parse(start.substring(0, 10)) ?: Date()

                val elapsed = (System.currentTimeMillis() - date.time)
                    .coerceAtLeast(0L)

                val days = elapsed / 86400000L
                val hours = elapsed / 3600000L

                prefs.edit()
                    .putString("relationship_start", start)
                    .putString("relationship_days", days.toString())
                    .putString("relationship_hours", hours.toString())
                    .apply()

                runOnUiThread {
                    info.text = "♥ Eşleşme aktif\n\n$days gündür birliktesiniz\n$hours saat"
                }

            } catch (e: Exception) {
                runOnUiThread {
                    info.text = "Bilgi alınamadı:\n${e.message}"
                }
            }
        }
    }
}
