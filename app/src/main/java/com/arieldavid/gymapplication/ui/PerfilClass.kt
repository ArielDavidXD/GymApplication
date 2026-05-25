package com.arieldavid.gymapplication.ui

import android.util.Log
import com.arieldavid.gymapplication.SupabaseConfig
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object PerfilRepository {

    private val client = OkHttpClient()
    suspend fun cargarPerfil(token: String, userId: String): String? {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {

            val url = "${SupabaseConfig.URL}/rest/v1/Clientes?user_id=eq.$userId&select=*,Payment(*)"

            Log.d("PERFIL_DEBUG", "USER ID: $userId")
            Log.d("PERFIL_DEBUG", "URL: $url")

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            try {
                val response = client.newCall(request).execute()

                Log.d("PERFIL_DEBUG", "HTTP CODE: ${response.code}")

                val body = response.body?.string()

                Log.d("PERFIL_DEBUG", "RESPONSE BODY: $body")

                body

            } catch (e: Exception) {
                Log.e("PERFIL_DEBUG", "ERROR: ${e.message}")
                null
            }
        }
    }
}