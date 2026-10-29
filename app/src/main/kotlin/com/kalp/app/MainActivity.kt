package com.kalp.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var info: TextView
    private lateinit var codeInput: EditText
    private lateinit var dateInput: EditText
    private lateinit var session: SupabaseStore.Session

    private val prefs by lazy {
        getSharedPreferences("kalp_data", Context.MODE_PRIVATE)
    }

    private val dark = Color.rgb(12, 14, 25)
    private val pink = Color.rgb(255, 130, 170)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = dark
        window.navigationBarColor = dark
        window.decorView.setBackgroundColor(dark)

        showLoading()

        val uid = prefs.getString("uid", null)
        val token = prefs.getString("token", null)

        if (!uid.isNullOrBlank() && !token.isNullOrBlank()) {
            session = SupabaseStore.Session(uid, token)
            buildUi()
        } else {
            connectSupabase()
        }
    }

    private fun showLoading() {
        info = TextView(this).apply {
            text = "♥\n\nKalp hazırlanıyor..."
            textSize = 22f
            setTextColor(pink)
            gravity = Gravity.CENTER
            setBackgroundColor(dark)
            setPadding(24, 24, 24, 24)
        }
        setContentView(info)
    }

    private fun connectSupabase() {
        info.text = "♥\n\nSupabase bağlantısı kuruluyor..."

        thread {
            try {
                val newSession = SupabaseStore.anonymousSignIn()

                prefs.edit()
                    .putString("uid", newSession.uid)
                    .putString("token", newSession.token)
                    .apply()

                runOnUiThread {
                    session = newSession
                    buildUi()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    info.text = "Bağlantı kurulamadı.\n\n${e.message ?: "Bilinmeyen hata"}"
                    info.setTextColor(Color.rgb(255, 100, 120))
                    info.textSize = 15f
                }
            }
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
            setBackgroundColor(dark)
        }

        fun text(value: String, size: Float): TextView {
            return TextView(this).apply {
                text = value
                textSize = size
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(8, 12, 8, 12)
            }
        }

        val title = text("♥  Kalp", 34f)
        title.setTextColor(pink)
        root.addView(title)

        root.addView(text("Mesafeler uzak olsa da kalpler yakın.", 14f))

        info = text("Henüz eşleşmediniz.", 16f)
        root.addView(info)

        codeInput = EditText(this).apply {
            hint = "6 haneli eşleşme kodu"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        root.addView(codeInput)

        dateInput = EditText(this).apply {
            hint = "İlişki başlangıcı (YYYY-AA-GG)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setText(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
        }
        root.addView(dateInput)

        val createButton = Button(this).apply {
            text = "Yeni eşleşme oluştur"
            setOnClickListener { createPair() }
        }
        root.addView(createButton)

        val joinButton = Button(this).apply {
            text = "Partnerimin koduyla katıl"
            setOnClickListener { joinPair() }
        }
        root.addView(joinButton)

        setContentView(root)
    }

    private fun createPair() {
        val date = dateInput.text.toString().trim()

        if (!date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            info.text = "Tarihi YYYY-AA-GG şeklinde yaz."
            return
        }

        val code = (100000..999999).random().toString()
        info.text = "Eşleşme oluşturuluyor..."

        thread {
            try {
                val pairId = SupabaseStore.createPair(session, code, date)

                prefs.edit()
                    .putString("pair_id", pairId)
                    .putString("pair_code", code)
                    .apply()

                runOnUiThread {
                    info.text = "♥ Eşleşme oluşturuldu!\nKodun: $code\nPartnerine gönder."
                }
            } catch (e: Exception) {
                runOnUiThread {
                    info.text = "Eşleşme hatası:\n${e.message}"
                }
            }
        }
    }

    private fun joinPair() {
        val code = codeInput.text.toString().trim()

        if (!code.matches(Regex("\\d{6}"))) {
            info.text = "6 haneli kodu gir."
            return
        }

        info.text = "Eşleşiliyor..."

        thread {
            try {
                val pairId = SupabaseStore.joinPair(session, code)

                prefs.edit()
                    .putString("pair_id", pairId)
                    .apply()

                runOnUiThread {
                    info.text = "♥ Başarıyla eşleştiniz!"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    info.text = "Eşleşme hatası:\n${e.message}"
                }
            }
        }
    }
}
