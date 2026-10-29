package com.kalp.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object SupabaseStore {

    const val BASE_URL = "https://dtdecwxgflsywnvylcwe.supabase.co"

    // Kendi Supabase Publishable Key anahtarını buraya yaz.
    const val PUBLISHABLE_KEY = "sb_publishable_fnDics7iGtWDaO_oC1JXbA_T045VCXp"

    data class Session(val uid: String, val token: String)

    private fun request(
        path: String,
        method: String,
        token: String? = null,
        body: Any? = null,
        prefer: String? = null
    ): String {
        val conn = URL(BASE_URL + path).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        conn.setRequestProperty("apikey", PUBLISHABLE_KEY)
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")

        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer $token")
        }

        if (prefer != null) {
            conn.setRequestProperty("Prefer", prefer)
        }

        if (body != null) {
            conn.doOutput = true
            conn.outputStream.use {
                it.write(body.toString().toByteArray(Charsets.UTF_8))
            }
        }

        val result = if (conn.responseCode in 200..299) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()
            throw Exception("Supabase ${conn.responseCode}: $error")
        }

        conn.disconnect()
        return result
    }

    fun anonymousSignIn(): Session {
        val json = JSONObject(
            request("/auth/v1/signup", "POST", body = JSONObject())
        )

        return Session(
            json.getJSONObject("user").getString("id"),
            json.getString("access_token")
        )
    }

    fun createPair(session: Session, code: String, date: String): String {
        return request(
            "/rest/v1/rpc/create_pair",
            "POST",
            session.token,
            JSONObject()
                .put("p_code", code)
                .put("p_start_date", date)
        ).trim().trim('"')
    }

    fun joinPair(session: Session, code: String): String {
        return request(
            "/rest/v1/rpc/join_pair",
            "POST",
            session.token,
            JSONObject().put("p_code", code)
        ).trim().trim('"')
    }

    fun getLocations(session: Session, pairId: String): JSONArray {
        return JSONArray(
            request(
                "/rest/v1/locations?pair_id=eq.$pairId&select=user_id,latitude,longitude,updated_at",
                "GET",
                session.token
            )
        )
    }
}
