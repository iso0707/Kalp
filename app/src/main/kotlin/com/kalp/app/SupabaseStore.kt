package com.kalp.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object SupabaseStore {

    const val BASE_URL = "https://dtdecwxgflsywnvylcwe.supabase.co"
    const val PUBLISHABLE_KEY = "MEVCUT_PUBLISHABLE_KEYINI_BURAYA_YAZ"

    data class Session(val uid: String, val token: String)

    private fun request(
        path: String,
        method: String,
        token: String? = null,
        body: JSONObject? = null
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
            val code = conn.responseCode
            conn.disconnect()
            throw Exception("Supabase $code: $error")
        }

        conn.disconnect()
        return result
    }

    private fun accountEmail(username: String) =
        username.trim().lowercase() + "@kalp.app"

    private fun sessionFrom(response: String): Session {
        val json = JSONObject(response)
        return Session(
            json.getJSONObject("user").getString("id"),
            json.getString("access_token")
        )
    }

    fun register(username: String, password: String): Session {
        return sessionFrom(
            request(
                "/auth/v1/signup",
                "POST",
                body = JSONObject()
                    .put("email", accountEmail(username))
                    .put("password", password)
            )
        )
    }

    fun login(username: String, password: String): Session {
        return sessionFrom(
            request(
                "/auth/v1/token?grant_type=password",
                "POST",
                body = JSONObject()
                    .put("email", accountEmail(username))
                    .put("password", password)
            )
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

    fun getPairInfo(session: Session, pairId: String): JSONObject {
        val result = JSONArray(
            request(
                "/rest/v1/pairs?id=eq.$pairId&select=relationship_start,partner_id",
                "GET",
                session.token
            )
        )

        if (result.length() == 0) {
            throw Exception("Eşleşme bilgisi bulunamadı.")
        }

        return result.getJSONObject(0)
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
