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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uid = prefs.getString("uid", null)
        val token = prefs.getString("token", null)

        if (uid != null && token != null) {
            session = SupabaseStore.Session(uid, token)
            buildUi()
        } else {
            info = TextView(this)
            info.text = "Kalp hazırlanıyor..."
            info.setTextColor(Color.WHITE)
            setContentView(info)
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
                        info.text = "Bağlantı hatası: ${e.message}"
                    }
                }
            }
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
            setBackgroundColor(Color.rgb(12, 14, 25))
        }

        fun text(value: String, size: Float): TextView {
            return TextView(this).apply {
                text = value
                textSize = size
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
        }

        val title = text("♥  Kalp", 32f)
        root.addView(title)

        root.addView(text("Mesafeler uzak olsa da kalpler yakın.", 14f))

        info = text("Henüz eşleşmediniz.", 16f)
        root.addView(info)

        codeInput = EditText(this).apply {
            hint = "Eşleşme kodu"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        root.addView(codeInput)

        dateInput = EditText(this).apply {
            hint = "İlişki başlangıcı (YYYY-MM-DD)"
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
        val code = (100000..999999).random().toString()

        if (!date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            info.text = "Tarihi YYYY-AA-GG şeklinde yaz."
            return
        }

        info.text = "Eşleşme oluşturuluyor..."

        thread {
            try {
                val pairId = SupabaseStore.createPair(session, code, date)

                prefs.edit()
                    .putString("pair_id", pairId)
                    .putString("pair_code", code)
                    .apply()

                runOnUiThread {
                    info.text = "Kodun: $code\nPartnerine gönder."
                }
            } catch (e: Exception) {
                runOnUiThread {
                    info.text = "Hata: ${e.message}"
                }
            }
        }
    }

    private fun joinPair() {
        val code = codeInput.text.toString().trim()

        if (code.length != 6) {
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
                    info.text = "Hata: ${e.message}"
                }
            }
        }
    }
}
