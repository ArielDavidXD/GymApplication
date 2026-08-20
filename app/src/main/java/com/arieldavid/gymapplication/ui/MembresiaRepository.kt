package com.arieldavid.gymapplication.ui

import android.util.Log
import com.arieldavid.gymapplication.SupabaseConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object MembresiaRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .writeTimeout(2, TimeUnit.SECONDS)
        .build()

    suspend fun cargarMembresias(
        token: String?
    ): String? {

        val request = Request.Builder()
            .url("${SupabaseConfig.URL}/rest/v1/Membresia?select=*")
            .addHeader("apikey", SupabaseConfig.API_KEY)
            .addHeader(
                "Authorization",
                "Bearer $token"
            )
            .build()

        val response = client.newCall(request).execute()

        Log.d("PLANES", "HTTP = ${response.code}")

        val body = response.body?.string()

        Log.d("PLANES", "BODY = $body")

        return body
    }
}