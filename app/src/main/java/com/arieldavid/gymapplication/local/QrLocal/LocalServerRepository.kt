package com.arieldavid.gymapplication.local.QrLocal

import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object LocalServerRepository {

    private val client = OkHttpClient()

    suspend fun enviarQR(
        idCliente: String
    ): QrResponse? {

        return try {

            val json = """
            {
                "idCliente":"$idCliente"
            }
            """

            val body = json.toRequestBody(
                "application/json".toMediaType()
            )

            val request = Request.Builder()
                .url("http://10.213.7.114:8080/api/qr")
                .post(body)
                .build()

            val response = client.newCall(request).execute()

            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null)
                return null

            Json.decodeFromString<QrResponse>(responseBody)

        } catch (e: Exception) {

            Log.e("LOCAL", "ERROR", e)
            null
        }
    }
}