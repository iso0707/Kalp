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

    private val dark = Color.rgb(12, 14, 25)
    private val pink = Color.rgb(255, 130, 170)

    private lateinit var root: LinearLayout
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var info: TextView

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
            showPairScreen(SupabaseStore.Session(uid, token))
        } else {
            showLoginScreen()
        }
    }

    private fun makeRoot() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
            setBackgroundColor(dark)
        }
        setContentView(root)
    }

    private fun addText(value: String, size: Float): TextView {
        val view = TextView(this).apply {
            text = value
            textSize = size
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(8, 12, 8, 12)
        }
        root.addView(view)
        return view
    }

    private fun makeInput(hintText: String, isPassword: Boolean = false): EditText {
        return EditText(this).apply {
            hint = hintText
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            if (isPassword) {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            } else {
                inputType = android.text.InputType.TYPE_CLASS_TEXT
            }
            root.addView(this)
        }
    }

    private fun makeButton(label: String, action: () -> Unit) {
        val button = Button(this).apply {
            text = label
            setOnClickListener { action() }
        }
        root.addView(button)
    }

    private fun showLoginScreen() {
        makeRoot()

        val title = addText("♥ Kalp", 36f)
        title.setTextColor(pink)

        addText("Birbirinize olan mesafeniz artık önemli değil.", 14f)

        username = makeInput("Kullanıcı adı")
        password = makeInput("Şifre", true)

        info = addText("", 14f)

        makeButton("Giriş Yap") {
            authenticate(false)
        }

        makeButton("Hesap Oluştur") {
            authenticate(true)
        }
    }

    private fun authenticate(register: Boolean) {
        val user = username.text.toString().trim()
        val pass = password.text.toString()

        if (!user.matches(Regex("[a-zA-Z0-9_]{3,20}"))) {
            info.text = "Kullanıcı adı 3-20 karakter olmalı. Harf, rakam ve _ kullan."
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
                    showPairScreen(session)
                }

            } catch (e: Exception) {
                runOnUiThread {
                    info.text = e.message ?: "Bir hata oluştu."
                }
            }
        }
    }

    private fun showPairScreen(session: SupabaseStore.Session) {
        makeRoot()

        val title = addText("♥ Kalp", 34f)
        title.setTextColor(pink)

        addText("Mesafeler uzak olsa da kalpler yakın.", 14f)

        info = addText("Henüz eşleşmediniz.", 16f)

        val codeInput = makeInput("6 haneli eşleşme kodu")
        val dateInput = makeInput("İlişki başlangıcı (YYYY-AA-GG)")

        dateInput.setText(
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        )

        makeButton("Yeni eşleşme oluştur") {
            val date = dateInput.text.toString().trim()

            if (!date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                info.text = "Tarihi YYYY-AA-GG şeklinde yaz."
                return@makeButton
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
                        info.text = "♥ Kodun: $code\nPartnerine gönder."
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        info.text = "Hata: ${e.message}"
                    }
                }
            }
        }

        makeButton("Partnerimin koduyla katıl") {
            val code = codeInput.text.toString().trim()

            if (!code.matches(Regex("\\d{6}"))) {
                info.text = "6 haneli kodu gir."
                return@makeButton
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

        makeButton("Çıkış Yap") {
            prefs.edit().clear().apply()
            showLoginScreen()
        }
    }
}
