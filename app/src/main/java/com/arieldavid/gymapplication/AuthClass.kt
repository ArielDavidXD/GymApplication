package com.arieldavid.gymapplication

import android.content.Context
import android.util.Log
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.arieldavid.gymapplication.LoginResponse
import com.arieldavid.gymapplication.ui.profile.Cliente
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object AuthClass {

    private val client = OkHttpClient()

    suspend fun signUp(context: Context, email: String, password: String): Boolean {
        val json = """{"email":"$email","password":"$password"}"""
        val body = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${SupabaseConfig.URL}/auth/v1/signup")
            .addHeader("apikey", SupabaseConfig.API_KEY)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody: String? = response.body?.string()
        if (responseBody == null || responseBody.length == 0) {
            return false
        }

        // Parsear y guardar token si lo devuelve
        val loginResponse = Json.decodeFromString<LoginResponse>(responseBody)
        val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        prefs.edit().putString("access_token", loginResponse.access_token).apply()

        return true
    }

    suspend fun login(
        context: Context,
        email: String,
        password: String
    ): Boolean {

        return withContext(Dispatchers.IO) {

            val json = """{"email":"$email","password":"$password"}"""

            val body = json.toRequestBody(
                "application/json".toMediaType()
            )

            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/auth/v1/token?grant_type=password")
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            try {

                val response = client.newCall(request).execute()

                val responseBody = response.body?.string()

                if (responseBody.isNullOrEmpty())
                    return@withContext false

                if (responseBody.contains("error"))
                    return@withContext false

                val jsonParser = Json {
                    ignoreUnknownKeys = true
                }

                val loginResponse =
                    jsonParser.decodeFromString<LoginResponse>(
                        responseBody
                    )

                val token = loginResponse.access_token

                val userId =
                    loginResponse.user?.id ?: ""

                //////////////////////////////////////////////////////
                // BUSCAR PERFIL
                //////////////////////////////////////////////////////

                val perfilRequest = Request.Builder()
                    .url(
                        "${SupabaseConfig.URL}/rest/v1/Clientes?user_id=eq.$userId&select=*,Payment(*)"
                    )
                    .addHeader("apikey", SupabaseConfig.API_KEY)
                    .addHeader(
                        "Authorization",
                        "Bearer $token"
                    )
                    .addHeader(
                        "Content-Type",
                        "application/json"
                    )
                    .build()

                val perfilResponse =
                    client.newCall(perfilRequest).execute()

                val perfilBody =
                    perfilResponse.body?.string()

                Log.d(
                    "LOGIN_DEBUG",
                    "PERFIL: $perfilBody"
                )

                var nombre = "Usuario"

                if (!perfilBody.isNullOrEmpty()) {

                    val clientes =
                        jsonParser.decodeFromString<List<Cliente>>(
                            perfilBody
                        )

                    nombre =
                        clientes.firstOrNull()?.nombre
                            ?: "Usuario"
                }

                //////////////////////////////////////////////////////
                // GUARDAR SESSION
                //////////////////////////////////////////////////////

                val prefs = context.getSharedPreferences(
                    "auth",
                    Context.MODE_PRIVATE
                )

                prefs.edit()
                    .putString(
                        "access_token",
                        token
                    )
                    .putString(
                        "user_id",
                        userId
                    )
                    .putString(
                        "nombre",
                        nombre
                    )
                    .apply()

                true

            } catch (e: Exception) {

                e.printStackTrace()

                false
            }
        }
    }
    suspend fun cambiarPassword(context: Context, nuevaPassword: String): Boolean {
        return withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
            val token = prefs.getString("access_token", null) ?: return@withContext false

            val json = """{"password":"$nuevaPassword"}"""
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/auth/v1/user")
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .put(body)
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d("AuthClass", "Respuesta cambio password: $responseBody")

                response.isSuccessful
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }



}
