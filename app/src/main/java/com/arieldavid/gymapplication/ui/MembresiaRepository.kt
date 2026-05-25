package com.arieldavid.gymapplication.ui

import com.arieldavid.gymapplication.SupabaseConfig
import okhttp3.OkHttpClient
import okhttp3.Request

object MembresiaRepository {

    private val client = OkHttpClient()

    suspend fun cargarMembresias(): String? {

        val request = Request.Builder()
            .url("${SupabaseConfig.URL}/rest/v1/Membresia?select=*")
            .addHeader("apikey", SupabaseConfig.API_KEY)
            .build()

        val response = client.newCall(request).execute()

        return response.body?.string()
    }
}